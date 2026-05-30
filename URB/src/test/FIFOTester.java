package test;

import FIFOComponents.FIFOProces;
import components.Linker;
import components.ListenerThread;

public class FIFOTester {
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

        // pošalji više poruka zaredom od istog procesa
        for (int i = 1; i <= 3; i++) {
            System.out.println("[" + myId + "] Slanje poruke broj " + i);
            fifo.FIFO_Broadcast("Poruka_" + i + "_od_procesa_" + myId);

            // mali delay da poruke stignu redom kroz mrežu
            Thread.sleep(100);
        }

        System.out.println("[" + myId + "] Sve poruke poslane, čekam na drive.");
        Thread.sleep(2000);
    }
}
