package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

public class FollowCommandState extends Message {
	public static final int ID_STATIC = 498;

	/**
	 * The IMC identifier of the source system that is allowed to control the vehicle.
	 * If the value ''0xFFFF'' is used, any system is allowed to command references.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int control_src = 0;

	/**
	 * The entity identifier of the entity that is allowed to control the vehicle.
	 * If the value ''0xFF'' is used, any entity is allowed to command references.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int control_ent = 0;

	/**
	 * Command currently being followed.
	 */
	@FieldType(
			type = IMCField.TYPE_MESSAGE
	)
	public Command command = null;

	/**
	 * Current state of execution.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public STATE state = STATE.values()[0];

	public String abbrev() {
		return "FollowCommandState";
	}

	public int mgid() {
		return 498;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(control_src);
			_out.writeByte(control_ent);
			SerializationUtils.serializeInlineMsg(_out, command);
			_out.writeByte((int)(state != null? state.value() : 0));
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
			command = SerializationUtils.deserializeInlineMsg(buf);
			state = STATE.valueOf(buf.get() & 0xFF);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum STATE {
		FC_WAIT(1l),

		FC_MOVING(2l),

		FC_STOPPED(3l),

		FC_BAD_COMMAND(4l),

		FC_TIMEOUT(5l);

		protected long value;

		STATE(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static STATE valueOf(long value) throws IllegalArgumentException {
			for (STATE v : STATE.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for STATE: "+value);
		}
	}
}
