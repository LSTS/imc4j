package pt.lsts.autonomy.soi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Properties;

import pt.lsts.backseat.TimedFSM;
import pt.lsts.endurance.Plan;
import pt.lsts.endurance.Waypoint;
import pt.lsts.imc4j.annotations.Consume;
import pt.lsts.imc4j.annotations.Parameter;
import pt.lsts.imc4j.def.SpeedUnits;
import pt.lsts.imc4j.msg.EntityParameter;
import pt.lsts.imc4j.msg.EntityParameters;
import pt.lsts.imc4j.msg.EstimatedState;
import pt.lsts.imc4j.msg.FollowRefState;
import pt.lsts.imc4j.msg.FuelLevel;
import pt.lsts.imc4j.msg.IridiumTxStatus;
import pt.lsts.imc4j.msg.PlanControl;
import pt.lsts.imc4j.msg.PlanControl.OP;
import pt.lsts.imc4j.msg.PlanControlState;
import pt.lsts.imc4j.msg.ReportControl;
import pt.lsts.imc4j.msg.SoiCommand;
import pt.lsts.imc4j.msg.SoiCommand.COMMAND;
import pt.lsts.imc4j.msg.StateReport;
import pt.lsts.imc4j.msg.Temperature;
import pt.lsts.imc4j.msg.VehicleMedium;
import pt.lsts.imc4j.msg.IridiumTxStatus.STATUS;
import pt.lsts.imc4j.msg.VehicleMedium.MEDIUM;
import pt.lsts.imc4j.msg.VerticalProfile;
import pt.lsts.imc4j.msg.VerticalProfile.PARAMETER;
import pt.lsts.imc4j.util.PojoConfig;
import pt.lsts.imc4j.util.WGS84Utilities;

public class SoiExecutive extends TimedFSM {

	@Parameter(description = "Nominal Speed")
	double speed = 1;

	@Parameter(description = "Maximum Depth")
	double maxDepth = 10;

	@Parameter(description = "Minimum Depth")
	double minDepth = 0.0;

	@Parameter(description = "Maximum Speed")
	double maxSpeed = 1.5;

	@Parameter(description = "Minimum Speed")
	double minSpeed = 0.7;

	@Parameter(description = "DUNE Host Address")
	String hAddr = "127.0.0.1";

	@Parameter(description = "DUNE Host Port (TCP)")
	int hPort = 6006;

	@Parameter(description = "Minutes before termination")
	int timeout = 600;

	@Parameter(description = "Maximum time without reporting position")
	int minsOff = 15;
	
	@Parameter(description = "Maximum time without GPS")
	int minsUnder = 3;	

	@Parameter(description = "Seconds to idle at each vertex")
	int wptSecs = 60;

	@Parameter(description = "Cyclic execution")
	boolean cycle = false;
	
	@Parameter(description = "Speed up before descending")
	int descRpm = 1300;
	
	@Parameter(description = "Upload temperature profiles when idle")
	boolean upTemp = false;
	
	@Parameter(description = "Align with destination waypoint before going underwater")
	boolean align = true;
	
	private Plan plan = new Plan("idle");
	private int secs_no_comms = 0, count_secs = 0, secs_underwater = 0;
	private int wpt_index = 0;
	private ArrayList<String> txtMessages = new ArrayList<>();
	private ArrayList<SoiCommand> replies = new ArrayList<>();
	private ArrayList<VerticalProfile> profiles = new ArrayList<>();
	private VerticalProfiler<Temperature> tempProfiler = new VerticalProfiler<>();	
	
	private final String SOI_PLAN_ID = "soi_plan";
	private final int ANGLE_DIFF_DEGS = 5;
	
	/**
	 * Class constructor
	 */
	public SoiExecutive() {
		setPlanName(SOI_PLAN_ID);
		setDeadline(new Date(System.currentTimeMillis() + timeout * 60 * 1000));
		state = this::idleAtSurface;
	}

	/**
	 * In case the last plan failed, report the resulting error
	 * @param pControl A {@link PlanControl} message
 	 */
	@Consume
	public void on(PlanControl pControl) {		
		if (pControl.op == OP.PC_START && pControl.type == PlanControl.TYPE.PC_FAILURE) {
			if (pControl.plan_id.equals(SOI_PLAN_ID)) {
				// Error during execution!
				String err = "Detected error during execution: "+pControl.info;
				printError(err);
				txtMessages.add("ERROR: "+pControl.info);
				print("Ascending for report");
				state = this::surface_to_report_error;
			}
		}
		
		if (pControl.op == OP.PC_STOP && pControl.type == PlanControl.TYPE.PC_SUCCESS) {
			state = this::start_waiting;
		}
	}

	/**
	 * React to incoming commands
	 * @param cmd The received command
	 */
	@Consume
	public void on(SoiCommand cmd) {
		if (cmd.type != SoiCommand.TYPE.SOITYPE_REQUEST)
			return;
		SoiCommand reply = new SoiCommand();
		reply.command = cmd.command;
		reply.type = SoiCommand.TYPE.SOITYPE_ERROR;
		reply.src = remoteSrc;
		reply.dst = cmd.src;
		reply.dst_ent = cmd.src_ent;

		switch (cmd.command) {

		case SOICMD_EXEC:
			print("CMD: Exec plan!");
			if (cmd.plan == null || cmd.plan.waypoints.isEmpty()) {
				plan = null;				
			}
			else {
				plan = Plan.parse(cmd.plan);
				
				if (!plan.scheduledInTheFuture()) {
					EstimatedState s = get(EstimatedState.class);
					if (s != null) {
						double[] pos = WGS84Utilities.toLatLonDepth(s);
						plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, pos[0], pos[1], speed);
					} else
						plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, speed);
				}
				
				if (plan.getETA().after(deadline)) {
					int timeDiff = (int) ((plan.getETA().getTime() - deadline.getTime()) / 1000.0);
					String err = "Deadline would be reached " + timeDiff + " seconds before the end of the plan";
					printError(err);
					plan = null;
					txtMessages.add(err);
					reply.type = SoiCommand.TYPE.SOITYPE_ERROR;
					reply.plan = null;
					break;				
				}
				wpt_index = 0;
				reply.plan = plan.asImc();
				
				print("Start executing this plan:");
				print("" + plan);					
				
			}
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;			
			break;

		case SOICMD_GET_PARAMS:
			print("CMD: Get Params!");
			for (Field f : getClass().getDeclaredFields()) {
				f.setAccessible(true);
				Parameter p = f.getAnnotation(Parameter.class);
				if (p == null)
					continue;
				String name = f.getName();
				try {
					reply.settings.set(name, f.get(this));
				} catch (Exception e) {

				}
			}
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			break;

		case SOICMD_SET_PARAMS:
			print("CMD: Set Params!");
			try {
				for (String key : cmd.settings.keys())
					PojoConfig.setProperty(this, key, cmd.settings.get(key));
				reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;

		case SOICMD_STOP:
			print("CMD: Stop execution!");
			setPaused(true);
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			break;

		case SOICMD_GET_PLAN:
			print("CMD: Get plan!");
			reply.plan = plan.asImc();
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			break;

		case SOICMD_RESUME:
			print("CMD: Resume execution!");
			resetDeadline();
			if (paused) {
				setPaused(false);
				return;
			}
			break;
		default:
			break;
		}

		print("Replying with "+reply);
		
		try {
			send(reply);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		replies.add(reply);
		state = this::start_waiting;
	}

	@Consume
	public void on(EntityParameters params) {
		if (params.name.equals(getClass().getSimpleName())) {
			for (EntityParameter param : params.params) {
				try {
					PojoConfig.setValue(this, param.name, param.value);
					print("Set " + param.name + " := " + param.value);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Consume
	public void on(Temperature temp) {
		tempProfiler.setSample(get(EstimatedState.class), temp);
	}
	
	@Consume
	public void on(VehicleMedium medium) {
		if (medium.medium == MEDIUM.VM_UNDERWATER)
			secs_underwater++;
		else
			secs_underwater = 0;				
	}
	
	/**
	 * Actively go to the surface to wait for a plan
	 */
	public FSMState idleAtSurface(FollowRefState state) {
		printFSMState();
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		setLocation(pos[0], pos[1]);
		setDepth(0);
		return this::idle;
	}

	/**
	 * Wait for a plan to arrive
	 */
	public FSMState idle(FollowRefState state) {
		printFSMState();
		
		if (!profiles.isEmpty() && upTemp) {
			print("Sending vertical profile ("+(profiles.size()-1)+" left)...");
			sendViaIridium(profiles.get(profiles.size()-1), wptSecs * 3);
			profiles.remove(profiles.size()-1);
			return this::wait;					
		}
		else {
			profiles.clear();
			print("Waiting for plan...");
		}
		return this::idle;
	}

	/**
	 * Execute the next waypoint
	 */
	public FSMState exec(FollowRefState state) {
		printFSMState();
		if (plan == null)
			return this::idleAtSurface;

		Waypoint wpt = plan.waypoint(wpt_index);

		if (wpt == null) {
			print("Finished executing plan.");
			if (cycle) {
				print("Starting over (cyclic)...");
				wpt_index = 0;
				EstimatedState s = get(EstimatedState.class);
				if (s != null) {
					double[] pos = WGS84Utilities.toLatLonDepth(s);
					plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, pos[0], pos[1], speed);
				} 
				else
					plan.scheduleWaypoints(System.currentTimeMillis(), wptSecs, speed);
				
				if (plan.getETA().after(deadline)) {
					int timeDiff = (int) ((plan.getETA().getTime() - deadline.getTime()) / 1000.0);
					String err = "Deadline would be reached " + timeDiff + " seconds before the end of the plan";
					printError(err);
					plan = null;
					txtMessages.add(err);					
					return this::idleAtSurface;
				}
				
				SoiCommand reply = new SoiCommand();
				reply.command = COMMAND.SOICMD_GET_PLAN;
				reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
				reply.src = remoteSrc;
				reply.dst = 0xFFFF;
				reply.plan = plan.asImc();
				replies.add(reply);
				
				return this::start_waiting;
			} else
				return this::idleAtSurface;
		}

		print("Executing wpt " + wpt_index);
		setLocation(wpt.getLatitude(), wpt.getLongitude());
		setSpeed();

		return this::align;
	}

	/**
	 * Go to maximum depth
	 */
	public FSMState descend(FollowRefState ref) {
		printFSMState();
		setDepth(maxDepth);
		
		secs_no_comms++;
		if (offlineForTooLong()) {
			String err = "Offline for too long ("+secs_no_comms+")";
			printError(err);
			txtMessages.add("ERROR: "+err);
			return this::surface_to_report_error;
		}
		
		if (arrivedXY()) {
			print("Arrived at waypoint " + wpt_index);
			wpt_index++;
			return this::start_waiting;
		}
		try {
			double[] cur_pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
			double[] target_pos = new double[] { plan.waypoint(wpt_index).getLatitude(),
					plan.waypoint(wpt_index).getLongitude() };
			
			double dist = WGS84Utilities.distance(cur_pos[0], cur_pos[1], target_pos[0], target_pos[1]);
			double min_dist = 4 * cur_pos[2];
			
			if (dist < min_dist) {
				print("Starting to ascend.");
				return this::ascend;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (arrivedZ()) {
			setSpeed();
			if (minDepth < maxDepth)
				print("Now ascending.");
			return this::ascend;
		}
		else
			return this::descend;
	}

	/**
	 * Go to minimum depth
	 */
	public FSMState ascend(FollowRefState ref) {
		printFSMState();
		
		double target_depth = minDepth;
		if (minsUnder > 0 && (secs_underwater / 60) >= minsUnder)
			target_depth = 0;						
		
		setDepth(target_depth);

		secs_no_comms++;
		if (offlineForTooLong()) {
			String err = "Offline for too long ("+secs_no_comms+")";
			printError(err);
			txtMessages.add("ERROR: "+err);
			return this::surface_to_report_error;
		}
		
		if (secs_no_comms / 60 >= minsOff) {
			print("Periodic surface");
			return this::start_waiting;
		}			

		if (arrivedXY()) {
            print("Arrived at waypoint " + wpt_index);
			wpt_index++;
			return this::start_waiting;
		}

		if (target_depth > 0 && arrivedZ() || !isUnderwater()) {
			if (secs_no_comms / 60 >= minsOff) {
				print("Periodic surface");
				return this::start_waiting;
			} else {
				
				if (maxDepth != target_depth) {
					print("Now descending (disconnected for " + secs_no_comms + " seconds).");
					if (upTemp)
						profiles.add(tempProfiler.getProfile(PARAMETER.PROF_TEMPERATURE, Math.min((int)maxDepth, 20)));				
				}
				
				if (isUnderwater())
					return this::descend;
				else 
					if (align)
						return this::align;
				else
					return this::dive;
			}

		} else
			return this::ascend;
	}
	
	/**
	 * Right before diving, align yaw with target waypoint
	 */
	public FSMState align(FollowRefState ref) {
		printFSMState();
		EstimatedState state = get(EstimatedState.class);
		double[] pos = WGS84Utilities.toLatLonDepth(state);
		
		secs_no_comms++;
		if (offlineForTooLong()) {
			String err = "Offline for too long ("+secs_no_comms+")";
			printError(err);
			txtMessages.add("ERROR: "+err);
			return this::surface_to_report_error;
		}
		
		if (arrivedXY()) {
			print("Arrived at waypoint " + wpt_index);
			wpt_index++;
			return this::start_waiting;
		}
		double[] dest = getDestinationDegs();
		// Difference between current and destination location
		double[] diff = WGS84Utilities.WGS84displacement(pos[0], pos[1], 0, dest[0], dest[1], 0);
		double des_ang = Math.toDegrees(Math.atan2(diff[1], diff[0]));
		double cur_ang = Math.toDegrees(state.psi);
		// Angle difference to destination (degrees)
		double ang_diff = Math.abs(des_ang - cur_ang);
		
		setSpeed(descRpm, SpeedUnits.RPM);
		
		// go underwater only if aligned with destination
		if (ang_diff < ANGLE_DIFF_DEGS) {
			setDepth(maxDepth);
			return this::dive;
		}
		else {
			setDepth(0);
			return this::align;
		}
	}
	
	/**
	 * Go underwater at fixed RPM speed
	 */
	public FSMState dive(FollowRefState ref) {
		printFSMState();
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		
		secs_no_comms++;
		if (offlineForTooLong()) {
			String err = "Offline for too long ("+secs_no_comms+")";
			printError(err);
			txtMessages.add("ERROR: "+err);
			return this::surface_to_report_error;
		}
		
		if (arrivedXY()) {
			print("Arrived at waypoint " + wpt_index);
			wpt_index++;
			return this::start_waiting;
		}
		
		setDepth(maxDepth);
		setSpeed(descRpm, SpeedUnits.RPM);
		
		if (pos[2] < 2 && pos[2] < maxDepth) {
			return this::dive;
		}
		else {
			setSpeed();
			return this::descend;
		}
	}
	
	/**
	 * Send a position report and any pending replies / errors
	 */
	public FSMState communicate(FollowRefState ref) {
		printFSMState();
		int max_wait = wptSecs * 3;
		int min_wait = wptSecs;
		if (plan != null && plan.waypoint(wpt_index) != null)
			min_wait = Math.max(min_wait, plan.waypoint(wpt_index).getDuration());
		
		// Send "DUNE" report
		if (count_secs == 0) {
			EnumSet<ReportControl.COMM_INTERFACE> itfs = EnumSet.of(ReportControl.COMM_INTERFACE.CI_GSM);
			itfs.add(ReportControl.COMM_INTERFACE.CI_SATELLITE);
			sendReport(itfs);
			sendViaIridium(createStateReport(), max_wait - count_secs - 1);			
			print("Will wait from " + min_wait + " to " + max_wait + " seconds to send " + txtMessages.size()
					+ " texts and " + replies.size() + " command replies.");			
		}
		else {
			while (!replies.isEmpty()) {
				SoiCommand cmd = replies.get(0);
				sendViaIridium(cmd, max_wait - count_secs -1);
				replies.remove(0);
			}
			
			while (!txtMessages.isEmpty()) {
				String txt = txtMessages.get(0);
				if (txt.length() > 132)
					txt = txt.substring(0, 132);
				sendViaSms(txt, max_wait - count_secs - 1);
				sendViaIridium(txt, max_wait - count_secs - 1);
				txtMessages.remove(0);
			}
		}
		
		if (count_secs >= max_wait) {
			print("Advancing to next waypoint as maximum time was reached.");			
			return this::exec;
		}
		else if (count_secs > min_wait) {
			IridiumTxStatus iridiumStatus = get(IridiumTxStatus.class);
			
			if (iridiumStatus != null && iridiumStatus.timestamp > (System.currentTimeMillis() / 1000.0) - 3
					&& iridiumStatus.status == STATUS.TXSTATUS_EMPTY) {
				
				print("Synchronized with server in "+count_secs+" seconds. Advancing to next waypoint.");
				 return this::exec;
			 }
		}				
		count_secs++;			
		return this::communicate;
		
	}

	/**
	 * Request the vehicle to (actively) go at the surface
	 */
	public FSMState start_waiting(FollowRefState ref) {
		printFSMState();
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		setLocation(pos[0], pos[1]);
		setDepth(0);
		setSpeed(speed, SpeedUnits.METERS_PS);

		print("Surfacing...");
		return this::wait;
	}
	
	/**
	 * Stop the motor and start waiting to float to the surface
	 */
	public FSMState surface_to_report_error(FollowRefState ref) {
		printFSMState();
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		setLocation(pos[0], pos[1]);
		setDepth(0);
		setSpeed(0, SpeedUnits.METERS_PS);

		print("Surfacing to report error...");

		return this::report_error;
	}
	
	/**
	 * Wait to arrive at the surface before communications
	 */
	public FSMState report_error(FollowRefState ref) {
		printFSMState();
		VehicleMedium medium = get(VehicleMedium.class); 
		
		// arrived at surface
		if (medium != null && medium.medium == MEDIUM.VM_WATER) {
			print("Now at surface, starting communications.");
			secs_no_comms = 0;
			count_secs = 0;
			return this::communicate;
		}
		else
			return this::report_error;
	}

	/**
	 * Actively go at the surface before communications
	 */
	public FSMState wait(FollowRefState ref) {
		printFSMState();
		secs_no_comms++;
		if (offlineForTooLong()) {
			String err = "Offline for too long ("+secs_no_comms+")";
			printError(err);
			txtMessages.add("ERROR: "+err);
			return this::surface_to_report_error;
		}
		
		VehicleMedium medium = get(VehicleMedium.class); 
		// arrived at surface
		if (medium != null && medium.medium == MEDIUM.VM_WATER) {
			print("Now at surface, starting communications.");
			double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
			setLocation(pos[0], pos[1]);
			secs_no_comms = 0;
			count_secs = 0;
			return this::communicate;
		}
		else {			
			return this::wait;
		}
	}
	
	/**
	 * Set the desired speed based on current ETA and distance
	 */
	public void setSpeed() {
		Waypoint wpt = plan.waypoint(wpt_index);
		double speed = this.speed;

		if (wpt == null)
			speed = 0;

		if (wpt.getArrivalTime() != null) {
			double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
			double dist = WGS84Utilities.distance(wpt.getLatitude(), wpt.getLongitude(), pos[0], pos[1]);
			double secs = (wpt.getArrivalTime().getTime() - System.currentTimeMillis()) / 1000.0;

			if (secs < 0) {
				speed = maxSpeed;
			} else {
				speed = Math.min(maxSpeed, dist / secs);
				speed = Math.max(minSpeed, speed);
			}
		}

		print("Setting speed according to ETA: "+speed+" m/s.");
		setSpeed(speed, SpeedUnits.METERS_PS);
	}
	
	/**
	 * Check if the vehicle has been disconnected for too long
	 * @return <code>true</code> if the vehicle has been more than <code>mins_offline</code> minutes disconnected. 
	 */
	public boolean offlineForTooLong() {
		 // Check if it has taken too long to go at the surface...
		int max_time = minsOff * 60 * 2;
		return secs_no_comms > max_time;
	}
	
	/**
	 * Reset watchdog based on timeout parameter
	 */
	private void resetDeadline() {
		deadline = new Date(System.currentTimeMillis() + timeout * 60 * 1000);
		String txtDeadline = "INFO: Execution will end by "+deadline; 
		txtMessages.add(txtDeadline);
		setDeadline(deadline);
		print(txtDeadline);		
	}
	

	/**
	 * Generate state report to be sent over Iridium
	 * @return A {@link StateReport} to be sent over Iridium
	 */
	private StateReport createStateReport() {
		EstimatedState estate = get(EstimatedState.class);
		FuelLevel flevel = get(FuelLevel.class);
		PlanControlState pcs = get(PlanControlState.class);
		
		StateReport report = new StateReport();
		report.depth = estate == null || estate.depth == -1 ? 0xFFFF : (int) (estate.depth * 10);
		report.altitude = estate == null || estate.alt == -1 ? 0xFFFF : (int) (estate.alt * 10);
		report.speed = estate == null ? 0xFFFF : (int) (estate.u * 100);
		report.fuel = flevel == null ? 255 : (int)flevel.value;
		
		if (estate != null) {
			double[] loc = WGS84Utilities.toLatLonDepth(estate);
			report.latitude = (float) loc[0];
			report.longitude = (float) loc[1];
			double rads = estate.psi;
			while (rads < 0)
				rads += (Math.PI * 2);
			report.heading = (int) ((rads / (Math.PI * 2)) * 65535);
		}				
		
		report.exec_state = -2;
		if (pcs != null) {
			switch (pcs.state) {
			case PCS_EXECUTING:
				report.exec_state = (int) pcs.plan_progress;
				break;
			case PCS_READY:
				report.exec_state = -1;
				break;
			case PCS_INITIALIZING:
				report.exec_state = -3;
				break;				
			case PCS_BLOCKED:
				report.exec_state = -4;
				break;
			default:
				break;
			}
		}
		
		if (plan != null)
			report.plan_checksum = plan.checksum();
		
		report.stime = (int) (System.currentTimeMillis() / 1000);
		return report;
		
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java -jar SoiExec.jar <FILE>");
			System.exit(1);
		}

		File file = new File(args[0]);
		if (!file.exists()) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			SoiExecutive tmp = new SoiExecutive();
			writer.write("#SOI Executive settings\n\n");
			for (Field f : tmp.getClass().getDeclaredFields()) {
				Parameter p = f.getAnnotation(Parameter.class);
				if (p != null) {
					writer.write("#" + p.description() + "\n");
					writer.write(f.getName() + "=" + f.get(tmp) + "\n\n");
				}
			}
			System.out.println("Wrote default properties to " + file.getAbsolutePath());
			writer.close();
			System.exit(0);
		}

		Properties props = new Properties();
		props.load(new FileInputStream(file));

		SoiExecutive tracker = PojoConfig.create(SoiExecutive.class, props);

		System.out.println("Executive started with settings:");
		for (Field f : tracker.getClass().getDeclaredFields()) {
			Parameter p = f.getAnnotation(Parameter.class);
			if (p != null) {
				System.out.println(f.getName() + "=" + f.get(tracker));
			}
		}
		System.out.println();

		tracker.connect(tracker.hAddr, tracker.hPort);
		tracker.join();
	}
}
