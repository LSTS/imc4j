package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * This message is used to restrict the vehicle from using some communication means.
 */
public class CommRestriction extends Message {
	public static final int ID_STATIC = 2010;

	/**
	 * The restricted communication means.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Bitfield"
	)
	public EnumSet<RESTRICTION> restriction = EnumSet.noneOf(RESTRICTION.class);

	/**
	 * Textual description for why this restriction is needed.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String reason = "";

	public String abbrev() {
		return "CommRestriction";
	}

	public int mgid() {
		return 2010;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			long _restriction = 0;
			if (restriction != null) {
				for (RESTRICTION __restriction : restriction.toArray(new RESTRICTION[0])) {
					_restriction += __restriction.value();
				}
			}
			_out.writeByte((int)_restriction);
			SerializationUtils.serializePlaintext(_out, reason);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			long restriction_val = buf.get() & 0xFF;
			restriction.clear();
			for (RESTRICTION RESTRICTION_op : RESTRICTION.values()) {
				if ((restriction_val & RESTRICTION_op.value()) == RESTRICTION_op.value()) {
					restriction.add(RESTRICTION_op);
				}
			}
			reason = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum RESTRICTION {
		MEAN_SATELLITE(0x01l),

		MEAN_ACOUSTIC(0x02l),

		MEAN_WIFI(0x04l),

		MEAN_GSM(0x08l);

		protected long value;

		RESTRICTION(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static RESTRICTION valueOf(long value) throws IllegalArgumentException {
			for (RESTRICTION v : RESTRICTION.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for RESTRICTION: "+value);
		}
	}
}
