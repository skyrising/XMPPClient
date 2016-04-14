package de.skyrising.xmpp.core;

import de.skyrising.xmpp.xml.XMLNode;

public abstract class Stanza extends XMLNode {
    public Stanza(String type){
        super(type);
    }
}
