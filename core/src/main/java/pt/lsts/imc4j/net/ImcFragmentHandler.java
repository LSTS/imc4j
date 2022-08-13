package pt.lsts.imc4j.net;

import pt.lsts.imc4j.msg.Message;
import pt.lsts.imc4j.msg.MessagePart;
import pt.lsts.imc4j.util.SerializationUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ImcFragmentHandler {
    private static int uid = (int) (Math.random() * 255.0);
    private static int headerLength = 0;
    private final Map<Integer, List<MessagePart>> incoming = new LinkedHashMap<>();

    public ImcFragmentHandler() {
    }
    public static int headerLength(Message message) {
        if (headerLength > 0)
            return headerLength;

        headerLength = message == null ? 0 :
                message.serialize().length - message.size();
        return headerLength;
    }

    /**
     * Add an incoming fragment
     *
     * @param fragment
     *            The fragment to add to the list of incoming fragments
     * @return The resulting assembled message if this was the last fragment
     *         required to build it or <code>null</code> if this is not the last
     *         fragment.
     */
    public Message setFragment(MessagePart fragment) {
        int hash = (fragment.src + "" + fragment.uid).hashCode();
        if (!this.incoming.containsKey(hash)) {
            this.incoming.put(hash, new LinkedList<>());
        }

        this.mergeFragment(hash, this.incoming, fragment);
        if (this.incoming.get(hash).size() >= fragment.num_frags) {
            List<MessagePart> parts = this.incoming.get(hash);
            this.incoming.remove(hash);

            try {
                return this.reassemble(parts);
            } catch (Exception var5) {
                var5.printStackTrace();
            }
        }

        return null;
    }

    private void mergeFragment(int hash, Map<Integer, List<MessagePart>> incoming, MessagePart fragment) {
        List<MessagePart> ic = incoming.get(hash);
        if (ic.isEmpty()) {
            ic.add(fragment);
        } else {
            for (MessagePart mp : ic) {
                if (fragment.frag_number == mp.frag_number) {
                    return;
                }
            }

            ic.add(fragment);
        }
    }

    /**
     * Given a list of message fragments try to reassemble the fragments into an IMCMessage
     * @param parts The fragments to process
     * @return The resulting assembled message
     * @throws Exception In case the fragments do not result in a valid message
     */
    public Message reassemble(List<MessagePart> parts) throws Exception {
        parts.sort(Comparator.comparingInt(o -> o.frag_number));

        int totalSize = 0;
        for (MessagePart p : parts) {
            totalSize += p.data.length;
        }
        byte[] res = new byte[totalSize];
        int pos = 0;
        for (MessagePart p : parts) {
            System.arraycopy(p.data, 0, res, pos, p.data.length);
            pos += p.data.length;
        }

        return SerializationUtils.deserializeMessage(res);
    }

    /**
     * Fragment a message into smaller MessagePart's
     * @param message The message to be fragmented
     * @param maxFragLength The maximum size of any generated MessagePart. Must be greater than 25.
     * @return A List of messages containing fragments of the original message
     * @throws Exception In case the message cannot be fragmented
     */
    public MessagePart[] fragment(Message message, int maxFragLength) throws Exception {
        int id = uid = (uid + 1) % 255;
        int dataFragLength = maxFragLength - headerLength(message) - 5;
        byte[] data = message.serialize();
        int part = 0;
        int pos = 0;
        List<MessagePart> parts = new LinkedList<>();
        int numfrags = (int)Math.ceil((double)data.length / (double)dataFragLength);

        while(pos < data.length) {
            int remaining = data.length - pos;
            int size = Math.min(dataFragLength, remaining);
            byte[] partData = Arrays.copyOfRange(data, pos, pos + size);
            pos += size;
            MessagePart tmp = new MessagePart();
            tmp.uid = (short) id;
            tmp.frag_number = (short) (part++);
            tmp.num_frags = (short) numfrags;
            tmp.data = partData;
            tmp.src = message.src;
            tmp.src_ent = message.src_ent;
            tmp.dst = message.dst;
            tmp.dst_ent = message.dst_ent;
            tmp.timestamp = message.timestamp;
            parts.add(tmp);
        }

        return parts.toArray(new MessagePart[0]);
    }
}
