package test;

import URBComponents.URBProces;
import components.Linker;
import components.ListenerThread;

public class URBTester {
    public static void main(String[] args) throws Exception {
        String baseName = args[0];
        int myId = Integer.parseInt(args[1]);
        int numProc = Integer.parseInt(args[2]);

        Linker linker = new Linker(baseName, myId, numProc);
        URBProces urb = new URBProces(linker);

        for (int i = 0; i < numProc; i++) {
            if (i != myId) {
                new ListenerThread(i, urb).start();
            }
        }

        Thread.sleep(500);

        System.out.println("Broadcastam poruku");
        urb.URB_Broadcast("Poruka od procesa " + myId);

        Thread.sleep(2000 * (numProc + 3));

        System.out.println("P" + myId + " gotov.");
        System.out.println("Dostavljene poruke: ");
        for (String msg : urb.getDelivered()) {
            System.out.println("  " + msg);
        }
    }
}