package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.dto.service.*;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import org.apache.http.HttpHeaders;
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
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Future;

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
    private String edgeApplicationServiceWsEndpoint;

    public EdgeApplicationConnector(String edgeApplicationServiceEndpoint, AuthorizedApplicationConfiguration authorizedApplicationConfiguration, final String edgeApplicationServiceWsEndpoint) throws EdgeApplicationConnectorException {

        try{

            this.edgeApplicationServiceEndpoint = edgeApplicationServiceEndpoint;
            this.edgeApplicationServiceWsEndpoint = edgeApplicationServiceWsEndpoint;
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
            logger.debug(serviceJsonString);
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

    public EdgeApplicationSubscriptionList getSubscriptions() throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%ssubscriptions", this.edgeApplicationServiceEndpoint);
        logger.debug("Get Subscriptions - Target Url: {}", targetUrl);
        final HttpGet getSubscriptions = new HttpGet(targetUrl);
        try {
            final CloseableHttpResponse response = httpClient.execute(getSubscriptions);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                final String body = EntityUtils.toString(response.getEntity());
                logger.debug("Application Connector Response Code: {}", response.getStatusLine().getStatusCode());
                logger.debug("Response Body: {}", body);
                return objectMapper.readValue(body, EdgeApplicationSubscriptionList.class);
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error getting Subscriptions ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error getting Subscriptions ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    public void postSubscription(final EdgeApplicationServiceNotificationDescriptor notificationDescriptor, final String applicationId, final String nameSpace) throws EdgeApplicationConnectorException {
        String targetUrl;  // When the consumer application decides on a particular service that it would like to subscribe to, it should call POST /subscriptions/{urn.namespace} to subscribe to all services available in a namespace or call POST /subscriptions/{urn.namespace}/{urn.id} to subscribe to notifications related to the exact producer.
        if (nameSpace != "") {
            if (applicationId != "") {
                targetUrl = String.format("%ssubscriptions/%s/%s", this.edgeApplicationServiceEndpoint, nameSpace, applicationId);
            } else {
                targetUrl = String.format("%ssubscriptions/%s", this.edgeApplicationServiceEndpoint, nameSpace);
            }
        } else {
            targetUrl = String.format("%ssubscriptions", this.edgeApplicationServiceEndpoint);
        }
        final HttpPost postSubscription = new HttpPost(targetUrl);
        logger.debug("Post subscription - Target Url: {}", targetUrl);
        try {
            final String notificationDescriptorJsonString = this.objectMapper.writeValueAsString(notificationDescriptor);
            logger.debug(notificationDescriptorJsonString);
            postSubscription.setEntity(new StringEntity(notificationDescriptorJsonString));
            final CloseableHttpResponse response = httpClient.execute(postSubscription);
            if (response != null && response.getStatusLine().getStatusCode() == 201) {
                logger.debug("Application Connector Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error posting Service ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error posting Notification ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    public boolean getNotifications(final String nameSpace, final String applicationId) throws EdgeApplicationConnectorException {  // The Websocket connection should have been previously established by the consumer using GET /notifications before subscribing to any edge service.
        final String targetUrl = String.format("%snotifications", this.edgeApplicationServiceEndpoint);  // or this.edgeApplicationServiceWsEndpoint ? ERROR 400 (bad request) with https, "ws/wss protocol not supported" with ws/wss
        logger.debug("Get Notifications - Target Url: {}", targetUrl);
        final HttpGet getNotifications = new HttpGet(targetUrl);
        getNotifications.addHeader(HttpHeaders.CONNECTION, "Upgrade");
        getNotifications.addHeader(HttpHeaders.UPGRADE, "websocket");
        getNotifications.addHeader(HttpHeaders.HOST, String.format("%s:%s", nameSpace, applicationId));
        getNotifications.addHeader("Sec-Websocket-Version", "13");
        getNotifications.addHeader("Sec-Websocket-Key", "xqBt3ImNzJbYqRINxEFlkg==");
        try {
            final CloseableHttpResponse response = httpClient.execute(getNotifications);
            if (response != null && response.getStatusLine().getStatusCode() == 101) {
                logger.debug("Application Connector Response Code: {}", response.getStatusLine().getStatusCode());
                return true;
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error getting Notifications ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error getting Notifications ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    public void establishWebsocket(final String path) {
        logger.info("Establishing WS connection, target: {}{}", this.edgeApplicationServiceWsEndpoint, path);
        final WebSocketClient client = new StandardWebSocketClient();
        final WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        final StompSessionHandler sessionHandler = new NotificationsStompSessionHandler();
        String target;
        if (path != "") {
            target = String.format("%s%s", this.edgeApplicationServiceWsEndpoint, path);
        } else {
            target = this.edgeApplicationServiceWsEndpoint;
        }
        ListenableFuture<StompSession> willConnect = stompClient.connect(target, sessionHandler);
        willConnect.addCallback(
                x -> logger.info("Success: {}", x),
                x -> logger.info("Failure: {} -> {}", x.getCause(), x.getLocalizedMessage())
        );
        //new Scanner(System.in).nextLine(); // Don't close immediately.
    }

    public void postNotification(final NotificationFromProducer notification) throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%snotifications", this.edgeApplicationServiceEndpoint);
        logger.debug("Post notification - Target Url: {}", targetUrl);
        final HttpPost postNotification = new HttpPost(targetUrl);
        try {
            final String notificationJsonString = this.objectMapper.writeValueAsString(notification);
            logger.debug(notificationJsonString);
            postNotification.setEntity(new StringEntity(notificationJsonString));
            final CloseableHttpResponse response = httpClient.execute(postNotification);
            if (response != null && response.getStatusLine().getStatusCode() == 202) {
                logger.debug("Application Connector Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error posting Service ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error posting Notification ! Cause: %s -> Msg: %s",
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
