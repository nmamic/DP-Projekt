package COComponents;

import URBComponents.URBProces;
import components.Linker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class COProces extends URBProces {

    // causal_past_i[0..N-1]
    private final int[] causalPast;

    // msg_set_i
    private final Set<COMessage> msgSet = new HashSet<>();
    private int seq = 0;
    private boolean done = false;

    public COProces(Linker linker) {
        super(linker);
        this.causalPast = new int[N];
    }

    public synchronized void CO_Broadcast(String text) {

        done = false;
        int[] snapshot = Arrays.copyOf(causalPast, N);
        COMessage message = new COMessage(
                myId,
                ++seq,
                snapshot,
                text.replace(" ", "_")
        );

        String encoded = message.encode();
        super.URB_Broadcast(encoded);
        while (!done) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


    @Override
    public synchronized void URB_Deliver(String m) {
        super.URB_Deliver(m);

        String content = stripUrbPrefix(m);
        COMessage message = COMessage.decode(content);

        tryCOMessage(message);
    }

    private String stripUrbPrefix(String m) {
        int index = m.indexOf(":");

        if (index == -1) {
            return m;
        }

        return m.substring(index + 1);
    }

    private void tryCOMessage(COMessage message) {

        if (canDeliver(message)) {
            CO_Deliver(message);

            int j = message.sender;
            causalPast[j] = message.causalPast[j] + 1;

            if (message.sender == myId) {
                done = true;
                notifyAll();
            }

            checkMsgSet();

        } else {

            msgSet.add(message);
        }
    }

    private boolean canDeliver(COMessage message) {
        for (int k = 0; k < N; k++) {
            if (causalPast[k] < message.causalPast[k]) {
                return false;
            }
        }

        return true;
    }


    private void CO_Deliver(COMessage message) {

        System.out.println(
                "Proces " + myId +
                        " CO-deliverao: " + message.text.replace("_", " ") +
                        " od P" + message.sender +
                        " seq=" + message.seq +
                        " causalPast=" + Arrays.toString(message.causalPast)
        );
    }

    private void checkMsgSet() {
        boolean deliveredSomething;

        do {
            deliveredSomething = false;

            Iterator<COMessage> iterator = msgSet.iterator();

            while (iterator.hasNext()) {
                COMessage message = iterator.next();

                if (canDeliver(message)) {

                    CO_Deliver(message);


                    int j = message.sender;
                    causalPast[j] = message.causalPast[j] + 1;
                    if (message.sender == myId) {
                        done = true;
                        notifyAll();
                    }

                    iterator.remove();

                    deliveredSomething = true;
                }
            }

        } while (deliveredSomething);
    }
}