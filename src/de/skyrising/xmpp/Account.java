package de.skyrising.xmpp;

import de.skyrising.xmpp.core.SaslAuthentication;

public class Account {
    public Jid jid;
    public String username;
    private String password;

    public Account(Jid jid) {
        this.jid = jid;
    }

    public String getUsername() {
        return username != null ? username : jid.user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        if(Util.getCallingClass() != SaslAuthentication.class) throw new SecurityException("Not called from SASL class");
        if(password != null) return password;

        return null;
    }
}
