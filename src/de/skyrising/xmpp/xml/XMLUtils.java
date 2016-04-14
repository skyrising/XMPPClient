package de.skyrising.xmpp.xml;

import java.util.HashMap;
import java.util.Map;

public class XMLUtils {
    public static String encodeEntities(String text) {
        if(text == null) return "";
        StringBuilder sb = new StringBuilder();
        text.codePoints().boxed().map(XMLUtils::encodeEntity).forEachOrdered(sb::append);
        return sb.toString();
    }

    public static String encodeEntity(int entity) {
        if(entities.containsKey(entity)) return entities.get(entity);
        if(entity < 128)
            return Character.toString((char)entity);
        if(entity < 10000)
            return "&#" + entity + ";";
        else
            return "&#x" + Integer.toHexString(entity) + ";";
    }

    private static Map<Integer, String> entities = new HashMap<>();

    static {
        entities.put((int)'<', "&lt;");
        entities.put((int)'>', "&gt;");
        entities.put((int)'"', "&quot;");
        entities.put((int)'\'', "&apos;");
        entities.put((int)'&', "&amp;");
    }
}
