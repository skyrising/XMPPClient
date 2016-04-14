package de.skyrising.xmpp.core;

import de.skyrising.xmpp.XMPP;
import de.skyrising.xmpp.xml.XMLNode;

import java.util.ArrayList;
import java.util.List;

public class Feature {

    private Type type;
    public boolean required;
    public boolean optional;
    public List<?> children;

    private Feature(Type type, XMLNode node) {
        this.type = type;
        node.getChildren().forEach(child -> {
            if(child.type.equals("required"))
                required = true;
            else if(child.type.equals("optional"))
                optional = true;
        });
        switch (type) {
            case STARTTLS:
                break;
            case REGISTER:
                break;
            case SASL_MECHANISMS:
                parseSaslMechanisms(node);
                break;
        }
    }

    private void parseSaslMechanisms(XMLNode node) {
        ArrayList<SaslAuthentication.Mechanism> mechanisms = new ArrayList<>();
        node.getChildren().stream().filter(child -> child.type.equals("mechanism")).map(
                child -> SaslAuthentication.Mechanism.lookup(child.content)
        ).forEach(mechanisms::add);
        children = mechanisms;
    }

    public Type getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Feature/");
        sb.append(type);
        if(type.xep == 0) sb.append("(Core)");
        else if(type.xep == 1) sb.append("(IM)");
        else if(type.xep > 1) sb.append(String.format("(XEP-%04d %s)", type.xep, XMPP.XEP_NAMES.get(type.xep)));
        if(children != null)
            sb.append(children);
        ArrayList<String> props = new ArrayList<>();
        if(required) props.add("required");
        else if(optional) props.add("optional");
        sb.append(props);
        return sb.toString();
    }

    public static Feature parse(XMLNode node) {
        Type type = Type.UNKNOWN;
        if(node.isType(XMPP.Namespace.Feature.STARTTLS, "starttls"))
            type = Type.STARTTLS;
        else if(node.isType(XMPP.Namespace.Feature.REGISTER, "register"))
            type = Type.REGISTER;
        else if(node.isType(XMPP.Namespace.Feature.SASL, "mechanisms"))
            type = Type.SASL_MECHANISMS;
        else if(node.isType(XMPP.Namespace.Feature.SM2, "sm"))
            type = Type.SM2;
        else if(node.isType(XMPP.Namespace.Feature.SM3, "sm"))
            type = Type.SM3;
        else if(node.isType(XMPP.Namespace.Feature.BIND, "bind"))
            type = Type.BIND;
        else if(node.isType(XMPP.Namespace.Feature.SESSION, "session"))
            type = Type.SESSION;
        else if(node.isType(XMPP.Namespace.Feature.ROSTERVER, "ver"))
            type = Type.ROSTERVER;
        else if(node.isType(XMPP.Namespace.Feature.COMPRESSION, "compression"))
            type = Type.COMPRESSION;
        else if(node.isType(XMPP.Namespace.Feature.CSI, "csi"))
            type = Type.CSI;
        return new Feature(type, node);
    }

    public enum Type {
        STARTTLS(0), SASL_MECHANISMS(0), BIND(0), SESSION(1), SM2(-1), REGISTER(77), COMPRESSION(138), SM3(198), ROSTERVER(237), CSI(352), UNKNOWN(-1);

        public final int xep;
        Type(int xep) {
            this.xep = xep;
        }
    }
}
