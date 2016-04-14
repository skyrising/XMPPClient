package de.skyrising.xmpp.core;

import de.skyrising.xmpp.Account;
import de.skyrising.xmpp.XMPP;
import de.skyrising.xmpp.xml.XMLNode;

import java.io.IOException;
import java.util.*;

public class SaslAuthentication {
    public Mechanism mechanism;
    private Connection connection;

    public SaslAuthentication(Mechanism mechanism, Connection connection) {
        this.mechanism = mechanism;
        this.connection = connection;
    }

    public void sendInitialAuth() throws IOException {
        XMLNode node = new XMLNode("auth", XMPP.Namespace.Feature.SASL);
        Base64.Encoder base64 = Base64.getEncoder();
        node.setAttribute("mechanism", mechanism.getName());
        Account account = connection.account;
        switch (mechanism) {
            case PLAIN:
                node.content = base64.encodeToString(("\0" + account.getUsername() + "\0" + account.getPassword()).getBytes());
                break;
            case SCRAM_SHA_1: case SCRAM_SHA_1_PLUS:
                //TODO: implement SCRAM-SHA-1 & SCRAM-SHA-1-PLUS
                throw new IllegalArgumentException(mechanism + " not yet implemented");
        }
        connection.send(node);
    }

    public void onChallange(XMLNode challange) {

    }

    public static String saslPrep(String s) {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(Character.isSpaceChar(c) && c > 0x7f) sb.append(' ');
            else if(c < 0x20) continue;
            else if((c&0xff00) == 0xfe00 && ((c&0xff) < 0x10 || c == 0xfeff)) continue;
            else if(((c&0xff00) == 0x1800 || (c&0xff00) == 0x2000) && ((c&0xff) >= 0xb && (c&0xff) <= 0xd)) continue;

            else
                sb.append(c);
        }
        return sb.toString();
    }

    public static final List<Mechanism> priorities = Collections.unmodifiableList(Arrays.asList(
            //Mechanism.SCRAM_SHA_1_PLUS,
            //Mechanism.SCRAM_SHA_1,
            Mechanism.PLAIN
    ));

    public static List<Mechanism> choose(List<Mechanism> other) {
        ArrayList<Mechanism> chosen = new ArrayList<>();
        priorities.stream().filter(other::contains).forEach(chosen::add);
        return chosen;
    }

    enum Mechanism {
        PLAIN("PLAIN"), SCRAM_SHA_1("SCRAM-SHA-1"), SCRAM_SHA_1_PLUS("SCRAM-SHA-1-PLUS"), UNKNOWN("");

        public String name;
        Mechanism(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Mechanism lookup(String name) {
            for(Mechanism m : values())
                if(m.name.equals(name)) return m;
            return UNKNOWN;
        }
    }
}
