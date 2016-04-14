package de.skyrising.xmpp.im;

import de.skyrising.xmpp.Jid;
import de.skyrising.xmpp.XMPP;
import de.skyrising.xmpp.core.Connection;
import de.skyrising.xmpp.core.IqStanza;
import de.skyrising.xmpp.xml.XMLNode;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;

public class Roster {
    public Set<Item> all = new HashSet<>();
    public String version = null;

    private Connection connection;

    public Roster(Connection connection) {
        this.connection = connection;
    }

    public List<Item> getGroup(String group) {
        Item[] contacts = (Item[]) all.stream().filter(c ->
                c.groups.contains(group) || group == null
        ).toArray();
        return Arrays.asList(contacts);
    }

    public void update(List<Item> items, String version) {
        this.version = version;
        all.addAll(items);
        items.stream().filter(item -> item.subscription == Item.Subscription.REMOVE).forEach(all::remove);
    }

    public void updateOnline(List<Item> items, Consumer<IqStanza> callback) throws IOException {
        IqStanza iq = new IqStanza(IqStanza.Type.SET);
        iq.setAttribute("from", connection.jid);
        XMLNode query = new XMLNode("query", XMPP.Namespace.IQ.ROSTER);
        items.stream().map(Item::toXML).forEach(query::addChild);
        iq.addChild(query);
        connection.query(iq, callback);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Roster{\n");
        all.forEach(item -> sb.append('\t').append(item).append('\n'));
        return sb.append('}').toString();
    }

    public static class Item {
        public Jid jid;
        public String name;
        public List<String> groups = new ArrayList<>();
        public Subscription subscription;

        public Item() {}

        public Item(XMLNode node) {
            try {
                jid = node.getAttributeAsJid("jid");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            name = node.getAttribute("name");
            subscription = Subscription.valueOf(node.getAttribute("subscription", "none").toUpperCase());
            node.getChildren().stream().filter(child -> child.type.equals("group")).forEach(child -> groups.add(child.content));
        }

        public XMLNode toXML() {
            XMLNode item = new XMLNode("item");
            item.setAttribute("jid", jid);
            item.setAttribute("name", name);
            if(subscription != null) item.setAttribute("subscription", subscription.name().toLowerCase());
            for (String group : groups)
                item.addChild(new XMLNode("group").setContent(group));
            return item;
        }

        @Override
        public String toString() {
            return "RosterItem[" + name + "<" + jid + ">, sub=" + subscription.name().toLowerCase() + "]" + groups;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && o instanceof Roster.Item && ((Roster.Item)o).jid.equals(this.jid);
        }

        @SuppressWarnings("unused")
        public enum Subscription {
            NONE, TO, FROM, BOTH, REMOVE
        }
    }
}
