package pt.lsts.autonomy.util;

import pt.lsts.autonomy.soi.SoiExecutive;
import pt.lsts.imc4j.msg.EstimatedState;
import pt.lsts.imc4j.util.WGS84Utilities;

public class DistanceUtil {

    private DistanceUtil() {}

    public static boolean limitReached(SoiExecutive soi, EstimatedState msg, double target_lat, double target_lon,
                                       boolean stateAscendingDescending) {

        double[] cur_pos = WGS84Utilities.toLatLonDepth(msg);

        double dist = WGS84Utilities.distance(cur_pos[0], cur_pos[1], target_lat, target_lon);
        double min_dist = 4 * cur_pos[2];

        if (dist < min_dist) {
            soi.print("!!!!!!!!!! " + (stateAscendingDescending ? "Continue" : "Starting") +
                    " to ascend, getting close to destination.  " + Math.round(dist) + " < "
                    + Math.round(min_dist) + " (cur depth " + Math.round(cur_pos[2]) + ")");
            return true;
        }
        else {
            return false;
        }
    }

}
