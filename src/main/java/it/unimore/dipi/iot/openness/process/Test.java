package it.unimore.dipi.iot.openness.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.dto.ApplicationAuthenticationRequest;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;


/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:39
 */
public class Test {

    private static final Logger logger = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) {

        try {

            logger.info("Starting OpenNess Connector Tester ... ");

            //EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator();
            //edgeApplicationAuthenticator.authenticateApplication("Test");

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            //keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();

            //PrivateKey priv = pair.getPrivate();
            //PublicKey pub = pair.getPublic();

            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=Requested Test Certificate"), pair.getPublic());
            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withECDSA");
            ContentSigner signer = csBuilder.build(pair.getPrivate());
            PKCS10CertificationRequest csr = p10Builder.build(signer);

            logger.info("CSR getEncoded: {}", csr.getEncoded());

            //ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //PemWriter pemWriter = new PemWriter(new OutputStreamWriter(outputStream));
            //pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()));
            //logger.info(new String(outputStream.toByteArray()));

            StringWriter writer = new StringWriter();
            PemWriter pemWriter = new PemWriter(writer);

            PemObject pemObject = new PemObject("CERTIFICATE REQUEST", csr.getEncoded());
            pemWriter.writeObject(pemObject);

            pemWriter.flush();
            pemWriter.close();
            writer.close();
            logger.info(writer.toString());

            ApplicationAuthenticationRequest applicationAuthenticationRequest = new ApplicationAuthenticationRequest();
            applicationAuthenticationRequest.setCertificateSigningRequest(writer.toString());

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(applicationAuthenticationRequest);

            logger.info("Json String: {}", jsonString);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
