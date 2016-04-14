package de.skyrising.xmpp.xml;

public class XMLTextNode extends XMLNode {

    public XMLTextNode(String content) {
        super("");
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
