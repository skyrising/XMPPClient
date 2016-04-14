package de.skyrising.xmpp.xml;

import java.util.Hashtable;
import java.util.Map;

public class XMLTag {
    private Hashtable<String, String> attributes;
    private String type;

    private boolean start;
    private boolean empty;

    private XMLNode node;

    private XMLTag(String type, Hashtable<String, String> attributes) {
        this.type = type;
        this.attributes = attributes;
    }

    public XMLTag setStart() {
        this.start = true;
        this.empty = false;
        return this;
    }

    public boolean isStart() {
        return this.start;
    }

    public XMLTag setEmpty() {
        this.start = true;
        this.empty = true;
        return this;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public XMLTag setEnd() {
        this.start = false;
        this.empty = false;
        return this;
    }

    public boolean isEnd() {
        return !this.start;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        if(isEnd()) sb.append('/');
        sb.append(type);
        if(isStart() && attributes != null) {
            for(Map.Entry<String, String> attribute : attributes.entrySet()) {
                sb.append(' ');
                sb.append(attribute.getKey());
                sb.append("='");
                sb.append(XMLUtils.encodeEntities(attribute.getValue()));
                sb.append("'");
            }
        }
        if(isEmpty()) sb.append(" /");
        sb.append('>');
        return sb.toString();
    }

    public static XMLTag start(String type) {
        return new XMLTag(type, null).setStart();
    }

    public static XMLTag start(String type, Hashtable<String, String> attributes) {
        return new XMLTag(type, attributes).setStart();
    }

    public static XMLTag start(XMLNode node) {
        return start(node.type, node.getAttributes()).setNode(node);
    }

    public static XMLTag empty(String type) {
        return new XMLTag(type, null).setEmpty();
    }

    public static XMLTag empty(String type, Hashtable<String, String> attributes) {
        return new XMLTag(type, attributes).setEmpty();
    }

    public static XMLTag empty(XMLNode node) {
        return empty(node.type, node.getAttributes()).setNode(node);
    }

    public static XMLTag end(String type) {
        return new XMLTag(type, null).setEnd();
    }

    public static XMLTag end(XMLNode node) {
        return end(node.type).setNode(node);
    }

    public String getType() {
        return type;
    }

    public XMLNode getNode() {
        return node;
    }

    private XMLTag setNode(XMLNode node) {
        this.node = node;
        return this;
    }
}
