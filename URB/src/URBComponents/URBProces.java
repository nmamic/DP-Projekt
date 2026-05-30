package URBComponents;

import components.Linker;
import components.Msg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URBProces extends components.Process {

    private Set<String> received = new HashSet<>();

    private List<String> delivered = new ArrayList<>();

    public URBProces(Linker linker) {
        super(linker);
    }

    public void URB_Broadcast(String m) {
        String fullMsg = myId + ":" + m;
        handleMsg(new Msg(myId, myId, "MSG", fullMsg), myId, "MSG");
    }

    public void URB_Deliver(String m) {
        System.out.println("URB-dostavljam poruku: " + m);
        delivered.add(m);
    }

    public synchronized void handleMsg(Msg m, int src, String tag) {
        String content = m.getMessage().replace("#", "").trim();
        String msgId = content;

        if (received.add(msgId)) {
            for (int j = 0; j < N; j++) {
                if (j != myId && j != src) {
                    System.out.println("Saljem poruku od procesa " + src + " procesu " + j +": " + content);
                    comm.sendMsg(j, "MSG", content);
                    try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                }
            }
            URB_Deliver(content);
        }
    }

    public List<String> getDelivered() {
        return delivered;
    }
}