package de.skyrising.xmpp.core;

import de.skyrising.xmpp.*;
import de.skyrising.xmpp.im.Roster;
import de.skyrising.xmpp.xml.XMLNode;
import de.skyrising.xmpp.xml.XMLStream;
import de.skyrising.xmpp.xml.XMLTag;
import org.xbill.DNS.*;

import javax.net.ssl.*;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Connection extends XMLStream implements Runnable, StanzaHandler, Closeable, HandshakeCompletedListener {

    public Socket socket;

    private InputStream in;
    private PrintStream out;

    private boolean running;
    private boolean streamOpen = false;
    private boolean encrypted = false;

    private List<Feature> streamFeatures = new ArrayList<>();

    public Account account;
    public Jid jid;
    private Jid serverJid;
    private boolean restartStream;
    private boolean pauseStream;
    private boolean authenticated;

    private Consumer<XMPPEvent.ConnectionChange> onChange;
    private Predicate<Feature> onFeatures;

    private SaslAuthentication ongoingAuthentication;

    private Set<String> pendingQueries = new HashSet<>();
    private Map<String, Consumer<IqStanza>> queryCallbacks = new HashMap<>();
    private String resourceBindId;

    public Roster roster;
    public Set<Integer> serverXEPs = new TreeSet<>();

    public Connection(String address) throws ConnectException {
        onChange = (c -> {});
        onFeatures = (f -> false);
        serverJid = new Jid(null, address, null);
        Map<Integer, List<SRVRecord>> srvRecords = Util.getSrvRecord("xmpp-client", "tcp", address);
        try {
            srvRecords.put(65535, Collections.singletonList(
                    new SRVRecord(new Name(address + "."), DClass.IN, 84600, 65535, 100, XMPP.C2S_PORT, new Name(address + "."))));
        } catch (TextParseException ignored) {}
        List<Integer> prios = new ArrayList<>(srvRecords.keySet());
        Collections.sort(prios);
        for(int prio : prios) {
            int totalWeight = 0;
            List<SRVRecord> records = srvRecords.get(prio);
            for(SRVRecord record : records) {
                System.out.println("Prio=" + prio + ", Weight=" + record.getWeight() + ": " + record);
                totalWeight += record.getWeight();
            }
            if(totalWeight == 0 && !records.isEmpty()) totalWeight = 1;
            if(totalWeight == 0) continue;
            int selected = new Random().nextInt(totalWeight);
            int i = 0;
            for(SRVRecord record : records) {
                if(i >= selected) {
                    try {
                        if(tryConnect(new InetSocketAddress(InetAddress.getByName(record.getTarget().toString()), record.getPort())))
                            return;
                    } catch (UnknownHostException|ConnectException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                i += record.getWeight();
            }
        }
        throw new ConnectException();
    }

    public Connection(InetSocketAddress address) throws ConnectException {
        serverJid = new Jid(null, address.getHostString(), null);
        tryConnect(address);
    }

    private boolean tryConnect(InetSocketAddress address) throws ConnectException {
        System.out.println("Trying: " + address);
        try {
            Socket socket = new Socket(address.getAddress(), address.getPort());
            if(socket.isConnected()) {
                this.socket = socket;
                this.in = socket.getInputStream();
                this.out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
                Runtime.getRuntime().addShutdownHook(new Thread(this::endStream));
                return true;
            } else
                return false;
        } catch (IOException e) {
            throw (ConnectException) new ConnectException().initCause(e);
        }
    }

    private void checkConnected() throws IOException {
        if(socket == null || !socket.isConnected())
            throw new IOException("Not connected");
    }

    public void startStream() throws IOException {
        if(!streamOpen) {
            writeStreamHeader("en", !restartStream);
            onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.STARTING));
        }
        streamOpen = true;
        if(!running) new Thread(this).start();
    }

    private void restartStream() throws IOException {
        onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.RESTARTING));
        in = socket.getInputStream();
        out = new PrintStream(socket.getOutputStream());
        if(pauseStream)
            onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.UNPAUSING));
        pauseStream = false;
        restartStream = true;
    }

    public void endStream() {
        onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.STOPPING));
        if(!streamOpen) return;
        streamOpen = false;
        System.out.println("Stream closed");
        writeXML(XMLTag.end("stream:stream"));
    }

    @Override
    public void close() throws IOException {
        endStream();
        try {
            reader.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            socket.close();
        }
    }

    private void writeStreamHeader(String lang, boolean xml) throws IOException {
        checkConnected();
        restartStream = false;
        pauseStream = false;
        if(xml) this.writeXMLHeader();
        XMLNode stream = new XMLNode("stream:stream", "jabber:client").setAttribute("xmlns:stream", XMPP.Namespace.STREAMS);
        stream.setAttribute("xml:lang", lang);
        stream.setAttribute("version", "1.0");
        stream.setAttribute("to", serverJid);
        this.writeXML(XMLTag.start(stream));
    }

    public void send(XMLNode stanza) throws IOException {
        checkConnected();
        beforeSend(stanza);
        this.writeXML(stanza);
    }

    @Override
    public void run() {
        running = true;
        try {
            while (running) {
                startStream();
                initParser();
                while(!restartStream) {
                    while (!restartStream && !pauseStream) {
                        //System.out.println("Receiving...");
                        receive();
                    }
                    Thread.sleep(2);
                }
                System.out.println("Restarting stream");
                streamOpen = false;
            }
        }catch(Exception e) {
            running = false;
            e.printStackTrace();
        }
    }

    private String nextId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean handle(XMLNode node) throws IOException {
        if(node.isType(XMPP.Namespace.STREAMS, "features"))
            return processStreamFeatures(node);
        if(node.isType(XMPP.Namespace.Feature.STARTTLS, "proceed"))
            return handleStarttlsProceed();
        if(node.isType(XMPP.Namespace.Feature.SASL, "success")) {
            onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.SASL_SUCCESS));
            restartStream();
            return true;
        }
        if(node.type.equals("iq")) {
            IqStanza iq = new IqStanza(node);
            String id = iq.getAttribute("id");
            System.out.print("IQ[" + iq.type + "#" + id + "] ");
            iq.getChildren().forEach(System.out::print);
            System.out.println();
            switch(iq.type) {
                case RESULT:
                    iq.getChildren().stream().filter(child -> child.isType("jabber:iq:roster", "query")).findFirst().ifPresent(
                            queryResult -> {
                                String version = queryResult.getAttribute("ver");
                                if(this.roster == null) this.roster = new Roster(this);
                                List<Roster.Item> items = new ArrayList<>();
                                queryResult.getChildren().forEach(child -> items.add(new Roster.Item(node)));
                                this.roster.update(items, version);
                            }
                    );

                    pendingQueries.remove(id);

                    if(id != null && id.equals(resourceBindId)) {
                        resourceBindId = null;
                        iq.childrenOfType(XMPP.Namespace.Feature.BIND, "bind").findFirst()
                                .ifPresent(bind -> bind.childrenOfType("jid").findFirst()
                                        .ifPresent(jidNode -> {
                                            try {
                                                this.jid = new Jid(jidNode.content);
                                                System.out.println("Received full jid: " + jid);
                                            } catch (ParseException e) {
                                                try {
                                                    sendIqError(id);
                                                } catch (IOException e1) {
                                                    throw new RuntimeException(e1);
                                                }
                                                e.printStackTrace();
                                            }
                                        }));
                        onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.RESOURCE_BOUND));
                        return true;
                    }

                    if(queryCallbacks.containsKey(id)) {
                        queryCallbacks.get(id).accept(iq);
                        queryCallbacks.remove(id);
                    } else {
                        System.out.println("No callback");
                    }
                    return true;
                case SET:
                    try {
                        if(!iq.hasAttribute("from") || jid.bareJidEquals(iq.getAttributeAsJid("from")))
                        iq.getChildren().stream().filter(child -> node.isType("jabber:iq:roster", "query")).findFirst().ifPresent(
                                rosterPush -> {
                                    if(this.roster == null) this.roster = new Roster(this);
                                    List<Roster.Item> items = new ArrayList<>();
                                    rosterPush.getChildren().forEach(child -> items.add(new Roster.Item(node)));
                                    this.roster.update(items, roster.version);
                                }
                        );
                    } catch (ParseException e) {
                        e.printStackTrace();
                        sendIqError(id);
                    }

            }
        }
        System.out.println(node);
        return true;
    }

    @Override
    public boolean handleTag(XMLTag tag) {
        if(tag.isStart() && tag.getNode() != null) {
            XMLNode node = tag.getNode();
            if (node.isType(XMPP.Namespace.STREAMS, "stream")) {
                System.out.println("In: " + node.toPrettyString());
                onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.STREAM_OPEN));
                return true;
            }
        }
        if(tag.isEnd() && tag.getNode() != null) {
            XMLNode node = tag.getNode();
            if(node.isType(XMPP.Namespace.STREAMS, "stream")) {
                System.out.println("In: " + node.toPrettyString());
                System.out.println("Stream closed");
                this.endStream();
                onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.STREAM_CLOSED));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleNode(XMLNode node) {
        System.out.println("In: " + node.toPrettyString());
        try {
            return handle(node);
        } catch (IOException e) {
            e.printStackTrace();
            endStream();
        } catch (StanzaError e) {
            sendStanzaError(e);
        }
        return true;
    }

    @Override
    public void beforeSend(XMLNode stanza) {
        if(stanza instanceof IqStanza && !stanza.hasAttribute("id")) {
            String id = nextId();
            stanza.setAttribute("id", id);
            pendingQueries.add(id);
        }
        if(stanza instanceof MessageStanza && !stanza.hasAttribute("id"))
            stanza.setAttribute("id", nextId());
    }

    @Override
    public InputStream getIn() {
        return in;
    }

    @Override
    public PrintStream getOut() {
        return out;
    }

    private boolean processStreamFeatures(XMLNode featureNode) throws IOException {
        streamFeatures.clear();
        streamFeatures.addAll(featureNode.getChildren().stream().map(Feature::parse).collect(Collectors.toList()));
        streamFeatures.stream().filter(f -> f.getType().xep > 1).forEach(f -> serverXEPs.add(f.getType().xep));
        System.out.println("Features: ");
        streamFeatures.stream().forEach(System.out::println);
        final boolean[] flags = {false};
        final List<Feature> required = new ArrayList<>();
        Consumer<Feature> use = (f -> {
            try {
                if(f.required) required.add(0, f);
                flags[0] |= useFeature(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        streamFeatures.stream().filter(onFeatures).findFirst().ifPresent(use);
        if(flags[0]) return true;
        streamFeatures.stream().filter(Feature::isRequired).findFirst().ifPresent(use);
        if(!flags[0] && !required.isEmpty()) //required but not used
            throw new IllegalStateException("Could not handle " + required.get(0));
        return flags[0];
    }

    public boolean useFeature(Feature feature) throws IOException {
        System.out.println("Using " + feature);
        switch (feature.getType()) {
            case STARTTLS:
                switchToTls();
                return true;
            case SASL_MECHANISMS:
                if(account == null)
                    throw new RuntimeException("No Account set");
                @SuppressWarnings("unchecked")
                List<SaslAuthentication.Mechanism> mechanisms = SaslAuthentication.choose(
                        (List<SaslAuthentication.Mechanism>) feature.children);
                System.out.println(mechanisms);
                if(mechanisms.isEmpty())
                    throw new RuntimeException("No valid mechanism");
                onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.AUTHENTICATING));
                this.ongoingAuthentication = new SaslAuthentication(mechanisms.get(0), this);
                this.ongoingAuthentication.sendInitialAuth();
                return true;
            case BIND:
                if(account == null || account.jid == null) return false;
                bindResource(account.jid);
                return true;
        }
        return false;
    }

    private void switchToTls() throws IOException {
        XMLNode starttls = new XMLNode("starttls", XMPP.Namespace.Feature.STARTTLS);
        send(starttls);
    }

    private boolean handleStarttlsProceed() throws IOException {
        System.out.println("Switching to TLS");
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sf.createSocket(socket, socket.getInetAddress().toString(), socket.getPort(), true);
        sslSocket.addHandshakeCompletedListener(this);
        sslSocket.startHandshake();
        pauseStream = true;
        onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.PAUSING));
        return true;
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent hce) {
        try {
            this.socket = hce.getSocket();
            Util.dumpCert(Util.convertCert(hce.getPeerCertificateChain()[0]), System.out);
            System.out.println(hce.getSession().getProtocol() + " with " + hce.getCipherSuite());
            encrypted = true;
            onChange.accept(new XMPPEvent.ConnectionChange(XMPP.StateChange.ENCRYPTED));
            restartStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onChange(Consumer<XMPPEvent.ConnectionChange> onChange) {
        this.onChange = onChange;
    }

    public void onFeatures(Predicate<Feature> onFeatures) {
        this.onFeatures = onFeatures;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void bindResource(Jid jid) throws IOException {
        IqStanza iq = new IqStanza(IqStanza.Type.SET);
        XMLNode bind = new XMLNode("bind", XMPP.Namespace.Feature.BIND);
        bind.addChild(new XMLNode("resource").setContent(jid.resource));
        iq.addChild(bind);
        resourceBindId = nextId();
        iq.setAttribute("id", resourceBindId);
        this.jid = jid;
        this.send(iq);
    }

    public void query(IqStanza query, Consumer<IqStanza> callback) throws IOException {
        String id = query.hasAttribute("id") ? query.getAttribute("id") : nextId();
        query.setAttribute("id", id);
        this.queryCallbacks.put(id, callback);
        this.send(query);
    }

    public void queryRoster(Consumer<Roster> callback) throws IOException {
        IqStanza iq = new IqStanza(IqStanza.Type.GET);
        XMLNode query = new XMLNode("query", XMPP.Namespace.IQ.ROSTER);
        iq.addChild(query);
        if(this.roster != null) query.setAttribute("ver", this.roster.version);
        this.query(iq, result -> callback.accept(this.roster));
    }

    public void sendIqError(String id) throws IOException {
        IqStanza error = new IqStanza(IqStanza.Type.ERROR);
        error.setAttribute("id", id);
        send(error);
    }

    public void sendStanzaError(StanzaError err) {
        try {
            send(err.toXML());
        } catch (IOException e) {
            endStream();
        }
    }
}
