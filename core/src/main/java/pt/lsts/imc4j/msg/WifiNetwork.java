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
 * This message is used to log wifi networks in the surroundings.
 */
public class WifiNetwork extends Message {
	public static final int ID_STATIC = 2012;

	/**
	 * Extended Service Set Identifier of the network
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String essid = "";

	/**
	 * MAC Address of the network.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String mac = "";

	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "dB"
	)
	public int signal = 0;

	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "dB"
	)
	public int noise = 0;

	@FieldType(
			type = IMCField.TYPE_INT8,
			units = "%"
	)
	public int ccq = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int channel = 0;

	@FieldType(
			type = IMCField.TYPE_FP32
	)
	public float freq = 0f;

	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String security = "";

	public String abbrev() {
		return "WifiNetwork";
	}

	public int mgid() {
		return 2012;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, essid);
			SerializationUtils.serializePlaintext(_out, mac);
			_out.writeShort(signal);
			_out.writeShort(noise);
			_out.writeByte(ccq);
			_out.writeByte(channel);
			_out.writeFloat(freq);
			SerializationUtils.serializePlaintext(_out, security);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			essid = SerializationUtils.deserializePlaintext(buf);
			mac = SerializationUtils.deserializePlaintext(buf);
			signal = buf.getShort();
			noise = buf.getShort();
			ccq = buf.get();
			channel = buf.get() & 0xFF;
			freq = buf.getFloat();
			security = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
