package URBComponents;

import components.Linker;
import components.Msg;
import components.Process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FIFOProces extends Process {

    private volatile boolean crashed = false;

    public void markCrashed() {
        crashed = true;
    }

    // da ne obrađujemo istu poruku više puta
    private final Set<String> seen = new HashSet<>();

    // za svakog pošiljatelja čuvamo poruke koje su stigle prerano
    private final Map<Integer, TreeMap<Integer, String>> pending = new HashMap<>();

    // sljedeći redni broj koji očekujemo od svakog pošiljatelja
    private final int[] nextExpectedSeq;

    // sljedeći redni broj koji šaljemo kao lokalni pošiljatelj
    private final int[] nextSendSeq;

    public FIFOProces(Linker linker) {
        super(linker);

        nextExpectedSeq = new int[N];
        nextSendSeq = new int[N];

        for (int i = 0; i < N; i++) {
            nextExpectedSeq[i] = 1;
            nextSendSeq[i] = 1;
            pending.put(i, new TreeMap<Integer, String>());
        }
    }

    public void FIFO_Broadcast(String m) {
        int seq = nextSendSeq[myId]++;
        String content = myId + " " + seq + " " + m;

        // lokalna dostava
        handleMsg(new Msg(myId, myId, "MSG", content), myId, "MSG");

        // slanje svima ostalima
        for (int j = 0; j < N; j++) {
            if (j != myId) {
                comm.sendMsg(j, "MSG", content);
            }
        }
    }

    public void FIFO_Deliver(String m) {
        if (crashed) return;
        System.out.println("Proces " + myId + " isporucio: " + m);
    }

    @Override
    public synchronized void handleMsg(Msg m, int src, String tag) {
        if (crashed) return;
        String content = m.getMessage().trim();

        // format: origin seq payload...
        String[] parts = content.split("\\s+", 3);
        if (parts.length < 3) {
            return;
        }

        int origin = Integer.parseInt(parts[0]);
        int seq = Integer.parseInt(parts[1]);
        String body = parts[2];

        String key = origin + ":" + seq;

        // samo prvi put kad vidimo poruku
        if (seen.add(key)) {
            // rebroadcast prema ostalima, ali ne nazad na izvor i ne sebi
            if (src != myId) {
                for (int j = 0; j < N; j++) {
                    if (j != myId && j != src) {
                        comm.sendMsg(j, "MSG", content);
                    }
                }
            }

            pending.get(origin).put(seq, body);
            deliverInOrder(origin);
        }
    }

    private void deliverInOrder(int origin) {
        TreeMap<Integer, String> queue = pending.get(origin);

        while (queue.containsKey(nextExpectedSeq[origin])) {
            int currentSeq = nextExpectedSeq[origin];
            String body = queue.remove(currentSeq);

            // ispis uključuje origin, seq, i body
            FIFO_Deliver("sender = " + origin + ", seq = " + currentSeq + ", msg = " + body);
            nextExpectedSeq[origin]++;
        }
    }
}