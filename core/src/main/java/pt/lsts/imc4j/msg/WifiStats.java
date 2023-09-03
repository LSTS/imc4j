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
 * This message is used to log wifi connection statistics, heavily influenced by the stats available in ubiquiti radios.
 */
public class WifiStats extends Message {
	public static final int ID_STATIC = 2011;

	/**
	 * MAC address of the associated radio.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String mac = "";

	/**
	 * Last IP address of the associated radio.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String ip = "";

	/**
	 * Client Connection Quality indicator
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "%"
	)
	public int ccq = 0;

	/**
	 * Noise Floor (measure of the signal created from the sum of all the noise sources
	 * and unwanted signals within a measurement system, where noise is defined as any signal other
	 * than the one being monitored).
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "dB"
	)
	public int noise_floor = 0;

	/**
	 * Measure of the signal of the associated radio.
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "dB"
	)
	public int signal = 0;

	/**
	 * Received Signal Strength Indicator, in arbitraty units. The bigger the RSSI, the better
	 * the connection quality.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int rssi = 0;

	/**
	 * Reception data rate for the associated radio, -1 if not available.
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "Mbps"
	)
	public int rx_rate = 0;

	/**
	 * Transmission data rate for the associated radio, -1 if not available.
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "Mbps"
	)
	public int tx_rate = 0;

	/**
	 * Latency of transmission to the associated radio, -1 if not available.
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "s"
	)
	public int tx_latency = 0;

	/**
	 * Power of transmission to the associated radio, -1 if not available.
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "s"
	)
	public int tx_power = 0;

	/**
	 * Amount of bytes already received from the associated radio.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT32,
			units = "byte"
	)
	public long rx_count = 0;

	/**
	 * Amount of bytes already transmitted to the associated radio.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT32,
			units = "byte"
	)
	public long tx_count = 0;

	/**
	 * Distance for the associated radio, -1 if not available.
	 */
	@FieldType(
			type = IMCField.TYPE_INT16,
			units = "m"
	)
	public int distance = 0;

	public String abbrev() {
		return "WifiStats";
	}

	public int mgid() {
		return 2011;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, mac);
			SerializationUtils.serializePlaintext(_out, ip);
			_out.writeByte(ccq);
			_out.writeShort(noise_floor);
			_out.writeShort(signal);
			_out.writeShort(rssi);
			_out.writeShort(rx_rate);
			_out.writeShort(tx_rate);
			_out.writeShort(tx_latency);
			_out.writeShort(tx_power);
			_out.writeInt((int)rx_count);
			_out.writeInt((int)tx_count);
			_out.writeShort(distance);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			mac = SerializationUtils.deserializePlaintext(buf);
			ip = SerializationUtils.deserializePlaintext(buf);
			ccq = buf.get() & 0xFF;
			noise_floor = buf.getShort();
			signal = buf.getShort();
			rssi = buf.getShort() & 0xFFFF;
			rx_rate = buf.getShort();
			tx_rate = buf.getShort();
			tx_latency = buf.getShort();
			tx_power = buf.getShort();
			rx_count = buf.getInt() & 0xFFFFFFFF;
			tx_count = buf.getInt() & 0xFFFFFFFF;
			distance = buf.getShort();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
