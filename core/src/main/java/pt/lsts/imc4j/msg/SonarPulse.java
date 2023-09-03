package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;

/**
 * Information regarding a sent/received Sonar pulse.
 */
public class SonarPulse extends Message {
	public static final int ID_STATIC = 2013;

	/**
	 * Frequency of the sent/received sonar pulse.
	 */
	@FieldType(
			type = IMCField.TYPE_INT32,
			units = "Hz"
	)
	public int frequency = 0;

	/**
	 * Pulse Length of the sonar pulse.
	 */
	@FieldType(
			type = IMCField.TYPE_INT32,
			units = "ms"
	)
	public int pulse_length = 0;

	/**
	 * Time Delay of the sonar pulse.
	 */
	@FieldType(
			type = IMCField.TYPE_INT32,
			units = "ms"
	)
	public int time_delay = 0;

	/**
	 * Doppler shift added to the sonar pulse in retransmission
	 */
	@FieldType(
			type = IMCField.TYPE_INT32,
			units = "m/s"
	)
	public int simulated_speed = 0;

	public String abbrev() {
		return "SonarPulse";
	}

	public int mgid() {
		return 2013;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeInt((int)frequency);
			_out.writeInt((int)pulse_length);
			_out.writeInt((int)time_delay);
			_out.writeInt((int)simulated_speed);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			frequency = buf.getInt();
			pulse_length = buf.getInt();
			time_delay = buf.getInt();
			simulated_speed = buf.getInt();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
