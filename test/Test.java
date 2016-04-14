import de.skyrising.xmpp.*;
import de.skyrising.xmpp.core.Connection;
import de.skyrising.xmpp.core.Feature;
import de.skyrising.xmpp.core.MessageStanza;
import de.skyrising.xmpp.im.Roster;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;

public class Test {
    public static void main(String[] args) throws IOException, ParseException {
        //new MainWindow();
        Account account = new Account(new Jid("test@pvpctutorials.de/res"));
        account.setPassword("ac+wXZ/UC6YkVqLQ5JXzA22SeWU=");

        Connection connection = new Connection("pvpctutorials.de");
        connection.account = account;
        connection.onChange(c -> System.out.println(c.change));
        connection.onFeatures(feature -> {
            if(!connection.isEncrypted()) return feature.getType() == Feature.Type.STARTTLS;
            if(connection.isEncrypted()) return
                    feature.getType() == Feature.Type.SASL_MECHANISMS ||
                    feature.getType() == Feature.Type.BIND;
            return false;
        });
        connection.startStream();
        MessageStanza message = new MessageStanza(
                new Jid("test@pvpctutorials.de"),
                new Jid("simon@pvpctutorials.de"),
                MessageStanza.Type.CHAT
        );
        message.setText("Test " + Math.random());
        connection.onChange(c -> {
            System.out.println(c.change);
            if(c.change == XMPP.StateChange.RESOURCE_BOUND)
                try {
                    //connection.send(message);
                    connection.queryRoster(roster -> {
                        System.out.println(roster);
                        Roster.Item item = new Roster.Item();
                        try {
                            item.jid = new Jid("simon@pvpctutorials.de");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        item.subscription = Roster.Item.Subscription.REMOVE;
                        try {
                            roster.updateOnline(Collections.singletonList(item), System.out::println);
                            item.subscription = Roster.Item.Subscription.BOTH;
                            item.name = "simon-test";
                            roster.updateOnline(Collections.singletonList(item), System.out::println);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    connection.serverXEPs.stream().forEachOrdered(xep -> System.out.printf("XEP-%04d %s\n", xep, XMPP.XEP_NAMES.get(xep)));
                    //connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        });

    }
}