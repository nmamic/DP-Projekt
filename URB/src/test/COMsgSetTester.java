package test;

import COComponents.COProces;
import components.Linker;
import components.ListenerThread;

public class COMsgSetTester {

    private static void log(int myId, String message) {
        System.out.println("[P" + myId + "] " + message);
    }

    private static void separator(int myId) {
        System.out.println();
        System.out.println("====================================");
        System.out.println(" Proces P" + myId);
        System.out.println("====================================");
    }

    public static void main(String[] args) throws Exception {
        String basename = args[0];
        int myId = Integer.parseInt(args[1]);
        int numProc = Integer.parseInt(args[2]);

        separator(myId);

        Linker linker = new Linker(basename, myId, numProc);
        COProces proces = new COProces(linker);

        if (myId == 0) {
            proces.enableTestDelay();
            //log(myId, "Uključen test delay za m0 od P2.");
        }

        log(myId, "Pokrećem listenere...");

        for (int i = 0; i < numProc; i++) {
            if (i != myId) {
                new ListenerThread(i, proces).start();
            }
        }

        Thread.sleep(2000);

        if (myId == 2) {
            Thread.sleep(1000);

            log(myId, "Šaljem m0.");
            proces.CO_Broadcast("m0");

            log(myId, "Završio slanje m0.");
        }

        if (myId == 1) {
            log(myId, "Čekam da CO-deliveram m0.");
            proces.waitUntilDelivered("m0");

            log(myId, "Deliverao sam m0, sada šaljem m1_nakon_m0.");
            proces.CO_Broadcast("m1_nakon_m0");

        }


        if (myId == 0) {
            log(myId, "P0 je proces na kojem očekujemo msgSet.");
            log(myId, "Trebao bi kasniti s m0, dobiti m1_nakon_m0 prerano i staviti ga u msgSet.");
        }

        Thread.sleep(25000);

        log(myId, "Gotov.");
    }
}