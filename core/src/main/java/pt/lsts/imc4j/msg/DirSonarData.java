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
 * This message contains the data acquired by a single sonar measurement whose angle can be controlled.
 */
public class DirSonarData extends Message {
	public static final int ID_STATIC = 2019;

	/**
	 * Pose of this sonar measurement, in relation to the body frame.
	 */
	@FieldType(
			type = IMCField.TYPE_MESSAGE
	)
	public DeviceState pose = null;

	/**
	 * Data acquired by the measurement.
	 */
	@FieldType(
			type = IMCField.TYPE_MESSAGE
	)
	public SonarData measurement = null;

	public String abbrev() {
		return "DirSonarData";
	}

	public int mgid() {
		return 2019;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializeInlineMsg(_out, pose);
			SerializationUtils.serializeInlineMsg(_out, measurement);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			pose = SerializationUtils.deserializeInlineMsg(buf);
			measurement = SerializationUtils.deserializeInlineMsg(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
