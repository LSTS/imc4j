package pt.lsts.imc.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc.annotations.FieldType;
import pt.lsts.imc.annotations.IMCField;
import pt.lsts.imc.util.SerializationUtils;

/**
 * This message holds a list of inline data samples produced by one or more vehicles in the past.
 * It is used to transfer data over disruption tolerant networks.
 */
public class CompressedHistory extends Message {
	public static final int ID_STATIC = 185;

	/**
	 * All data sent inside this message will have offsets relative to this latitude.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "°"
	)
	public float base_lat = 0f;

	/**
	 * All data sent inside this message will have offsets relative to this longitude.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "°"
	)
	public float base_lon = 0f;

	/**
	 * All data sent inside this message will use this time as the origin (0).
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "s"
	)
	public float base_time = 0f;

	/**
	 * A message-list of HistoricSample messages compressed with GZip algorithm.
	 */
	@FieldType(
			type = IMCField.TYPE_RAWDATA
	)
	public byte[] data = new byte[0];

	public String abbrev() {
		return "CompressedHistory";
	}

	public int mgid() {
		return 185;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeFloat(base_lat);
			_out.writeFloat(base_lon);
			_out.writeFloat(base_time);
			SerializationUtils.serializeRawdata(_out, data);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			base_lat = buf.getFloat();
			base_lon = buf.getFloat();
			base_time = buf.getFloat();
			data = SerializationUtils.deserializeRawdata(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
