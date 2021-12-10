package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * Acoustic range measurement.
 */
public class UamRxRange extends Message {
	public static final int ID_STATIC = 817;

	/**
	 * The sequence identifier of the ranging request.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int seq = 0;

	/**
	 * The canonical name of the ranged system.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String sys = "";

	/**
	 * The actual range. Negative values denote invalid measurements.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float value = 0f;

	public String abbrev() {
		return "UamRxRange";
	}

	public int mgid() {
		return 817;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(seq);
			SerializationUtils.serializePlaintext(_out, sys);
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
			seq = buf.getShort() & 0xFFFF;
			sys = SerializationUtils.deserializePlaintext(buf);
			value = buf.getFloat();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
