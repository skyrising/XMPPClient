package de.skyrising.xmpp;

import de.skyrising.xmpp.xml.XMLNode;

import java.io.IOException;

public interface StanzaHandler {
    boolean handle(XMLNode stanza) throws IOException;
    void beforeSend(XMLNode stanza);
}
