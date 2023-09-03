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
 * Entity parameter with all the data that defines an entity parameter.
 */
public class TypedEntityParameter extends Message {
	public static final int ID_STATIC = 2008;

	/**
	 * Name of the parameter.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String name = "";

	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public TYPE type = TYPE.values()[0];

	/**
	 * Default value of the parameter.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String default_value = "";

	/**
	 * The units of the field, if applicable
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String units = "";

	/**
	 * Description of the parameter
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String description = "";

	/**
	 * Optional. Min value of the parameter
	 */
	@FieldType(
			type = IMCField.TYPE_FP32
	)
	public float min_value = 0f;

	/**
	 * Optional. Max value of the parameter
	 */
	@FieldType(
			type = IMCField.TYPE_FP32
	)
	public float max_value = 0f;

	/**
	 * When the parameter is a list, list_min_size indicates the minimum size of the list
	 */
	@FieldType(
			type = IMCField.TYPE_UINT32
	)
	public long list_min_size = 0;

	/**
	 * When the parameter is a list, list_max_size indicates the maximum size of the list
	 */
	@FieldType(
			type = IMCField.TYPE_UINT32
	)
	public long list_max_size = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public VISIBILITY visibility = VISIBILITY.values()[0];

	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public SCOPE scope = SCOPE.values()[0];

	public String abbrev() {
		return "TypedEntityParameter";
	}

	public int mgid() {
		return 2008;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, name);
			_out.writeByte((int)(type != null? type.value() : 0));
			SerializationUtils.serializePlaintext(_out, default_value);
			SerializationUtils.serializePlaintext(_out, units);
			SerializationUtils.serializePlaintext(_out, description);
			_out.writeFloat(min_value);
			_out.writeFloat(max_value);
			_out.writeInt((int)list_min_size);
			_out.writeInt((int)list_max_size);
			_out.writeByte((int)(visibility != null? visibility.value() : 0));
			_out.writeByte((int)(scope != null? scope.value() : 0));
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			name = SerializationUtils.deserializePlaintext(buf);
			type = TYPE.valueOf(buf.get() & 0xFF);
			default_value = SerializationUtils.deserializePlaintext(buf);
			units = SerializationUtils.deserializePlaintext(buf);
			description = SerializationUtils.deserializePlaintext(buf);
			min_value = buf.getFloat();
			max_value = buf.getFloat();
			list_min_size = buf.getInt() & 0xFFFFFFFF;
			list_max_size = buf.getInt() & 0xFFFFFFFF;
			visibility = VISIBILITY.valueOf(buf.get() & 0xFF);
			scope = SCOPE.valueOf(buf.get() & 0xFF);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum TYPE {
		TYPE_BOOL(1l),

		TYPE_INT(2l),

		TYPE_FLOAT(3l),

		TYPE_STRING(4l),

		TYPE_LIST_BOOL(5l),

		TYPE_LIST_INT(6l),

		TYPE_LIST_FLOAT(7l),

		TYPE_LIST_STRING(8l);

		protected long value;

		TYPE(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static TYPE valueOf(long value) throws IllegalArgumentException {
			for (TYPE v : TYPE.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for TYPE: "+value);
		}
	}

	public enum VISIBILITY {
		VISIBILITY_USER(0l),

		VISIBILITY_DEVELOPER(1l);

		protected long value;

		VISIBILITY(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static VISIBILITY valueOf(long value) throws IllegalArgumentException {
			for (VISIBILITY v : VISIBILITY.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for VISIBILITY: "+value);
		}
	}

	public enum SCOPE {
		SCOPE_GLOBAL(0l),

		SCOPE_IDLE(1l),

		SCOPE_PLAN(2l),

		SCOPE_MANEUVER(3l);

		protected long value;

		SCOPE(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static SCOPE valueOf(long value) throws IllegalArgumentException {
			for (SCOPE v : SCOPE.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for SCOPE: "+value);
		}
	}
}
