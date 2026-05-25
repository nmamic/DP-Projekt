package COComponents;

import components.Linker;
import components.Msg;

import java.util.*;

public class COProces extends components.Process {

    // causal_past_i[0..N-1]
    private final int[] causalPast;

    // msg_set_i: primljene poruke koje još ne smijemo CO-deliverati
    private final List<COMessage> msgSet = new ArrayList<>();

    // poruke koje smo već vidjeli: koristi se za URB rebroadcast
    private final Set<String> vecDobivenePoruke = new HashSet<>();

    // poruke koje smo već CO-deliverali - zastita da se ista poruka ne delivera dvaput
    private final Set<String> vecDeliveranePoruke = new HashSet<>();

    // redni broj poruka koje ovaj proces šalje
    private int seq = 0;

    public COProces(Linker linker) {
        super(linker);
        this.causalPast = new int[N];
    }

    /*
     * CO broadcast(m)
     */
    public synchronized void CO_Broadcast(String text) {
        // Poruka nosi kopiju trenutnog znanja procesa o kauzalnoj prošlosti.
        // kopija - Zato što se causalPast kasnije može mijenjati. Poruka mora zapamtiti stanje u trenutku slanja.
        int[] snapshot = Arrays.copyOf(causalPast, N);

        COMessage message = new COMessage(
                myId, //myId je ID procesa koji šalje poruku.
                ++seq,
                snapshot,
                text.replace(" ", "_")
        );

        String encoded = encode(message);

        /*
         * Isto kao kod kolege:
         * lokalno pozovemo handleMsg da proces obradi i vlastitu poruku.
         */
        //To znači da proces najprije sam sebi “dostavi” poruku, pa će je handleMsg obraditi kao da ju je dobio.
        Msg localMsg = new Msg(myId, myId, "CO", encoded);
        handleMsg(localMsg, myId, "CO");
    }

    /*
     * Ovdje se događa i URB dio i CO dio.
     */
    @Override
    public synchronized void handleMsg(Msg m, int src, String tag) {
        //Ako poruka nije tipa "CO", ovaj proces je ignorira.
        //To je samo sigurnosna provjera da ova klasa obrađuje samo CO poruke.
        if (!tag.equals("CO")) {
            return;
        }

        //Poruka se šalje kao string, na primjer:
        //1;2;0,1,0;hello
        //to znaci: sender = 1
        //seq = 2
        //causalPast = [0, 1, 0]
        //text = hello
        String content = m.getMessage().replace("#", "").trim();
        COMessage message = decode(content); //Metoda decode taj string pretvara natrag u objekt COMessage.
        String msgId = message.id();

        /*
         * URB dio:
         * ako poruku vidimo prvi put, proslijedimo je dalje.
         */
        //Ako ovu poruku vidim prvi put, obradi je.
        //Ako sam je već vidio, ignoriraj je.
        if (vecDobivenePoruke.add(msgId)) {

            //Proces prosljeđuje poruku svim ostalim procesima osim:
            for (int j = 0; j < N; j++) {
                if (j != myId && j != src) { //ne šalje sam sebi  i ne vraća poruku onome od koga ju je upravo dobio.
                    sendMsg(j, "CO", content);
                }
            }

            /*
             * CO dio:
             * ne deliveramo nužno odmah, nego tek ako je causal condition zadovoljen.
             */
            //Smijem li ovu poruku odmah CO-deliverati ili je moram spremiti u msgSet?
            tryCOMessage(message);
        }
    }

    private void tryCOMessage(COMessage message) {
        if (vecDeliveranePoruke.contains(message.id())) {
            return;
        }

        if (canDeliver(message)) { //Ako je uvjet deliveranja zadovoljen:
            CO_Deliver(message); //Ako je uvjet deliveranja zadovoljen:
            checkMsgSet(); //i zatim se provjeri jesu li neke stare poruke iz msgSet sada postale spremne:
        } else {
            msgSet.add(message); //poruka se sprema za kasnije.
        }
    }

    /*
     * DC_i(m):
     * forall k: causal_past_i[k] >= m.causal_past[k]
     */
    //to odgovara: DC_i(m) odnosno delivery condition.
    //Smijem deliverati poruku samo ako sam već deliverao sve poruke koje je
    // pošiljatelj te poruke znao u trenutku slanja.
    private boolean canDeliver(COMessage message) {
        for (int k = 0; k < N; k++) {
            if (causalPast[k] < message.causalPast[k]) {
                return false;
            }
        }

        return true;
    }

    private void CO_Deliver(COMessage message) {
        if (vecDeliveranePoruke.contains(message.id())) {
            return;
        }

        //Zatim se označi kao deliverana
        vecDeliveranePoruke.add(message.id());

        //To je zapravo tvoj “CO deliver”.
        System.out.println(
                "Proces " + myId +
                        " CO-deliverao: " + message.text.replace("_", " ") +
                        " od P" + message.sender +
                        " seq=" + message.seq +
                        " causalPast=" + Arrays.toString(message.causalPast)
        );

        /*
         * Nakon deliveranja poruke od procesa j,
         * povećavamo znanje o tom procesu.
         */
        //Ako sam deliverala poruku od procesa j, onda sada znam da sam vidjela još jednu poruku od j.
        int j = message.sender;
        causalPast[j] = message.causalPast[j] + 1;
    }

    /*
     * Nakon što smo nešto deliverali, možda su neke poruke iz msgSet sada spremne.
     */
    //Nakon što deliveramo jednu poruku, možda su neke druge poruke koje
    // su čekale u msgSet sada postale moguće za deliveranje.
    private void checkMsgSet() {
        boolean deliveredSomething;

        do {
            deliveredSomething = false;

            Iterator<COMessage> iterator = msgSet.iterator();

            while (iterator.hasNext()) {
                COMessage message = iterator.next();

                if (canDeliver(message)) {
                    iterator.remove();
                    CO_Deliver(message);
                    deliveredSomething = true;
                }
            }

        } while (deliveredSomething);
    }

    /*
     * Format:
     * sender;seq;vector;text
     *
     * Primjer:
     * 1;2;0,1,0;hello
     */
    //Ovo pretvara poruku u string da se može poslati preko tvoje mrežne/simulacijske infrastrukture.
    private String encode(COMessage message) {
        StringBuilder vector = new StringBuilder();

        for (int i = 0; i < message.causalPast.length; i++) {
            if (i > 0) {
                vector.append(",");
            }
            vector.append(message.causalPast[i]);
        }

        return message.sender + ";" +
                message.seq + ";" +
                vector + ";" +
                message.text;
    }

    //Ovo radi suprotno.
    private COMessage decode(String content) {
        String[] parts = content.split(";", 4);

        int sender = Integer.parseInt(parts[0]);
        int seq = Integer.parseInt(parts[1]);

        String[] vectorParts = parts[2].split(",");
        int[] vector = new int[vectorParts.length];

        for (int i = 0; i < vectorParts.length; i++) {
            vector[i] = Integer.parseInt(vectorParts[i]);
        }

        String text = parts[3];

        return new COMessage(sender, seq, vector, text);
    }
    //Ovo je unutarnja pomoćna klasa koja predstavlja CO poruku.
    //Svaka poruka nosi:
    //sender       tko ju je poslao
    //seq          koji joj je redni broj kod tog pošiljatelja
    //causalPast   što je pošiljatelj već znao/deliverao prije slanja
    //text         sadržaj poruke
    private static class COMessage {
        int sender;
        int seq;
        int[] causalPast;
        String text;

        COMessage(int sender, int seq, int[] causalPast, String text) {
            this.sender = sender;
            this.seq = seq;
            this.causalPast = causalPast;
            this.text = text;
        }

        String id() {
            return sender + ":" + seq;
        }
    }
}