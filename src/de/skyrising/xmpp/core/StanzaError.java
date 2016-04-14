package de.skyrising.xmpp.core;

import de.skyrising.xmpp.XMPP;
import de.skyrising.xmpp.xml.XMLNode;

public class StanzaError extends RuntimeException {
    private Stanza stanza;
    private Condition condition;
    private ErrorType errorType;
    private String text;

    public StanzaError(Stanza stanza, Condition condition, ErrorType type) {
        this.stanza = stanza;
        this.condition = condition;
        this.errorType = type;
    }

    public StanzaError setText(String text) {
        this.text = text;
        return this;
    }

    public XMLNode toXML() {
        XMLNode xml = new XMLNode(stanza.type).setAttribute("type", "error").setAttribute("id", stanza.getAttribute("id"));
        XMLNode errorXML = new XMLNode("error").setAttribute("type", errorType);
        XMLNode conditionXML = new XMLNode(condition.toString(), XMPP.Namespace.STANZAS);
        errorXML.addChild(conditionXML);
        if(text != null)
            errorXML.addChild(new XMLNode("text", XMPP.Namespace.STANZAS).setContent(text));
        xml.addChild(errorXML);
        return xml;
    }

    @SuppressWarnings("unused")
    public enum Condition {
        BAD_REQUEST, CONFLICT, FEATURE_NOT_IMPLEMENTED, FORBIDDEN, GONE, INTERNAL_SERVER_ERROR,
        ITEM_NOT_FOUND, JID_MALFORMED, NOT_ACCEPTABLE, NOT_ALLOWED, NOT_AUTHORIZED, POLICY_VIOLATION,
        RECIPIENT_UNAVAILABLE, REDIRECT, REGISTRATION_REQUIRED, REMOTE_SERVER_NOT_FOUND,
        REMOTE_SERVER_TIMEOUT, RESOURCE_CONSTRAINT, SERVICE_UNAVAILABLE, SUBSCRIPTION_REQUIRED,
        UNDEFINED_CONDITION, UNEXPECTED_REQUEST;

        public static Condition fromName(String name) {
            return Condition.valueOf(name.replace('-', '_').toUpperCase());
        }

        @Override
        public String toString() {
            return name().replace('_', '-').toLowerCase();
        }
    }

    @SuppressWarnings("unused")
    public enum ErrorType {
        AUTH, CANCEL, CONTINUE, MODIFY, WAIT;

        public static ErrorType fromName(String name) {
            return valueOf(name.toUpperCase());
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
