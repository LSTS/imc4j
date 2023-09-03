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

/**
 * Used to request/reply a health check of a system
 */
public class HealthCheck extends Message {
	public static final int ID_STATIC = 2015;

	/**
	 * Operation to perform. OP.REQUEST should start the HealthCheck.
	 * Use OP.REPLY to report the progress.
	 * OP.QUERY asks if the task implements health checking.
	 * Upon receiving OP.Query, reply with OP.REPLY and STATUS.IMP or STATUS.NOT_IMP.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public OP op = OP.values()[0];

	/**
	 * Echoes the request_id in the request
	 */
	@FieldType(
			type = IMCField.TYPE_UINT32
	)
	public long request_id = 0;

	/**
	 * Entity Label of the task that's replying to the request
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String entity_name = "";

	/**
	 * Health status of the entity
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16,
			units = "Enumerated"
	)
	public STATUS status = STATUS.values()[0];

	/**
	 * Textual description of the health status
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String text = "";

	public String abbrev() {
		return "HealthCheck";
	}

	public int mgid() {
		return 2015;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte((int)(op != null? op.value() : 0));
			_out.writeInt((int)request_id);
			SerializationUtils.serializePlaintext(_out, entity_name);
			_out.writeShort((int)(status != null? status.value() : 0));
			SerializationUtils.serializePlaintext(_out, text);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			op = OP.valueOf(buf.get() & 0xFF);
			request_id = buf.getInt() & 0xFFFFFFFF;
			entity_name = SerializationUtils.deserializePlaintext(buf);
			status = STATUS.valueOf(buf.getShort() & 0xFFFF);
			text = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum OP {
		OP_REQUEST(0l),

		OP_REPLY(1l),

		OP_QUERY(2l);

		protected long value;

		OP(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static OP valueOf(long value) throws IllegalArgumentException {
			for (OP v : OP.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for OP: "+value);
		}
	}

	public enum STATUS {
		ST_FINISHED_OK(0l),

		ST_FINISHED_ERROR(1l),

		ST_IN_PROGRESS(2l),

		ST_IMP(3l),

		ST_NOT_IMP(4l),

		ST_ABORT(5l),

		ST_CONN_ERROR(6l),

		ST_SEND_ERROR(7l),

		ST_READ_ERROR(8l),

		ST_DEV_ERROR(9l),

		ST_DISC_ERROR(10l);

		protected long value;

		STATUS(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static STATUS valueOf(long value) throws IllegalArgumentException {
			for (STATUS v : STATUS.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for STATUS: "+value);
		}
	}
}
