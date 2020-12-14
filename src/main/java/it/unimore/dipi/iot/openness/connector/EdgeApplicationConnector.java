package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.dto.service.*;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Entry point for authentication:
 *  (1) create connector by passing authorised application configuration, and https/wss target URLs
 *  (3) profit!
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationConnector {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationConnector.class);
    private static CloseableHttpClient httpClient;
    private AuthorizedApplicationConfiguration authorizedApplicationConfiguration;
    private final ObjectMapper objectMapper;
    private String edgeApplicationServiceEndpoint;
    private final String edgeApplicationServiceWsEndpoint;
    private final WebSocketClient wsClient;

    /**
     * Builds the connector object
     *
     * @param edgeApplicationServiceEndpoint complete URL where to contact the application API endpoint over https
     * @param authorizedApplicationConfiguration the configuration obtained through the authenticator
     * @param edgeApplicationServiceWsEndpoint complete URL where to contact the application API endpoint over wss
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public EdgeApplicationConnector(String edgeApplicationServiceEndpoint, AuthorizedApplicationConfiguration authorizedApplicationConfiguration, final String edgeApplicationServiceWsEndpoint) throws EdgeApplicationConnectorException {
        try {
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
            SslContextFactory ssl = new SslContextFactory.Client(true);
            ssl.setKeyStorePath(this.authorizedApplicationConfiguration.getKeyStoreFilePath());
            ssl.setTrustStorePath(this.authorizedApplicationConfiguration.getTrustStoreFilePath());
            ssl.setKeyStorePassword(this.authorizedApplicationConfiguration.getStorePassword());
            final HttpClient client = new HttpClient(ssl);
            this.wsClient = new WebSocketClient(client);
        } catch (Exception e) {
            throw new EdgeApplicationConnectorException("Error initializing the connector ! Error: " + e.getLocalizedMessage());
        }
    }

    /**
     * Called by consumers. Gets list of services already registered to Openness.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/GetServices
     *
     * @return the list of services already registered to Openness
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public EdgeApplicationServiceList getAvailableServices() throws EdgeApplicationConnectorException {
        try {
            String targetUrl = String.format("%sservices", this.edgeApplicationServiceEndpoint);
            logger.debug("Get Service List - Target Url: {}", targetUrl);
            HttpGet getServiceList = new HttpGet(targetUrl);
            CloseableHttpResponse response = httpClient.execute(getServiceList);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                String bodyString = EntityUtils.toString(response.getEntity());
                logger.debug("Getting available Services Response Code: {}", response.getStatusLine().getStatusCode());
                logger.debug("Response Body: {}", bodyString);
                return objectMapper.readValue(bodyString, EdgeApplicationServiceList.class);
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error retrieving Service List ! Status Code: %d -> Response Body: %s");
            }
        } catch (Exception e) {
            String errorMsg = String.format("Error retrieving Service List ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationConnectorException(errorMsg);
        }
    }

    /**
     * Called by producers. Registers the caller service for producing given notifications to Openness.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/RegisterApplication
     *
     * @param service the service descriptor to register
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
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
                logger.debug("Posting service Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error posting Service ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error posting Service ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by producers. Deregisters the caller service from Openness.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/DeregisterApplication
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void deleteService() throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%sservices", this.edgeApplicationServiceEndpoint);
        logger.debug("Delete Service - Target Url: {}", targetUrl);
        final HttpDelete deleteService = new HttpDelete(targetUrl);
        try {
            final CloseableHttpResponse response = httpClient.execute(deleteService);
            if (response != null && response.getStatusLine().getStatusCode() == 204) {
                logger.debug("Deleting service Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error deleting Service ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error deleting Service ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by consumers. Gets list of subscriptions active for the caller.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/GetSubscriptions
     *
     * @return list of subscriptions active for the caller
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public SubscriptionList getSubscriptions() throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%ssubscriptions", this.edgeApplicationServiceEndpoint);
        logger.debug("Get Subscriptions - Target Url: {}", targetUrl);
        final HttpGet getSubscriptions = new HttpGet(targetUrl);
        try {
            final CloseableHttpResponse response = httpClient.execute(getSubscriptions);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                final String body = EntityUtils.toString(response.getEntity());
                logger.debug("Getting Subscriptions Response Code: {}", response.getStatusLine().getStatusCode());
                logger.debug("Response Body: {}", body);
                return objectMapper.readValue(body, SubscriptionList.class);
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error getting Subscriptions ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error getting Subscriptions ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by consumers. Subscribes to a set of notifications (descriptors) coming from a service within the given namespace.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/SubscribeNotifications
     * Before subscribing, it is NECESSARY to set up the notification channel: see method setupNotificationChannel
     *
     * @param notifications the set of notifications (descriptors) to subscribe to
     * @param nameSpace the namespace of the service to subscribe to
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void postSubscription(final List<EdgeApplicationServiceNotificationDescriptor> notifications, final String nameSpace) throws EdgeApplicationConnectorException {
        this.postSubscription(notifications, nameSpace, "");
    }

    /**
     * Called by consumers. Subscribes to a set of notifications (descriptors) coming from a service within the given namespace and with the given ID.
     * https://www.openness.org/api-documentation/?api=eaa#/Eaa/SubscribeNotifications2
     * Before subscribing, it is NECESSARY to set up the notification channel: see method setupNotificationChannel
     *
     * @param notifications the set of notifications (descriptors) to subscribe to
     * @param nameSpace the namespace of the service to subscribe to
     * @param applicationId the ID of the service to subscribe to
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void postSubscription(final List<EdgeApplicationServiceNotificationDescriptor> notifications, final String nameSpace, final String applicationId) throws EdgeApplicationConnectorException {
        String targetUrl;  // When the consumer application decides on a particular service that it would like to subscribe to, it should call POST /subscriptions/{urn.namespace} to subscribe to all services available in a namespace or call POST /subscriptions/{urn.namespace}/{urn.id} to subscribe to notifications related to the exact producer.
        if (!nameSpace.equals("")) {
            if (!applicationId.equals("")) {
                targetUrl = String.format("%ssubscriptions/%s/%s", this.edgeApplicationServiceEndpoint, nameSpace, applicationId);
            } else {
                targetUrl = String.format("%ssubscriptions/%s", this.edgeApplicationServiceEndpoint, nameSpace);
            }
        } else {
            targetUrl = String.format("%ssubscriptions", this.edgeApplicationServiceEndpoint);
        }
        final HttpPost postSubscription = new HttpPost(targetUrl);
        logger.debug("Post Subscription - Target Url: {}", targetUrl);
        try {
            final String notificationDescriptorJsonString = this.objectMapper.writeValueAsString(notifications);
            logger.debug(notificationDescriptorJsonString);
            postSubscription.setEntity(new StringEntity(notificationDescriptorJsonString));
            final CloseableHttpResponse response = httpClient.execute(postSubscription);
            if (response != null && response.getStatusLine().getStatusCode() == 201) {
                logger.debug("Posting Subscription Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error posting Subscription ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error posting Subscription ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by consumers. Removes all of the caller's subscriptions.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/UnsubscribeAllNotifications
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void deleteAllSubscriptions() throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%ssubscriptions", this.edgeApplicationServiceEndpoint);
        logger.debug("Delete Subscriptions - Target Url: {}", targetUrl);
        final HttpDelete deleteSubscriptions = new HttpDelete(targetUrl);
        try {
            final CloseableHttpResponse response = httpClient.execute(deleteSubscriptions);
            if (response != null && response.getStatusLine().getStatusCode() == 204) {
                logger.debug("Deleting Subscriptions Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error deleting Subscriptions ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error deleting Subscriptions ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by consumers. Removes all subscriptions for the given service namespace.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/UnsubscribeNotifications
     *
     * @param nameSpace the namespace of the service to unsubscribe from
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void deleteSubscription(final String nameSpace) throws EdgeApplicationConnectorException {
        this.deleteSubscription(nameSpace, "");
    }

    /**
     * Called by consumers. Removes all subscriptions for the given service namespace and ID.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/UnsubscribeNotifications2
     *
     * @param nameSpace the namespace of the service to unsubscribe from
     * @param applicationId the ID of the service to unsubscribe from
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void deleteSubscription(final String nameSpace, final String applicationId) throws EdgeApplicationConnectorException {
        String targetUrl;
        if (!nameSpace.equals("")) {
            if (!applicationId.equals("")) {
                targetUrl = String.format("%ssubscriptions/%s/%s", this.edgeApplicationServiceEndpoint, nameSpace, applicationId);
            } else {
                targetUrl = String.format("%ssubscriptions/%s", this.edgeApplicationServiceEndpoint, nameSpace);
            }
        } else {
            targetUrl = String.format("%ssubscriptions", this.edgeApplicationServiceEndpoint);
        }
        final HttpDelete deleteSubscription = new HttpDelete(targetUrl);
        logger.debug("Delete Subscription - Target Url: {}", targetUrl);
        try {
            final CloseableHttpResponse response = httpClient.execute(deleteSubscription);
            if (response != null && response.getStatusLine().getStatusCode() == 204) {
                logger.debug("Deleting Subscription Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error deleting Subscription ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error deleting Subscription ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by consumers. Sets up the websocket notification channel to receive notifications through.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/GetNotifications
     *
     * @param namespace the namespace of your app
     * @param applicationId the ID of your app
     * @param notificationsHandler the separate thread handling incoming notifications
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void setupNotificationChannel(final String namespace, final String applicationId, AbstractWebSocketHandler notificationsHandler) throws EdgeApplicationConnectorException {
        try {
            this.wsClient.start();
            final URI uri = new URI(String.format("%snotifications", this.edgeApplicationServiceWsEndpoint));
            final ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Host", String.format("%s:%s", namespace, applicationId));
            this.wsClient.connect(notificationsHandler, uri, request);
        } catch (Exception e) {
            throw new EdgeApplicationConnectorException(String.format("Error getting Notifications websocket ! Cause: %s -> Msg: %s", e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by consumers. Disconnects the websocket channel through the "poison pill" technique.
     * The poison pill is the TerminateNotification message to which the client websocket handler extending AbstractWebSocketHandler
     * should react to by closing the channel.
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void closeNotificationChannel() throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%snotifications", this.edgeApplicationServiceEndpoint);
        logger.debug("Terminate notifications websocket - Target Url: {}", targetUrl);
        final HttpPost postNotification = new HttpPost(targetUrl);
        try {
            final String notificationJsonString = this.objectMapper.writeValueAsString(new TerminateNotification());
            logger.debug(notificationJsonString);
            postNotification.setEntity(new StringEntity(notificationJsonString));
            final CloseableHttpResponse response = httpClient.execute(postNotification);
            if (response != null && response.getStatusLine().getStatusCode() == 202) {
                logger.debug("Terminating notifications websocket Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error terminating notifications websocket ! Status Code: %d -> Response Body: %s");
            }
        } catch (IOException e) {  // JsonProcessingException | UnsupportedEncodingException | ClientProtocolException
            throw new EdgeApplicationConnectorException(String.format("Error terminating notifications websocket ! Cause: %s -> Msg: %s",
                    e.getCause(), e.getLocalizedMessage()));
        }
    }

    /**
     * Called by producers. Publishes to Openness a notification compatible with the registered service descriptor. The
     * notification will be delivered to subscribers through the websocket channel set up with setupNotificationChannel.
     * See https://www.openness.org/api-documentation/?api=eaa#/Eaa/PushNotificationToSubscribers
     *
     * @param notification the notification to push
     *
     * @throws EdgeApplicationConnectorException in case something goes bad while exploiting the application API
     */
    public void postNotification(final NotificationFromProducer notification) throws EdgeApplicationConnectorException {
        final String targetUrl = String.format("%snotifications", this.edgeApplicationServiceEndpoint);
        logger.debug("Post Notification - Target Url: {}", targetUrl);
        final HttpPost postNotification = new HttpPost(targetUrl);
        try {
            final String notificationJsonString = this.objectMapper.writeValueAsString(notification);
            logger.debug(notificationJsonString);
            postNotification.setEntity(new StringEntity(notificationJsonString));
            final CloseableHttpResponse response = httpClient.execute(postNotification);
            if (response != null && response.getStatusLine().getStatusCode() == 202) {
                logger.debug("Posting Notifications Response Code: {}", response.getStatusLine().getStatusCode());
            } else {
                logger.error("Wrong Response Received !");
                throw getEdgeApplicationConnectorException(response, "Error posting Notification ! Status Code: %d -> Response Body: %s");
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

    /**
     * Gets the configuration of your authorized app
     *
     * @return the configuration of your authorized app
     */
    public AuthorizedApplicationConfiguration getAuthorizedApplicationConfiguration() {
        return authorizedApplicationConfiguration;
    }

    public void setAuthorizedApplicationConfiguration(AuthorizedApplicationConfiguration authorizedApplicationConfiguration) {
        this.authorizedApplicationConfiguration = authorizedApplicationConfiguration;
    }

    /**
     * Gets the Openness service API endpoint
     * @return the Openness service API endpoint
     */
    public String getEdgeApplicationServiceEndpoint() {
        return edgeApplicationServiceEndpoint;
    }

    public void setEdgeApplicationServiceEndpoint(String edgeApplicationServiceEndpoint) {
        this.edgeApplicationServiceEndpoint = edgeApplicationServiceEndpoint;
    }

}
