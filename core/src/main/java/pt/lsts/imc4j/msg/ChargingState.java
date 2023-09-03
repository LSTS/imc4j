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

/**
 * Reports if the vehicle is charging or not
 */
public class ChargingState extends Message {
	public static final int ID_STATIC = 910;

	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public IS_CHARGING is_charging = IS_CHARGING.values()[0];

	public String abbrev() {
		return "ChargingState";
	}

	public int mgid() {
		return 910;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte((int)(is_charging != null? is_charging.value() : 0));
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			is_charging = IS_CHARGING.valueOf(buf.get() & 0xFF);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum IS_CHARGING {
		CSTATE_NOT_CHARGING(0l),

		CSTATE_IS_CHARGING(1l);

		protected long value;

		IS_CHARGING(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static IS_CHARGING valueOf(long value) throws IllegalArgumentException {
			for (IS_CHARGING v : IS_CHARGING.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for IS_CHARGING: "+value);
		}
	}
}
