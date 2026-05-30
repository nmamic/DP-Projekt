package COComponents;

import java.util.Objects;

public class COMessage {
    int sender;
    int seq;
    int[] causalPast;
    String text;

    COMessage(int sender, int seq, int[] causalPast, String text) {
        this.sender = sender;
        this.seq = seq;
        this.causalPast = causalPast;
        this.text = text;
    }

    String id() {
        return sender + ":" + seq;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof COMessage other)) {
            return false;
        }

        return this.sender == other.sender && this.seq == other.seq;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, seq);
    }

    String encode() {
        StringBuilder vector = new StringBuilder();

        for (int i = 0; i < causalPast.length; i++) {
            if (i > 0) {
                vector.append(",");
            }

            vector.append(causalPast[i]);
        }

        return sender + ";" +
                seq + ";" +
                vector + ";" +
                text;
    }

    static COMessage decode(String content) {
        String[] parts = content.split(";", 4);

        int sender = Integer.parseInt(parts[0]);
        int seq = Integer.parseInt(parts[1]);

        String[] vectorParts = parts[2].split(",");
        int[] vector = new int[vectorParts.length];

        for (int i = 0; i < vectorParts.length; i++) {
            vector[i] = Integer.parseInt(vectorParts[i]);
        }

        String text = parts[3];

        return new COMessage(sender, seq, vector, text);
    }

}