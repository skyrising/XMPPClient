package de.skyrising.xmpp.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.*;
import java.util.Stack;

public abstract class XMLStream {

    private Stack<XMLNode> openNodes = new Stack<>();

    protected XMLStreamReader reader;

    protected void initParser() {
        try {
            openNodes.clear();
            reader = XMLInputFactory.newFactory().createXMLStreamReader(getIn());
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    public void writeXMLHeader() {
        String xmlHeader = "<?xml version='1.0'?>";
        getOut().println(xmlHeader);
        getOut().flush();
        System.out.println("Out: " + xmlHeader);
    }

    public void writeXML(XMLNode node) {
        getOut().println(node);
        getOut().flush();
        System.out.println("Out: " + node.toPrettyString());
    }

    public void writeXML(XMLTag tag) {
        getOut().println(tag);
        getOut().flush();
        System.out.println("Out: " + tag);
    }

    public void receive() throws XMLStreamException {
        if(!reader.hasNext()) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
            return;
        }
        try {
            reader.next();
        } catch (XMLStreamException e) {
            try {
                getIn().mark(2);
                if(getIn().read() < 0) return;
                getIn().reset();
            } catch (IOException ignored) {
                return;
            }
            throw e;
        }
        int event = reader.getEventType();
        switch(event) {
            case XMLStreamConstants.START_ELEMENT:
                XMLNode startedNode = new XMLNode(qNameToString(reader.getName()));
                int attrs = reader.getAttributeCount();
                for(int i = 0; i < attrs; i++)
                    startedNode.setAttribute(qNameToString(reader.getAttributeName(i)), reader.getAttributeValue(i));
                int namespaces = reader.getNamespaceCount();
                for(int i = 0; i < namespaces; i++) {
                    String prefix = reader.getNamespacePrefix(i);
                    startedNode.setAttribute("xmlns" + (prefix != null ? ":" + prefix : ""), reader.getNamespaceURI(i));
                }
                if(!openNodes.isEmpty())
                    openNodes.peek().addChild(startedNode);
                openNodes.push(startedNode);
                handleTag(XMLTag.start(startedNode));
                break;
            case XMLStreamConstants.CHARACTERS:
                XMLNode node = openNodes.peek();
                node.addChild(new XMLTextNode(reader.getText()));
                break;
            case XMLStreamConstants.END_ELEMENT:
                XMLNode endedNode = openNodes.pop();
                if(endedNode.getChildren().stream().allMatch(child -> child instanceof XMLTextNode)) {
                    endedNode.content = endedNode.getChildren().stream().map(child -> child.content)
                            .reduce((a, b) -> a + " " + b).orElse(null);
                    endedNode.getChildren().clear();
                }
                if(openNodes.size() > 1) break;
                boolean handled = handleTag(XMLTag.end(endedNode));
                if(!handled) handleNode(endedNode);
                break;
        }
    }

    private String qNameToString(QName name) {
        if(name.getPrefix() == null || name.getPrefix().isEmpty())
            return name.getLocalPart();
        return name.getPrefix() + ":" + name.getLocalPart();
    }

    public abstract boolean handleTag(XMLTag tag);
    public abstract boolean handleNode(XMLNode node);

    protected abstract PrintStream getOut();
    protected abstract InputStream getIn();
}
