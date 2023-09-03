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

public class IridiumMsgTxExtended extends Message {
	public static final int ID_STATIC = 2005;

	/**
	 * The request identifier used to receive transmission updates.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int req_id = 0;

	/**
	 * Time, in seconds, after which there will be no more atempts to transmit the message.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16,
			units = "s"
	)
	public int ttl = 0;

	/**
	 * Time in seconds since the Unix Epoch after which the recipient shall discard the message.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT32,
			units = "s"
	)
	public long expiration = 0;

	/**
	 * The unique identifier of this message's destination (e.g. lauv-xtreme-2, manta-0).
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String destination = "";

	/**
	 * Message data.
	 */
	@FieldType(
			type = IMCField.TYPE_RAWDATA
	)
	public byte[] data = new byte[0];

	public String abbrev() {
		return "IridiumMsgTxExtended";
	}

	public int mgid() {
		return 2005;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(req_id);
			_out.writeShort(ttl);
			_out.writeInt((int)expiration);
			SerializationUtils.serializePlaintext(_out, destination);
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
			req_id = buf.getShort() & 0xFFFF;
			ttl = buf.getShort() & 0xFFFF;
			expiration = buf.getInt() & 0xFFFFFFFF;
			destination = SerializationUtils.deserializePlaintext(buf);
			data = SerializationUtils.deserializeRawdata(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
