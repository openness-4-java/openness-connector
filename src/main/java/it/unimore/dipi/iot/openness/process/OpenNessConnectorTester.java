package it.unimore.dipi.iot.openness.process;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
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

            String applicationId = "OpenNessConnectorTester_v2";
            String nameSpace = "testing_v2";
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
            int nLog = 1;
            logger.info("Posting service: {}", service);
            edgeApplicationConnector.postService(service);

            logger.info("Getting services [#1]...");
            EdgeApplicationServiceList availableServiceList = edgeApplicationConnector.getAvailableServices();
            for(EdgeApplicationServiceDescriptor serviceDescriptor : availableServiceList.getServiceList()){
                logger.info("Service Info: {}", serviceDescriptor);
            }

            logger.info("Getting subscriptions [#1]...");
            SubscriptionList subscriptions = edgeApplicationConnector.getSubscriptions();
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
            logger.info("Posting notification [#1]: {}", notification1);
            edgeApplicationConnector.postNotification(notification1);

            // The Websocket connection should have been previously established by the consumer using GET /notifications before subscribing to any edge service.
            logger.info("Booting websocket for getting notifications...");
            //Define a Custom Notification Handler
            MyNotificationsHandler myNotificationsHandler = new MyNotificationsHandler();
            edgeApplicationConnector.setupNotificationChannel(nameSpace, applicationId, myNotificationsHandler);

            // "The consumer application must establish a Websocket before subscribing to services." (https://www.openness.org/docs/doc/applications/openness_appguide#service-activation)
            logger.info("Posting subscription(s) [#1]: {}", notifications);
            edgeApplicationConnector.postSubscription(notifications, nameSpace, applicationId);
            logger.info("Posting subscription(s) [#2]: {}", notifications);
            edgeApplicationConnector.postSubscription(notifications, nameSpace);

            logger.info("Getting subscriptions [#2]...");
            subscriptions = edgeApplicationConnector.getSubscriptions();
            if (subscriptions.getSubscriptions() == null) {
                logger.info("No subscriptions");
            } else {
                for (Subscription s : subscriptions.getSubscriptions()) {
                    logger.info("Subscription Info: {}", s);
                }
            }

            logger.info("Deleting ALL subscriptions...");
            edgeApplicationConnector.deleteAllSubscriptions();

            logger.info("Getting subscriptions [#3]...");
            subscriptions = edgeApplicationConnector.getSubscriptions();
            if (subscriptions.getSubscriptions() == null) {
                logger.info("No subscriptions");
            } else {
                for (Subscription s : subscriptions.getSubscriptions()) {
                    logger.info("Subscription Info: {}", s);
                }
            }

            final NotificationFromProducer notification2 = new NotificationFromProducer(
                    "fake notification 2",
                    "0.0.2",
                    new NotificationPayload("fake payload 2")
            );
            logger.info("Posting notification [#2]: {}", notification2);
            edgeApplicationConnector.postNotification(notification2);

            final NotificationFromProducer notification22 = new NotificationFromProducer(
                    "fake notification 2",
                    "0.0.2",
                    new NotificationPayload("fake payload 22")
            );
            logger.info("Posting notification [#3]: {}", notification22);
            edgeApplicationConnector.postNotification(notification22);

            /* NOT WORKING, NOT SUBSCRIBED YET...(this is intended behaviour) */
            logger.info("Terminating notifications websocket [#1]...");
            edgeApplicationConnector.closeNotificationChannel();
            myNotificationsHandler.awaitClose(5, TimeUnit.SECONDS);

            notifications.clear();
            notifications.add(new EdgeApplicationServiceNotificationDescriptor(
                    "terminate",
                    "1.0.0",
                    "To get termination requests"
            ));
            logger.info("Posting subscription(s) [#3]: {}", notifications);
            edgeApplicationConnector.postSubscription(notifications, nameSpace, applicationId);

            logger.info("Getting subscriptions [#4]...");
            subscriptions = edgeApplicationConnector.getSubscriptions();
            if (subscriptions.getSubscriptions() == null) {
                logger.info("No subscriptions");
            } else {
                for (Subscription s : subscriptions.getSubscriptions()) {
                    logger.info("Subscription Info: {}", s);
                }
            }

            /* NOW WORKING, NOW SUBSCRIBED */
            logger.info("Terminating notifications websocket [#2]...");
            edgeApplicationConnector.closeNotificationChannel();

            logger.info("Deleting service...");
            edgeApplicationConnector.deleteService();

            logger.info("Getting services [#2]...");
            availableServiceList = edgeApplicationConnector.getAvailableServices();
            for(EdgeApplicationServiceDescriptor serviceDescriptor : availableServiceList.getServiceList()){
                logger.info("Service Info: {}", serviceDescriptor);
            }

            myNotificationsHandler.awaitClose(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
