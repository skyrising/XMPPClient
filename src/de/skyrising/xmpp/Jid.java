package de.skyrising.xmpp;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;

public class Jid {
    public String user;
    @NotNull
    public String host;
    public String resource;

    public Jid(String user, @NotNull String host, String resource) {
        if(host.isEmpty())
            throw new IllegalArgumentException("Host part is missing");
        this.user = user == null || user.isEmpty() ? null : user;
        this.host = host;
        this.resource = resource == null || resource.isEmpty() ? null : resource;
    }

    public Jid(@NotNull String jid) throws ParseException {
        String tmpJid = jid;
        if(jid.contains("/")) {
            int slashPos = jid.lastIndexOf('/');
            this.resource = jid.substring(slashPos + 1);
            jid = jid.substring(0, slashPos);
            if(jid.contains("/"))
                throw new ParseException("Multiple '/'s in Jid", tmpJid.indexOf('/'));
        }
        if(jid.contains("@")) {
            int atPos = jid.indexOf('@');
            this.user = jid.substring(0, atPos);
            jid = jid.substring(atPos + 1);
            if (jid.contains("@"))
                throw new ParseException("Multiple '@'s in Jid", tmpJid.indexOf('@', atPos + 1));
            if (this.user.isEmpty())
                throw new ParseException("User part of Jid is missing", 0);
            if (jid.isEmpty())
                throw new ParseException("Host part of Jid is missing", atPos + 1);
        }
        this.host = jid;
        if(this.host.isEmpty())
            throw new ParseException("Host part of Jid is missing", 0);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Jid))
            return false;
        Jid jid = (Jid)o;
        boolean userEqual = (jid.user == null && this.user == null) || this.user.equals(jid.user);
        boolean hostEqual = this.host.equals(jid.host);
        boolean resourceEqual = (jid.resource == null && this.resource == null) || this.resource.equals(jid.resource);
        return userEqual && hostEqual && resourceEqual;
    }


    public boolean bareJidEquals(Object o) {
        if(!(o instanceof Jid))
            return false;
        Jid jid = (Jid)o;
        boolean userEqual = (jid.user == null && this.user == null) || this.user.equals(jid.user);
        boolean hostEqual = this.host.equals(jid.host);
        return userEqual && hostEqual;
    }

    @Override
    public String toString() {
        if(user == null && resource == null)
            return host;
        if(resource == null)
            return user + '@' + host;
        if(user == null)
            return host + '/' + resource;
        return user + '@' + host + '/' + resource;
    }
}
