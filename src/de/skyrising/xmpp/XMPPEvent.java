package de.skyrising.xmpp;

import java.util.List;

public class XMPPEvent {

    public static class ConnectionChange extends XMPPEvent {
        public final XMPP.StateChange change;

        public ConnectionChange(XMPP.StateChange change) {
            this.change = change;
        }
    }
}
