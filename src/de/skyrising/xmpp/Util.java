package de.skyrising.xmpp;

import org.xbill.DNS.*;

import javax.security.cert.CertificateEncodingException;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {

    public static Map<Integer, List<SRVRecord>> getSrvRecord(String service, String proto, String domain) {
        try {
            Record[] records = new Lookup("_"+service+"._"+proto+"."+domain+".", Type.SRV).run();
            Map<Integer, List<SRVRecord>> srvs = new HashMap<>();
            for(Record record : records)
                if(record instanceof SRVRecord) {
                    int prio = ((SRVRecord) record).getPriority();
                    if(!srvs.containsKey(prio))
                        srvs.put(prio, new ArrayList<>());
                    srvs.get(prio).add((SRVRecord) record);
                }
            return srvs;
        } catch (TextParseException e) {
            return new HashMap<>();
        }
    }

    public static X509Certificate decodeCert(byte[] asn) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(asn));
    }

    public static X509Certificate convertCert(javax.security.cert.X509Certificate cert) throws CertificateEncodingException, CertificateException {
        return decodeCert(cert.getEncoded());
    }

    public static void dumpCert(X509Certificate cert, PrintStream out) throws CertificateParsingException {
        out.println("Version: " + cert.getVersion());
        out.println("Serial Number: " + cert.getSerialNumber().toString(16));
        out.println("Signature Algorithm: " + cert.getSigAlgName());
        out.println("Issuer: " + cert.getIssuerDN());
        out.println("Validity");
        out.println("\tNot Before: " + cert.getNotBefore());
        out.println("\tNot After : " + cert.getNotAfter());
        out.println("Subject: " + cert.getSubjectDN());
        PublicKey publicKey = cert.getPublicKey();
        if(publicKey instanceof RSAPublicKey)
            out.println("Public Key: " + publicKey.getAlgorithm() + "/" + ((RSAPublicKey)publicKey).getModulus().bitLength());
        else
            out.println("Public Key: " + publicKey.getAlgorithm());
        List<List<?>> altNames = new ArrayList<>(cert.getSubjectAlternativeNames());
        if(!altNames.isEmpty()) {
            out.print("Alternative Names:");
            altNames.forEach(a -> out.print(" " + a.get(1)));
            out.println();
        }
    }

    public static Class<?> getCallingClass(){
        try {
            return Class.forName(new Exception().getStackTrace()[2].getClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
