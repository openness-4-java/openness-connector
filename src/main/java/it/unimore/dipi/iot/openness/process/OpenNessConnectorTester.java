package it.unimore.dipi.iot.openness.process;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.connector.NotificationsHandle;
import it.unimore.dipi.iot.openness.dto.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 16:48
 */
public class OpenNessConnectorTester {

    private static final Logger logger = LoggerFactory.getLogger(OpenNessConnectorTester.class);

    public static void main(String[] args) {

        try {

            String OPENNESS_CONTROLLER_BASE_AUTH_URL = "http://eaa.openness:7080/";
            String OPENNESS_CONTROLLER_BASE_APP_URL = "https://eaa.openness:7443/";
            String OPENNESS_CONTROLLER_BASE_APP_WS_URL = "wss://eaa.openness:7443/";

            String applicationId = "OpenNessConnectorTester";
            String nameSpace = "testing";
            String organizationName =  "DIPIUniMore";

            AuthorizedApplicationConfiguration authorizedApplicationConfiguration;

            EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator(OPENNESS_CONTROLLER_BASE_AUTH_URL);

            Optional<AuthorizedApplicationConfiguration> storedConfiguration = edgeApplicationAuthenticator.loadExistingAuthorizedApplicationConfiguration(applicationId, organizationName);

            if(storedConfiguration.isPresent()) {
                logger.info("AuthorizedApplicationConfiguration Loaded Correctly !");
                authorizedApplicationConfiguration = storedConfiguration.get();
            }
            else {
                logger.info("AuthorizedApplicationConfiguration Not Available ! Authenticating the app ...");
                authorizedApplicationConfiguration = edgeApplicationAuthenticator.authenticateApplication(nameSpace, applicationId, organizationName);
            }

            EdgeApplicationConnector edgeApplicationConnector = new EdgeApplicationConnector(OPENNESS_CONTROLLER_BASE_APP_URL, authorizedApplicationConfiguration, OPENNESS_CONTROLLER_BASE_APP_WS_URL);

            final List<EdgeApplicationServiceNotificationDescriptor> notifications = new ArrayList<>();
            final EdgeApplicationServiceNotificationDescriptor notificationDescriptor1 = new EdgeApplicationServiceNotificationDescriptor(
                    "fake notification 1",
                    "0.0.1",
                    "fake description 2"
            );
            final EdgeApplicationServiceNotificationDescriptor notificationDescriptor2 = new EdgeApplicationServiceNotificationDescriptor(
                    "fake notification 2",
                    "0.0.2",
                    "fake description 2"
            );
            notifications.add(notificationDescriptor1);
            notifications.add(notificationDescriptor2);
            final EdgeApplicationServiceDescriptor service = new EdgeApplicationServiceDescriptor(
                    new EdgeApplicationServiceUrn(applicationId, nameSpace),  // MUST BE AS DURING AUTHENTICATION
                    "fake service",
                    String.format("%s/%s", nameSpace, applicationId),  // MUST BE AS DURING AUTHENTICATION
                    "fake status",
                    notifications,
                    new ServiceInfo("fake info")
            );
            logger.info("Posting service: {}", service);
            edgeApplicationConnector.postService(service);

            logger.info("Getting services...");
            EdgeApplicationServiceList availableServiceList = edgeApplicationConnector.getAvailableServices();
            for(EdgeApplicationServiceDescriptor serviceDescriptor : availableServiceList.getServiceList()){
                logger.info("Service Info: {}", serviceDescriptor);
            }

            logger.info("Getting subscritpions...");
            final SubscriptionList subscriptions = edgeApplicationConnector.getSubscriptions();
            if (subscriptions.getSubscriptions() == null) {
                logger.info("No subscriptions");
            } else {
                for (Subscription s : subscriptions.getSubscriptions()) {
                    logger.info("Subscription Info: {}", s);
                }
            }

            final NotificationFromProducer notification1 = new NotificationFromProducer(
                    "fake notification 1",
                    "0.0.1",
                    new NotificationPayload("fake payload 1")
            );
            logger.info("Posting notification: {}", notification1);
            edgeApplicationConnector.postNotification(notification1);

            // The Websocket connection should have been previously established by the consumer using GET /notifications before subscribing to any edge service.
            logger.info("Booting websocket for getting notifications...");
            final NotificationsHandle notificationsHandle = edgeApplicationConnector.getNotificationsWebSocket(nameSpace, applicationId, "notifications");

            // "The consumer application must establish a Websocket before subscribing to services." (https://www.openness.org/docs/doc/applications/openness_appguide#service-activation)
            logger.info("Posting subscription(s): {}", notifications);
            edgeApplicationConnector.postSubscription(notifications, nameSpace, applicationId);
            logger.info("Again: {}", notifications);
            edgeApplicationConnector.postSubscription(notifications, nameSpace);

            final NotificationFromProducer notification2 = new NotificationFromProducer(
                    "fake notification 2",
                    "0.0.2",
                    new NotificationPayload("fake payload 2")
            );
            logger.info("Posting notification: {}", notification2);
            edgeApplicationConnector.postNotification(notification2);

            final NotificationFromProducer notification22 = new NotificationFromProducer(
                    "fake notification 2",
                    "0.0.2",
                    new NotificationPayload("fake payload 22")
            );
            logger.info("Posting notification: {}", notification22);
            edgeApplicationConnector.postNotification(notification22);

            notificationsHandle.awaitClose(5, TimeUnit.SECONDS);  // TODO when to close? how long to wait?

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
