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
import pt.lsts.imc4j.msg.PlanControl;
import pt.lsts.imc4j.msg.PlanControl.OP;
import pt.lsts.imc4j.msg.PlanControlState;
import pt.lsts.imc4j.msg.PlanDB;
import pt.lsts.imc4j.msg.PlanDB.TYPE;
import pt.lsts.imc4j.msg.PlanSpecification;
import pt.lsts.imc4j.msg.ReportControl;
import pt.lsts.imc4j.msg.SoiCommand;
import pt.lsts.imc4j.msg.SoiCommand.COMMAND;
import pt.lsts.imc4j.msg.StateReport;
import pt.lsts.imc4j.msg.VehicleMedium;
import pt.lsts.imc4j.msg.VehicleMedium.MEDIUM;
import pt.lsts.imc4j.util.PojoConfig;
import pt.lsts.imc4j.util.WGS84Utilities;

public class SoiExecutive extends TimedFSM {

	@Parameter(description = "Nominal Speed")
	double speed = 1;

	@Parameter(description = "Maximum Depth")
	double max_depth = 10;

	@Parameter(description = "Minimum Depth")
	double min_depth = 0.0;

	@Parameter(description = "Maximum Speed")
	double max_speed = 1.5;

	@Parameter(description = "Minimum Speed")
	double min_speed = 0.7;

	@Parameter(description = "DUNE Host Address")
	String host_addr = "127.0.0.1";

	@Parameter(description = "DUNE Host Port (TCP)")
	int host_port = 6006;

	@Parameter(description = "Minutes before termination")
	int mins_timeout = 600;

	@Parameter(description = "Maximum time underwater")
	int mins_under = 10;

	@Parameter(description = "Number where to send reports")
	String sms_number = "+351914785889";

	@Parameter(description = "Seconds to idle at each vertex")
	int wait_secs = 60;

	@Parameter(description = "SOI plan identifier")
	String soi_plan_id = "soi_plan";

	@Parameter(description = "Cyclic execution")
	boolean cycle = false;
	
	@Parameter(description = "Speed up before descending")
	int descendSpeedRpm = 1300;
	
	private Plan plan = new Plan("idle");
	private PlanSpecification spec = null;
	private int secs_underwater = 0, count_secs = 0;
	private int wpt_index = 0;
	private ArrayList<String> errors = new ArrayList<>();
	
	public SoiExecutive() {
		setPlanName(soi_plan_id);
		setDeadline(new Date(System.currentTimeMillis() + mins_timeout * 60 * 1000));
		state = this::init;
	}
	
	public boolean underwaterForTooLong() {
		 // Check if it has taken too long to go at the surface...
		int max_time = mins_under * 60 * 2;
		return secs_underwater > max_time;
	}

	@Consume
	public void on(PlanDB planDb) {
		if (planDb.op == PlanDB.OP.DBOP_DEL && planDb.type == TYPE.DBT_REQUEST) {
			if (planDb.plan_id.equals(soi_plan_id)) {
				print("Stop execution (SOI plan removed)");
				spec = null;
				plan = null;
				wpt_index = 0;
				state = this::idleAtSurface;
			}
		}
		if (planDb.op == PlanDB.OP.DBOP_SET && planDb.type == TYPE.DBT_REQUEST) {
			if (planDb.plan_id.equals(soi_plan_id)) {
				spec = (PlanSpecification) planDb.arg;
				plan = Plan.parse(spec);
				EstimatedState s = get(EstimatedState.class);
				if (s != null) {
					double[] pos = WGS84Utilities.toLatLonDepth(s);
					plan.scheduleWaypoints(System.currentTimeMillis(), pos[0], pos[1], speed);
				} else
					plan.scheduleWaypoints(System.currentTimeMillis(), speed);

				print("Received soi plan:");
				print("" + plan);
				wpt_index = 0;
				state = this::start_waiting;
			}
		}
	}
	
	@Consume
	public void on(PlanControl pControl) {		
		if (pControl.op == OP.PC_START && pControl.type == PlanControl.TYPE.PC_FAILURE) {
			if (pControl.plan_id.equals(soi_plan_id)) {
				// Error during execution!
				print("Detected error during execution. Ascending for report");
				errors.add(pControl.info);
				state = this::surface_to_report_error;
			}
		}
	}

	@Consume
	public void on(SoiCommand cmd) {
		if (cmd.type != SoiCommand.TYPE.SOITYPE_REQUEST)
			return;
		System.out.println("SoiCommand!\n" + cmd);
		SoiCommand reply = new SoiCommand();
		reply.command = cmd.command;
		reply.type = SoiCommand.TYPE.SOITYPE_ERROR;
		reply.src = remoteSrc;
		reply.dst = cmd.src;
		reply.dst_ent = cmd.src_ent;

		switch (cmd.command) {

		case SOICMD_EXEC:
			print("CMD: Exec plan!");
			if (cmd.plan == null) {
				plan = null;
				state = this::idleAtSurface;
			}
			else {
				plan = Plan.parse(cmd.plan);

				if (!plan.scheduledInTheFuture()) {
					EstimatedState s = get(EstimatedState.class);
					if (s != null) {
						double[] pos = WGS84Utilities.toLatLonDepth(s);
						plan.scheduleWaypoints(System.currentTimeMillis(), pos[0], pos[1], speed);
					} else
						plan.scheduleWaypoints(System.currentTimeMillis(), speed);
				}					
				wpt_index = 0;
				print("Start executing this plan:");
				print("" + plan);
				state = this::start_waiting;				
			}
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			reply.plan = plan.asImc();
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
			state = this::start_waiting;
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			break;

		case SOICMD_GET_PLAN:
			print("CMD: Get plan!");
			reply.plan = plan.asImc();
			reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
			break;

		case SOICMD_RESUME:
			print("CMD: Resume execution!");
			if (paused) {
				setPaused(false);
				state = this::start_waiting;
				return;
			}
			break;
		default:
			break;
		}

		try {
			send(reply);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// FIXME make sure we wait for reply transmission...
		sendViaIridium(reply, 60);
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

	public FSMState init(FollowRefState state) {
	    printFSMStateName("init");
		deadline = new Date(System.currentTimeMillis() + mins_timeout * 60 * 1000);
		return this::idleAtSurface;
	}

	public FSMState idleAtSurface(FollowRefState state) {
		printFSMStateName("idleAtSurface");
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		setLocation(pos[0], pos[1]);
		setDepth(0);
		return this::idle;
	}

	public FSMState idle(FollowRefState state) {
		printFSMStateName("idle");
		print("Waiting for plan...");
		return this::idle;
	}

	public FSMState exec(FollowRefState state) {
		printFSMStateName("exec");
		if (plan == null)
			return this::idleAtSurface;

		Waypoint wpt = plan.waypoint(wpt_index);

		if (wpt == null) {
			print("Finished executing plan.");
			if (cycle) {
				wpt_index = 0;
				EstimatedState s = get(EstimatedState.class);
				if (s != null) {
					double[] pos = WGS84Utilities.toLatLonDepth(s);
					plan.scheduleWaypoints(System.currentTimeMillis(), pos[0], pos[1], speed);
				} else
					plan.scheduleWaypoints(System.currentTimeMillis(), speed);
				print("Starting over...");
				SoiCommand reply = new SoiCommand();
				reply.command = COMMAND.SOICMD_GET_PLAN;
				reply.type = SoiCommand.TYPE.SOITYPE_SUCCESS;
				reply.src = remoteSrc;
				reply.dst = 0xFFFF;
				reply.plan = plan.asImc();
				sendViaIridium(reply, wait_secs);	
				
				try {
					send(reply);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return this::start_waiting;
			} else
				return this::idleAtSurface;
		}

		print("Executing wpt " + wpt_index);
		setLocation(wpt.getLatitude(), wpt.getLongitude());
		setSpeed();

		return this::start_descend;
	}

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
				speed = max_speed;
			} else {
				speed = Math.min(max_speed, dist / secs);
				speed = Math.max(min_speed, speed);
			}
		}

		setSpeed(speed, SpeedUnits.METERS_PS);
	}

	public FSMState descend(FollowRefState ref) {
		printFSMStateName("descend");
		setDepth(max_depth);
		
		secs_underwater++;
		if (underwaterForTooLong()) {
			errors.add("Underwater for too long ("+secs_underwater+")");
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
			if (min_depth < max_depth)
				print("Now ascending.");
			return this::ascend;
		}
		else
			return this::descend;
	}

	public FSMState ascend(FollowRefState ref) {
		printFSMStateName("ascend");
		setDepth(min_depth);
		
		secs_underwater++;
		if (underwaterForTooLong()) {
			errors.add("Underwater for too long ("+secs_underwater+"s)");
			return this::surface_to_report_error;
		}
		
		if (secs_underwater / 60 >= mins_under) {
			print("Periodic surface");
			return this::start_waiting;
		}

		if (arrivedXY()) {
            print("Arrived at waypoint " + wpt_index);
			wpt_index++;
			return this::start_waiting;
		}

		if (min_depth > 0 && arrivedZ() || !isUnderwater()) {
			if (secs_underwater / 60 >= mins_under) {
				print("Periodic surface");
				return this::start_waiting;
			} else {
				if (max_depth != min_depth)
					print("Now descending (underwater for " + secs_underwater + " seconds).");
				
				return this::start_descend;
			}

		} else
			return this::ascend;
	}
	
	public FSMState start_descend(FollowRefState ref) {
		printFSMStateName("start_descend");
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		
		secs_underwater++;
		if (underwaterForTooLong()) {
			errors.add("Underwater for too long ("+secs_underwater+")");
			return this::surface_to_report_error;
		}
		
		if (arrivedXY()) {
			print("Arrived at waypoint " + wpt_index);
			wpt_index++;
			return this::start_waiting;
		}
		
		setDepth(max_depth);
		setSpeed(descendSpeedRpm, SpeedUnits.RPM);
		
		if (pos[2] < 2 && pos[2] < max_depth) {
			return this::start_descend;
		}
		else {
			print("Vehicle now underwater. Setting speed according to ETA.");
			setSpeed();
			return this::descend;
		}
	}	
	
	public FSMState communicate(FollowRefState ref) {
		printFSMStateName("communicate");
		int seconds = wait_secs;
		if (plan != null && plan.waypoint(wpt_index) != null)
			seconds = Math.max(seconds, plan.waypoint(wpt_index).getDuration());
		
		while (!errors.isEmpty()) {
			String error = errors.get(0);
			if (error.length() > 132)
				error = error.substring(0, 132);
			sendViaSms("ERROR: "+error, seconds - count_secs - 1);
			sendViaIridium("ERROR: "+error, seconds - count_secs - 1);
			errors.remove(0);
		}
		
		// Send "DUNE" report
		if (count_secs == 0) {
			EnumSet<ReportControl.COMM_INTERFACE> itfs = EnumSet.of(ReportControl.COMM_INTERFACE.CI_GSM);
			itfs.add(ReportControl.COMM_INTERFACE.CI_SATELLITE);
			sendReport(itfs);
			sendViaIridium(createStateReport(), seconds - count_secs - 1);			
			print("Will wait "+seconds+" seconds");
		}
		
		if (count_secs >= seconds) {
			return this::exec;
		} 
		else {
			count_secs++;			
			return this::communicate;
		}
	}

	public FSMState start_waiting(FollowRefState ref) {
		printFSMStateName("start_waiting");
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		setLocation(pos[0], pos[1]);
		setDepth(0);
		setSpeed(speed, SpeedUnits.METERS_PS);

		print("Surfacing...");
		return this::wait;
	}
	
	public FSMState surface_to_report_error(FollowRefState ref) {
		printFSMStateName("surface_to_report_error");
		double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
		setLocation(pos[0], pos[1]);
		setDepth(0);
		setSpeed(0, SpeedUnits.METERS_PS);

		print("Surfacing to report error...");

		return this::report_error;
	}
	
	public FSMState report_error(FollowRefState ref) {
		printFSMStateName("report_error");
		VehicleMedium medium = get(VehicleMedium.class); 
		
		// arrived at surface
		if (medium != null && medium.medium == MEDIUM.VM_WATER) {
			print("Now at surface, starting communications.");
			secs_underwater = 0;
			count_secs = 0;
			return this::communicate;
		}
		else
			return this::report_error;
	}

	public FSMState wait(FollowRefState ref) {
		printFSMStateName("wait");
		secs_underwater++;
		if (underwaterForTooLong()) {
			errors.add("Underwater for too long ("+secs_underwater+")");
			return this::surface_to_report_error;
		}
		
		VehicleMedium medium = get(VehicleMedium.class); 
		// arrived at surface
		if (medium != null && medium.medium == MEDIUM.VM_WATER) {
			print("Now at surface, starting communications.");
			double[] pos = WGS84Utilities.toLatLonDepth(get(EstimatedState.class));
			setLocation(pos[0], pos[1]);
			secs_underwater = 0;
			count_secs = 0;
			return this::communicate;
		}
		else {			
			return this::wait;
		}
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

		tracker.connect(tracker.host_addr, tracker.host_port);
		tracker.join();
	}
}
