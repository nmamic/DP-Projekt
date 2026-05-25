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

        /*
        Linker zna:
            koji sam ja proces
            koliko ima procesa
            kako se povezati s ostalima
            na koje portove slati poruke

            COProces ne šalje poruke direktno preko socketa ručno, nego koristi Linker.
         */
        Linker linker = new Linker(basename, myId, numProc);
        // COProces ne šalje poruke direktno preko socketa ručno, nego koristi Linker.
        COProces proces = new COProces(linker);

        /*
         * Pokreni listener za svaki drugi proces.
         * ListenerThread stalno prima poruke i zove handleMsg.
         * Svaki proces mora moći primati poruke od drugih procesa. Zato pokreće listener threadove.
         */
        for (int i = 0; i < numProc; i++) {
            if (i != myId) {
                new ListenerThread(i, proces).start();
            }
        }

        //Da svi procesi stignu pokrenuti svoje listener threadove prije nego što netko počne slati poruke.
        Thread.sleep(2000);

        /*
         * Test scenarij:
         * P0 šalje m0.
         * P1 nakon malo čekanja šalje m1.
         *
         * Ako je P1 prije slanja m1 već deliverao m0,
         * tada m1 u svom causalPast vektoru nosi informaciju da m0 mora doći prije m1.
         */
        if (myId == 0) {
            Thread.sleep(1000);
            proces.CO_Broadcast("m0");
        }

        if (myId == 1) {
            Thread.sleep(3000);
            proces.CO_Broadcast("m1_nakon_m0");
        }

        if (myId == 2) {
            // P2 samo prima i delivera
        }
    }
}