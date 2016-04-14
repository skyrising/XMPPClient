package de.skyrising.xmpp.core;

import de.skyrising.xmpp.Jid;
import de.skyrising.xmpp.xml.XMLNode;

public class MessageStanza extends XMLNode {
    public final Type type;

    public XMLNode body;

    private MessageStanza(String type) {
        super(type);
        this.type = null;
    }

    public MessageStanza(Jid from, Jid to, Type type) {
        super("message");
        this.type = type;
        this.setAttribute("type", type.toString().toLowerCase());
        this.setAttribute("from", from);
        this.setAttribute("to", to);
        this.addChild(body = new XMLNode("body"));
    }

    public void setText(String message) {
        body.content = message;
    }

    public enum Type {
        CHAT
    }
}
