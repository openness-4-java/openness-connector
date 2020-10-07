package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceList;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceUrn;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
                throw getEdgeApplicationConnectorException(response, "Error retrieving Service List ! Status Code: %d -> Response Body: %s");
            }

        }catch (Exception e){
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationConnectorException(errorMsg);
        }
    }

    public void postService(final EdgeApplicationServiceDescriptor service) throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%sservices", this.edgeApplicationServiceEndpoint);
        logger.debug("Post Service - Target Url: {}", targetUrl);
        final HttpPost postService = new HttpPost(targetUrl);
        try {
            final String serviceJsonString = this.objectMapper.writeValueAsString(service);
            postService.setEntity(new StringEntity(serviceJsonString));
            final CloseableHttpResponse response = httpClient.execute(postService);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                logger.debug("Application Connector Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error posting Service ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error posting Service ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    private EdgeApplicationConnectorException getEdgeApplicationConnectorException(CloseableHttpResponse response, String s) throws IOException {
        return new EdgeApplicationConnectorException(String.format(s,
                response != null ? response.getStatusLine().getStatusCode() : -1,
                response != null ? EntityUtils.toString(response.getEntity()) : null));
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
