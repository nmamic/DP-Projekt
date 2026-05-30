package test;

import COComponents.COProces;
import components.Linker;
import components.ListenerThread;

/*
P0 šalje m0
P1 nakon toga šalje m1_nakon_m0
P2 samo prima
 */

public class COTester {

    public static void main(String[] args) throws Exception {
        String basename = args[0];
        int myId = Integer.parseInt(args[1]); //id procesa
        int numProc = Integer.parseInt(args[2]); //ukupno procesa

        Linker linker = new Linker(basename, myId, numProc);
        COProces proces = new COProces(linker);

        for (int i = 0; i < numProc; i++) {
            if (i != myId) {
                new ListenerThread(i, proces).start();
            }
        }

        Thread.sleep(2000);


        if (myId == 0) {
            Thread.sleep(1000);
            proces.CO_Broadcast("m0");
        }

        if (myId == 1) {
            Thread.sleep(7000);
            proces.CO_Broadcast("m1_nakon_m0");
        }


        if (myId == 2) {
            // P2 samo prima i delivera
        }

        Thread.sleep(20000);
        System.out.println("P" + myId + " gotov.");
    }
}