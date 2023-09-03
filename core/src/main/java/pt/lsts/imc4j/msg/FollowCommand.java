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
 * This maneuver follows a direct command given by an external entity.
 */
public class FollowCommand extends Message {
	public static final int ID_STATIC = 496;

	/**
	 * The IMC identifier of the source system that is allowed to provide command to this maneuver.
	 * If the value ''0xFFFF'' is used, any system is allowed to command references.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int control_src = 0;

	/**
	 * The entity identifier of the entity that is allowed to provide commands to this maneuver.
	 * If the value ''0xFF'' is used, any entity is allowed to command references.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int control_ent = 0;

	/**
	 * The ammount of time, in seconds, after which the maneuver will be terminated if no new command has
	 * been received. In other words, the controlling entity should send command updates in shorter periods than
	 * 'timeout'.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32
	)
	public float timeout = 0f;

	public String abbrev() {
		return "FollowCommand";
	}

	public int mgid() {
		return 496;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(control_src);
			_out.writeByte(control_ent);
			_out.writeFloat(timeout);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			control_src = buf.getShort() & 0xFFFF;
			control_ent = buf.get() & 0xFF;
			timeout = buf.getFloat();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
