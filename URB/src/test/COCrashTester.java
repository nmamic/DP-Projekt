package test;

import COComponents.COProces;
import components.Linker;
import components.ListenerThread;

public class COCrashTester {

    public static void main(String[] args) throws Exception {
        String baseName = args[0];
        int myId = Integer.parseInt(args[1]);
        int numProc = Integer.parseInt(args[2]);

        Linker linker = new Linker(baseName, myId, numProc);
        COProces co = new COProces(linker);

        // Pokreni listener dretve za sve ostale procese
        for (int i = 0; i < numProc; i++) {
            if (i != myId) {
                new ListenerThread(i, co).start();
            }
        }

        Thread.sleep(500);

        for (int i = 1; i <= 3; i++) {
            System.out.println("[" + myId + "] Slanje CO poruke broj " + i);

            co.CO_Broadcast("Poruka_" + i + "_od_procesa_" + myId);

            if (myId == 1 && i == 2) {
                System.out.println("[" + myId + "] ==============================");
                System.out.println("[" + myId + "] SIMULACIJA CRASH-a!!!");
                System.out.println("[" + myId + "] Proces 1 je pao nakon 2. CO poruke!");
                System.out.println("[" + myId + "] ==============================");
                System.out.flush();

                Runtime.getRuntime().halt(0);
            }

            Thread.sleep(100);
        }

        System.out.println("[" + myId + "] Sve CO poruke poslane, čekam dostavu.");
        Thread.sleep(3000);

        System.out.println("[" + myId + "] Gotovo.");
    }
}