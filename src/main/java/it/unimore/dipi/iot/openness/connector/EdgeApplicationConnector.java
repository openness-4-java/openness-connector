package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationConnector {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationConnector.class);

    private static CloseableHttpClient httpClient;

    private String controllerApiEndpoint = null;

    private ObjectMapper objectMapper = null;

    public EdgeApplicationConnector(){
    }

    public void init(String controllerApiEndpoint) throws EdgeApplicationConnectorException {

        try{

            this.objectMapper = new ObjectMapper();
            this.controllerApiEndpoint = controllerApiEndpoint;


            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(
                            new File("certs/test.client.chain.p12"),
                            "changeit".toCharArray(),
                            "changeit".toCharArray()
                    )
                    .loadTrustMaterial(
                            new File("certs/test.ca.jks")
                    )
                    .build();

            /*
            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(
                            new File("example.client.chain.p12"),
                            "changeit".toCharArray(),
                            "changeit".toCharArray()
                    )
                    .loadTrustMaterial(
                            new File("example.ca.jks")
                    )
                    .build();
            */

            httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();

        }catch (Exception e){
            throw new EdgeApplicationConnectorException("Error initializing the connector ! Error: " + e.getLocalizedMessage());
        }
    }


    public void getAvailableServices() throws EdgeApplicationConnectorException {

        try{

            String targetUrl = String.format("%sservices", this.controllerApiEndpoint);

            logger.debug("Get Service List - Target Url: {}", targetUrl);

            HttpGet getServiceList = new HttpGet(targetUrl);

            CloseableHttpResponse response = httpClient.execute(getServiceList);

            if(response != null ){

                String bodyString = EntityUtils.toString(response.getEntity());

                logger.info("Application Authentication Response Code: {}", response.getStatusLine().getStatusCode());
                logger.info("Response Body: {}", bodyString);

            }
            else
                logger.error("NULL Response Received !");

        }catch (Exception e){

            e.printStackTrace();

            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationConnectorException(errorMsg);
        }
    }


    public static void main(String[] args) throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {


        try {

            //-Djavax.net.ssl.trustStore=example.ca.jks -Djavax.net.ssl.keyStore=example.client.chain.p12 -Djavax.net.ssl.keyStoreType=pkcs12 -Djavax.net.ssl.keyStorePassword=changeit

            //System.setProperty("javax.net.ssl.trustStore", "certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt");
            //System.setProperty("javax.net.ssl.keyStore", "certs/id_ec");

            //System.setProperty("javax.net.debug","all");
            //System.setProperty("javax.net.ssl.trustStore", "example.ca.jks");
            //System.setProperty("javax.net.ssl.keyStore", "example.client.chain.p12");
            //System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
            //System.setProperty("javax.net.ssl.keyStorePassword", "changeit");

            String OPENNESS_CONTROLLER_BASE_URL = "https://eaa.openness:7443/";

            logger.info("Testing EdgeApplicationAuthenticator ....");

            EdgeApplicationConnector edgeApplicationConnector = new EdgeApplicationConnector();
            edgeApplicationConnector.init(OPENNESS_CONTROLLER_BASE_URL);
            edgeApplicationConnector.getAvailableServices();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
