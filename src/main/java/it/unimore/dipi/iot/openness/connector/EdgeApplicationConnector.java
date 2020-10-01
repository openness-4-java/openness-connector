package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceList;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationConnector {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationConnector.class);

    private static CloseableHttpClient httpClient;

    private AuthorizedApplicationConfiguration authorizedApplicationConfiguration;

    private ObjectMapper objectMapper;

    private String edgeApplicationServiceEndpoint;

    public EdgeApplicationConnector(String edgeApplicationServiceEndpoint, AuthorizedApplicationConfiguration authorizedApplicationConfiguration) throws EdgeApplicationConnectorException {

        try{

            this.edgeApplicationServiceEndpoint = edgeApplicationServiceEndpoint;
            this.authorizedApplicationConfiguration = authorizedApplicationConfiguration;
            this.objectMapper = new ObjectMapper();

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(
                            new File(this.authorizedApplicationConfiguration.getKeyStoreFilePath()),
                            this.authorizedApplicationConfiguration.getStorePassword().toCharArray(),
                            this.authorizedApplicationConfiguration.getStorePassword().toCharArray()
                    )
                    .loadTrustMaterial(
                            new File(this.authorizedApplicationConfiguration.getTrustStoreFilePath())
                    )
                    .build();

            /*
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
            */

            httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();

        }catch (Exception e){
            throw new EdgeApplicationConnectorException("Error initializing the connector ! Error: " + e.getLocalizedMessage());
        }
    }

    public EdgeApplicationServiceList getAvailableServices() throws EdgeApplicationConnectorException {

        try{

            String targetUrl = String.format("%sservices", this.edgeApplicationServiceEndpoint);

            logger.debug("Get Service List - Target Url: {}", targetUrl);

            HttpGet getServiceList = new HttpGet(targetUrl);

            CloseableHttpResponse response = httpClient.execute(getServiceList);

            if(response != null && response.getStatusLine().getStatusCode() == 200){

                String bodyString = EntityUtils.toString(response.getEntity());

                logger.debug("Application Authentication Response Code: {}", response.getStatusLine().getStatusCode());
                logger.debug("Response Body: {}", bodyString);

                return objectMapper.readValue(bodyString, EdgeApplicationServiceList.class);

            }
            else {
                logger.error("Wrong Response Received !");
                throw new EdgeApplicationConnectorException(String.format("Error retrieving Service List ! Status Code: %d -> Response Body: %s",
                        response != null ? response.getStatusLine().getStatusCode() : -1,
                        response != null ? EntityUtils.toString(response.getEntity()) : null));
            }

        }catch (Exception e){
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationConnectorException(errorMsg);
        }
    }

    public AuthorizedApplicationConfiguration getAuthorizedApplicationConfiguration() {
        return authorizedApplicationConfiguration;
    }

    public void setAuthorizedApplicationConfiguration(AuthorizedApplicationConfiguration authorizedApplicationConfiguration) {
        this.authorizedApplicationConfiguration = authorizedApplicationConfiguration;
    }

    public String getEdgeApplicationServiceEndpoint() {
        return edgeApplicationServiceEndpoint;
    }

    public void setEdgeApplicationServiceEndpoint(String edgeApplicationServiceEndpoint) {
        this.edgeApplicationServiceEndpoint = edgeApplicationServiceEndpoint;
    }
}
