package pt.lsts.imc.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc.annotations.FieldType;
import pt.lsts.imc.annotations.IMCField;

/**
 * Chlorophyll measurement.
 */
public class Chlorophyll extends Message {
	public static final int ID_STATIC = 289;

	/**
	 * Chlorophyll reading.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "µg/L"
	)
	public float value = 0f;

	public String abbrev() {
		return "Chlorophyll";
	}

	public int mgid() {
		return 289;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeFloat(value);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			value = buf.getFloat();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
