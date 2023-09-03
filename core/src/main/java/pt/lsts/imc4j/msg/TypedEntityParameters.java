package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * This message can be used to query/report the entities and respective parameters in the system
 */
public class TypedEntityParameters extends Message {
	public static final int ID_STATIC = 2009;

	/**
	 * Operation to perform.
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
	 * Contains an optionally defined List of TypedEntityParameter as a response to a TypedEntityParamaters Request.
	 */
	@FieldType(
			type = IMCField.TYPE_MESSAGELIST
	)
	public ArrayList<TypedEntityParameter> parameters = new ArrayList<>();

	public String abbrev() {
		return "TypedEntityParameters";
	}

	public int mgid() {
		return 2009;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte((int)(op != null? op.value() : 0));
			_out.writeInt((int)request_id);
			SerializationUtils.serializePlaintext(_out, entity_name);
			SerializationUtils.serializeMsgList(_out, parameters);
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
			parameters = SerializationUtils.deserializeMsgList(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum OP {
		OP_REQUEST(0l),

		OP_REPLY(1l);

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
}
