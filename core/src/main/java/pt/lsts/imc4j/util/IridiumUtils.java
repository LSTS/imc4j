package pt.lsts.imc4j.util;

import pt.lsts.imc4j.msg.Message;
import pt.lsts.imc4j.net.ImcFragmentHandler;

import java.util.List;

public class IridiumUtils {
    public static final int MAX_IRIDIUM_PAYLOAD_SIZE = 270 - 17;

    private IridiumUtils() {
    }

    public static List<Message> sliceMessage(Message msg) {
        byte[] msgPaylodBytes = msg.serializeFields();
        if (msgPaylodBytes.length < MAX_IRIDIUM_PAYLOAD_SIZE) {
            return List.of(msg);
        }

        try {
            ImcFragmentHandler msgFrag = new ImcFragmentHandler();
            return List.of(msgFrag.fragment(msg, MAX_IRIDIUM_PAYLOAD_SIZE
                    + ImcFragmentHandler.headerLength(msg)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
