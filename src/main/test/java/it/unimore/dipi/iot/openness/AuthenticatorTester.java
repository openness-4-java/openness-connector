package it.unimore.dipi.iot.openness;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.utils.AuthenticatorFileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/12/2020 - 10:00
 */
public class AuthenticatorTester {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatorTester.class);

    @Test
    public void newApplicationAuthentication() throws EdgeApplicationAuthenticatorException {

        cleanCertsFolder();

        logger.info("Certs Folder correctly removed ...");

        AuthorizedApplicationConfiguration authorizedApplicationConfiguration;

        EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator(
                TesterConfiguration.OPENNESS_CONTROLLER_BASE_AUTH_URL,
                TesterConfiguration.TESTER_JAVA_STORE_PASSWORD);

        Optional<AuthorizedApplicationConfiguration> storedConfiguration = edgeApplicationAuthenticator.loadExistingAuthorizedApplicationConfiguration(
                TesterConfiguration.TESTER_APPLICATION_ID,
                TesterConfiguration.TESTER_ORGANIZATION_NAME);

        assertFalse(storedConfiguration.isPresent());

        logger.info("AuthorizedApplicationConfiguration Not Available ! Authenticating the app ...");

        authorizedApplicationConfiguration = edgeApplicationAuthenticator.authenticateApplication(
                    TesterConfiguration.TESTER_NAMESPACE,
                    TesterConfiguration.TESTER_APPLICATION_ID,
                    TesterConfiguration.TESTER_ORGANIZATION_NAME);

        assertNotNull(authorizedApplicationConfiguration);
        assertNotNull(authorizedApplicationConfiguration.getKeyStoreFilePath());
        assertNotNull(authorizedApplicationConfiguration.getTrustStoreFilePath());
        assertNotNull(authorizedApplicationConfiguration.getApplicationUniqueIdentifier());
        assertEquals(TesterConfiguration.TESTER_APPLICATION_ID, authorizedApplicationConfiguration.getApplicationId());
        assertEquals(TesterConfiguration.TESTER_ORGANIZATION_NAME, authorizedApplicationConfiguration.getOrganizationName());
        assertEquals(TesterConfiguration.TESTER_JAVA_STORE_PASSWORD, authorizedApplicationConfiguration.getStorePassword());
    }

    @Test
    public void existingApplicationAuthentication() throws EdgeApplicationAuthenticatorException {


        AuthorizedApplicationConfiguration authorizedApplicationConfiguration;

        EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator(
                TesterConfiguration.OPENNESS_CONTROLLER_BASE_AUTH_URL,
                TesterConfiguration.TESTER_JAVA_STORE_PASSWORD);

        Optional<AuthorizedApplicationConfiguration> storedConfiguration = edgeApplicationAuthenticator.loadExistingAuthorizedApplicationConfiguration(
                TesterConfiguration.TESTER_APPLICATION_ID,
                TesterConfiguration.TESTER_ORGANIZATION_NAME);

        assertTrue(storedConfiguration.isPresent());

        authorizedApplicationConfiguration = storedConfiguration.get();

        assertNotNull(authorizedApplicationConfiguration);
        assertNotNull(authorizedApplicationConfiguration.getKeyStoreFilePath());
        assertNotNull(authorizedApplicationConfiguration.getTrustStoreFilePath());
        assertNotNull(authorizedApplicationConfiguration.getApplicationUniqueIdentifier());
        assertEquals(TesterConfiguration.TESTER_APPLICATION_ID, authorizedApplicationConfiguration.getApplicationId());
        assertEquals(TesterConfiguration.TESTER_ORGANIZATION_NAME, authorizedApplicationConfiguration.getOrganizationName());
        assertEquals(TesterConfiguration.TESTER_JAVA_STORE_PASSWORD, authorizedApplicationConfiguration.getStorePassword());

    }

    private void cleanCertsFolder(){

        File index = new File(AuthenticatorFileUtils.CERT_BASE_FOLDER);

        if(index.exists()){

            //Remove all files in the folder
            String[]entries = index.list();
            for(String s: entries){
                File currentFile = new File(index.getPath(),s);
                currentFile.delete();
            }

            //Delete the folder
            index.delete();
        }
    }

}
