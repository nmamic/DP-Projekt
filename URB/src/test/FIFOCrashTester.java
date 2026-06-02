package test;

import FIFOComponents.FIFOProces;
import components.Linker;
import components.ListenerThread;

public class FIFOCrashTester {
    public static void main(String[] args) throws Exception {
        String baseName = args[0];
        int myId = Integer.parseInt(args[1]);
        int numProc = Integer.parseInt(args[2]);

        Linker linker = new Linker(baseName, myId, numProc);
        FIFOProces fifo = new FIFOProces(linker);

        // pokreni listener dretve za sve ostale procese
        for (int i = 0; i < numProc; i++) {
            if (i != myId) {
                new ListenerThread(i, fifo).start();
            }
        }

        // Simulacija crash-a: proces 1 crasha nakon 2 poruke
        for (int i = 1; i <= 3; i++) {
            System.out.println("[" + myId + "] Slanje poruke broj " + i);
            fifo.FIFO_Broadcast("Poruka_" + i + "_od_procesa_" + myId);

            // CRASH SIMULACIJA: ako je proces 1, crashaj nakon 2. poruke
            if (myId == 1 && i == 2) {
                System.out.println("[" + myId + "] ==============================");
                System.out.println("[" + myId + "] SIMULACIJA CRASH-a!!!");
                System.out.println("[" + myId + "] Proces 1 je pao!");
                System.out.println("[" + myId + "] ==============================");
                System.out.flush();

                fifo.markCrashed();
                Runtime.getRuntime().halt(0);
            }

            Thread.sleep(100);
        }

        System.out.println("[" + myId + "] Sve poruke poslane.");
        Thread.sleep(2000);
    }
}
