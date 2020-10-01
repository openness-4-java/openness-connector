package it.unimore.dipi.iot.openness.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 30/09/2020 - 18:57
 */
public class KeyToolTester {

    /*
    public static void main(String[] argv) throws Exception {

        KeyStore ks = KeyStore.getInstance("jks");

        char[] password = "yourKeyStorePass".toCharArray();
        ks.load(null, password);

        FileOutputStream fos = new FileOutputStream("example.ca.jks");
        ks.store(fos, password);
        fos.close();

        String certfile = "certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt";
        FileInputStream is = new FileInputStream("example.ca.jks");

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, "yourKeyStorePass".toCharArray());

        String alias = "eaa.openness";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream certstream = fullStream (certfile);
        Certificate certs =  cf.generateCertificate(certstream);

        ///
        File keystoreFile = new File("example.ca.jks");
        // Load the keystore contents
        FileInputStream in = new FileInputStream(keystoreFile);
        keystore.load(in, password);
        in.close();

        // Add the certificate
        keystore.setCertificateEntry(alias, certs);

        // Save the new keystore contents
        FileOutputStream out = new FileOutputStream(keystoreFile);
        keystore.store(out, password);
        out.close();
    }
    */

    public static void main(String[] argv) throws Exception {

        KeyStore ks = KeyStore.getInstance("pkcs12");

        char[] password = "yourKeyStorePass".toCharArray();
        ks.load(null, password);

        FileOutputStream fos = new FileOutputStream("example.client.chain.p12");
        ks.store(fos, password);
        fos.close();

        String certfile = "example.client.chain.crt";
        FileInputStream is = new FileInputStream("example.client.chain.p12");

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, "yourKeyStorePass".toCharArray());

        String alias = "eaa.openness";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream certstream = fullStream (certfile);
        Certificate certs =  cf.generateCertificate(certstream);

        ///
        File keystoreFile = new File("example.client.chain.p12");
        // Load the keystore contents
        FileInputStream in = new FileInputStream(keystoreFile);
        keystore.load(in, password);
        in.close();

        // Add the certificate
        keystore.setCertificateEntry(alias, certs);

        // Save the new keystore contents
        FileOutputStream out = new FileOutputStream(keystoreFile);
        keystore.store(out, password);
        out.close();
    }

    private static InputStream fullStream ( String fname ) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

}
