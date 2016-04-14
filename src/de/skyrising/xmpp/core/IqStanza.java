package de.skyrising.xmpp.core;

import de.skyrising.xmpp.xml.XMLNode;

public class IqStanza extends Stanza {
    public final Type type;

    private IqStanza(String type) {
        super(type);
        this.type = null;
    }

    public IqStanza(Type type) {
        super("iq");
        this.type = type;
        this.setAttribute("type", type.toString().toLowerCase());
    }

    public IqStanza(XMLNode node) {
        super("iq");
        setAttributes(node.getAttributes());
        addChildren(node.getChildren());
        try {
            type = Type.valueOf(getAttribute("type").toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new StanzaError(this, StanzaError.Condition.BAD_REQUEST, StanzaError.ErrorType.MODIFY)
                    .setText("Invalid IQ type: " + getAttribute("type"));
        }
    }

    public enum Type {
        GET, SET, RESULT, ERROR
    }
}
