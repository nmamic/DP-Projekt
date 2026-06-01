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
    private final Set<COMessage> msgSet = new HashSet<>();
    private int seq = 0;
    private boolean done = false;

    //za testiranje
    private final Set<String> deliveredCOTexts = new HashSet<>();

    // koristi se samo za testiranje msgSet-a
    private boolean testDelayEnabled = false;

    public COProces(Linker linker) {
        super(linker);
        this.causalPast = new int[N];
    }

    public void enableTestDelay() {
        this.testDelayEnabled = true;
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

        System.out.println(
                "[P" + myId + "] CO_Broadcast: " + text +
                        ", causalPast=" + Arrays.toString(snapshot)
        );

        super.URB_Broadcast(message.encode());

        while (!done) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    //za test
    public synchronized void waitUntilDelivered(String text) {
        while (!deliveredCOTexts.contains(text)) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    //
    @Override
    public void URB_Deliver(String m) {

        String content = stripUrbPrefix(m);
        COMessage message = COMessage.decode(content);

        //test delay - za simulaciju testa
        if (testDelayEnabled
                && myId == 0
                && message.sender == 2
                && message.text.replace("_", " ").equals("m0")) {

            System.out.println("[P0] TEST DELAY: Odgađam obradu m0 od P2 za 10 sekundi.");

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                synchronized (COProces.this) {
                    System.out.println("[P0] TEST DELAY: Sada nastavljam obradu m0 od P2.");
                    tryCOMessage(message);
                }
            }).start();

            return;
        }

        synchronized (this) {
            tryCOMessage(message);
        }
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
            deliverAndUpdate(message);
            checkMsgSet();
        } else {
            System.out.println(
                    "[P" + myId + "] NE MOGU CO-deliverati: " +
                            message.text.replace("_", " ") +
                            " od P" + message.sender
            );

            System.out.println(
                    "[P" + myId + "] Spremam poruku u msgSet."
            );

            System.out.println(
                    "[P" + myId + "] causalPast poruke = " +
                            Arrays.toString(message.causalPast)
            );

            System.out.println(
                    "[P" + myId + "] moj causalPast = " +
                            Arrays.toString(causalPast)
            );

            msgSet.add(message);

            System.out.println(
                    "[P" + myId + "] msgSet.size = " + msgSet.size()
            );
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

    private void deliverAndUpdate(COMessage message) {

        String text = message.text.replace("_", " ");

        //za test
        deliveredCOTexts.add(text);

        int j = message.sender;
        causalPast[j] = message.causalPast[j] + 1;

        System.out.println(
                "[P" + myId + "] CO_Deliver: " + text +
                        " od P" + message.sender +
                        ", seq=" + message.seq +
                        ", causalPast poruke=" + Arrays.toString(message.causalPast) +
                        ", moj causalPast nakon delivera=" + Arrays.toString(causalPast)
        );

        if (message.sender == myId) {
            done = true;
        }

        notifyAll();
    }

    private void checkMsgSet() {

        boolean deliveredSomething;

        do {
            deliveredSomething = false;

            Iterator<COMessage> iterator = msgSet.iterator();

            while (iterator.hasNext()) {
                COMessage message = iterator.next();

                if (canDeliver(message)) {
                    System.out.println(
                            "[P" + myId + "] Poruka iz msgSet sada može biti CO-deliverana: " +
                                    message.text.replace("_", " ")
                    );

                    iterator.remove();
                    deliverAndUpdate(message);

                    System.out.println(
                            "[P" + myId + "] msgSet.size = " + msgSet.size()
                    );

                    deliveredSomething = true;
                }
            }

        } while (deliveredSomething);
    }
}