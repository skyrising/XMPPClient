package de.skyrising.xmpp.xml;

import de.skyrising.xmpp.Jid;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Stream;

public class XMLNode {
    public String type;
    protected Hashtable<String, String> attributes = new Hashtable<>();
    protected List<XMLNode> children = new ArrayList<>();

    protected Map<String, String> nsAliases = new HashMap<>();
    protected Map<String, String> namespaces = new HashMap<>();

    public String content;

    public XMLNode(String type) {
        this.type = type.toLowerCase();
    }

    public XMLNode(String type, String xmlns) {
        this.type = type.toLowerCase();
        this.setAttribute("xmlns", xmlns);
    }

    public void addAttributes(Hashtable<String, String> attributes) {
        for(Map.Entry<String, String> attr : attributes.entrySet())
            setAttribute(attr.getKey(), attr.getValue());
    }

    public XMLNode addChild(XMLNode child) {
        this.content = null;
        this.children.add(child);
        nsAliases.forEach(child.nsAliases::putIfAbsent);
        namespaces.forEach(child.namespaces::putIfAbsent);
        return child;
    }

    public XMLNode addChild(String type) {
        return addChild(new XMLNode(type));
    }

    public XMLNode addChild(String type, String xmlns) {
        return addChild(new XMLNode(type, xmlns));
    }

    public XMLNode addChildren(XMLNode... children) {
        for(XMLNode child : children)
            this.addChild(child);
        return this;
    }

    public XMLNode addChildren(List<XMLNode> children) {
        for(XMLNode child : children)
            this.addChild(child);
        return this;
    }

    public String getAttribute(String attr) {
        if(this.attributes.containsKey(attr))
            return this.attributes.get(attr);
        return null;
    }

    public Long getAttributeAsLong(String attr) {
        String val = getAttribute(attr);
        return val == null ? null : Long.parseLong(val);
    }

    public Boolean getAttributeAsBoolean(String attr) {
        String val = getAttribute(attr);
        return val == null ? null : val.isEmpty() || Boolean.parseBoolean(attr) || attr.equals("1");
    }

    public Jid getAttributeAsJid(String attr) throws ParseException {
        String jid = getAttribute(attr);
        return jid == null ? null : new Jid(jid);
    }

    public String getAttribute(String attr, String defaultValue) {
        String value = getAttribute(attr);
        return value == null ? defaultValue : value;
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    public List<XMLNode> getChildren() {
        return children;
    }

    public String getNamespace() {
        return getAttribute("xmlns", "");
    }

    public String getNSAlias(String namespace) {
        return nsAliases.get(namespace);
    }

    private void registerNSAlias(String namespace, String alias) {
        nsAliases.put(namespace, alias);
        namespaces.put(alias, namespace);
    }

    public XMLNode setAttribute(String attr, Object o) {
        return this.setAttribute(attr, o.toString());
    }

    public XMLNode setAttribute(String attr, long l) {
        return this.setAttribute(attr, Long.toString(l));
    }

    public XMLNode setAttribute(String attr, int i) {
        return this.setAttribute(attr, Integer.toString(i));
    }

    public XMLNode setAttribute(String attr, boolean b) {
        return this.setAttribute(attr, Boolean.toString(b));
    }

    public XMLNode setAttribute(String attr, String value) {
        if(attr == null) return this;
        if(value != null)
            attributes.put(attr, value);
        else
            attributes.remove(attr);
        if(attr.equals("xmlns"))
            registerNSAlias(value, "");
        else if(attr.startsWith("xmlns:"))
            registerNSAlias(value, attr.substring(6));

        return this;
    }

    public void setAttributes(Hashtable<String, String> attributes) {
        this.attributes.clear();
        addAttributes(attributes);
    }

    public XMLNode setContent(String content) {
        this.content = content;
        return this;
    }

    public boolean isType(String namespace, String type) {
        String nsAlias = getNSAlias(namespace);
        return this.type.equals(nsAlias + ":" + type) ||
                (nsAlias == null || nsAlias.isEmpty()) && this.type.equals(type) && this.getNamespace().equals(namespace);
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        if(content == null && children.isEmpty()) {
            sb.append(XMLTag.empty(this));
        } else {
            sb.append(XMLTag.start(this));
            if(content != null)
                sb.append(XMLUtils.encodeEntities(content));
            else {
                sb.append('\n');
                for(XMLNode child : children)
                    for(String line : child.toPrettyString().split("\n"))
                        sb.append("    ").append(line).append('\n');
            }
            sb.append(XMLTag.end(type));
        }
        return sb.toString();
    }

    public boolean hasAttribute(String id) {
        return attributes.containsKey(id);
    }

    public Stream<XMLNode> childrenOfType(String type) {
        return this.getChildren().stream().filter(child -> child.type.equals(type));
    }

    public Stream<XMLNode> childrenOfType(String namespace, String type) {
        return this.getChildren().stream().filter(child -> child.isType(namespace, type));
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof XMLNode))
            return false;
        XMLNode node = (XMLNode)o;
        return this.type.equals(node.type) &&
                this.children.equals(node.children) &&
                this.attributes.equals(node.attributes) &&
                ((this.content == null && node.content == null) ||
                        (this.content != null && this.content.equals(node.content)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(content == null && children.isEmpty()) {
            sb.append(XMLTag.empty(this));
        } else {
            sb.append(XMLTag.start(this));
            if(content != null)
                sb.append(XMLUtils.encodeEntities(content));
            else {
                for(XMLNode child : children)
                    sb.append(child.toString());
            }
            sb.append(XMLTag.end(type));
        }
        return sb.toString();
    }
}
