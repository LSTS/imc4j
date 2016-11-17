package pt.lsts.imc.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.nio.ByteBuffer;
import pt.lsts.imc.annotations.FieldType;
import pt.lsts.imc.annotations.IMCField;
import pt.lsts.imc.util.SerializationUtils;
import pt.lsts.imc.util.TupleList;

/**
 * Low level maneuver that sends a (heading, roll, speed, ...)
 * reference to a controller of the vehicle and then optionally
 * lingers for some time.
 */
public class LowLevelControl extends Maneuver {
	public static final int ID_STATIC = 455;

	/**
	 * Control command: can be of type DesiredZ, DesiredHeading,
	 * DesiredRoll, DesiredPitch, DesiredSpeed, DesiredThrottle or DesiredPath.
	 */
	@FieldType(
			type = IMCField.TYPE_MESSAGE
	)
	public ControlCommand control = null;

	/**
	 * Duration of the control.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16,
			units = "s"
	)
	public int duration = 0;

	/**
	 * Custom settings for maneuver.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT,
			units = "TupleList"
	)
	public TupleList custom = new TupleList("");

	public int mgid() {
		return 455;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializeInlineMsg(_out, control);
			_out.writeShort(duration);
			SerializationUtils.serializePlaintext(_out, custom == null? null : custom.toString());
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			control = SerializationUtils.deserializeInlineMsg(buf);
			duration = buf.getShort() & 0xFFFF;
			custom = new TupleList(SerializationUtils.deserializePlaintext(buf));
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
