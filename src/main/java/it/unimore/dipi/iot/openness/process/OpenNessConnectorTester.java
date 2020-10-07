package it.unimore.dipi.iot.openness.process;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 16:48
 */
public class OpenNessConnectorTester {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationAuthenticator.class);

    public static void main(String[] args) {

        try {

            String OPENNESS_CONTROLLER_BASE_AUTH_URL = "http://eaa.openness:7080/";
            String OPENNESS_CONTROLLER_BASE_APP_URL = "https://eaa.openness:7443/";

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

            EdgeApplicationConnector edgeApplicationConnector = new EdgeApplicationConnector(OPENNESS_CONTROLLER_BASE_APP_URL, authorizedApplicationConfiguration);
            final List<EdgeApplicationServiceNotificationDescriptor> notifications = new ArrayList<>();
            notifications.add(new EdgeApplicationServiceNotificationDescriptor(
                    "fake notification",
                    "0.0.1",
                    "fake description"
            ));
            edgeApplicationConnector.postService(new EdgeApplicationServiceDescriptor(
                    new EdgeApplicationServiceUrn(applicationId, nameSpace),  // MUST BE AS DURING AUTHENTICATION
                    "fake service",
                    String.format("%s/%s", nameSpace, applicationId),  // MUST BE AS DURING AUTHENTICATION
                    "fake status",
                    notifications,
                    new ServiceInfo("fake info")
            ));
            final EdgeApplicationSubscriptionList subscriptions = edgeApplicationConnector.getSubscriptions();
            if (subscriptions.getSubscriptions() == null) {
                logger.info("No subscriptions");
            } else {
                for (EdgeApplicationSubscription s : subscriptions.getSubscriptions()) {
                    logger.info("Subscription Info: {}", s);
                }
            }

            EdgeApplicationServiceList availableServiceList = edgeApplicationConnector.getAvailableServices();

            for(EdgeApplicationServiceDescriptor serviceDescriptor : availableServiceList.getServiceList()){
                logger.info("Service Info: {}", serviceDescriptor);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
