package pt.lsts.imc.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc.annotations.FieldType;
import pt.lsts.imc.annotations.IMCField;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.imc.util.SerializationUtils;
import pt.lsts.imc.util.TupleList;

/**
 * The Station Keeping maneuver makes the vehicle come to the surface
 * and then enter a given circular perimeter around a waypoint coordinate
 * for a certain amount of time.
 */
public class StationKeeping extends Maneuver {
	public static final int ID_STATIC = 461;

	/**
	 * WGS-84 Latitude.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			max = 1.5707963267948966,
			min = -1.5707963267948966,
			units = "rad"
	)
	public double lat = 0;

	/**
	 * WGS-84 Longitude.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			max = 3.141592653589793,
			min = -3.141592653589793,
			units = "rad"
	)
	public double lon = 0;

	/**
	 * Maneuver reference in the z axis. Use z_units to specify
	 * whether z represents depth, altitude or other.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float z = 0f;

	/**
	 * Units of the z reference.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public ZUnits z_units = ZUnits.values()[0];

	/**
	 * Radius.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float radius = 0f;

	/**
	 * Duration (0 for unlimited).
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16,
			units = "s"
	)
	public int duration = 0;

	/**
	 * The value of the desired speed, in the scale specified
	 * by the "Speed Units" field.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32
	)
	public float speed = 0f;

	/**
	 * Indicates the units used for the speed value.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public SpeedUnits speed_units = SpeedUnits.values()[0];

	/**
	 * Custom settings for maneuver.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT,
			units = "TupleList"
	)
	public TupleList custom = new TupleList("");

	public String abbrev() {
		return "StationKeeping";
	}

	public int mgid() {
		return 461;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeDouble(lat);
			_out.writeDouble(lon);
			_out.writeFloat(z);
			_out.writeByte((int)(z_units != null? z_units.value() : 0));
			_out.writeFloat(radius);
			_out.writeShort(duration);
			_out.writeFloat(speed);
			_out.writeByte((int)(speed_units != null? speed_units.value() : 0));
			SerializationUtils.serializePlaintext(_out, custom == null? null : custom.toString());
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			lat = buf.getDouble();
			lon = buf.getDouble();
			z = buf.getFloat();
			z_units = ZUnits.valueOf(buf.get() & 0xFF);
			radius = buf.getFloat();
			duration = buf.getShort() & 0xFFFF;
			speed = buf.getFloat();
			speed_units = SpeedUnits.valueOf(buf.get() & 0xFF);
			custom = new TupleList(SerializationUtils.deserializePlaintext(buf));
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
