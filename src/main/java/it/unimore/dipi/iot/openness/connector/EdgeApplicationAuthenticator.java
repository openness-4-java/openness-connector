package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.dto.auth.ApplicationAuthenticationRequest;
import it.unimore.dipi.iot.openness.dto.auth.ApplicationAuthenticationResponse;
import it.unimore.dipi.iot.openness.exception.CommandLineException;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.utils.AuthenticatorFileUtils;
import it.unimore.dipi.iot.openness.utils.CommandLineExecutor;
import it.unimore.dipi.iot.openness.utils.LinuxCliExecutor;
import it.unimore.dipi.iot.openness.utils.PemFileManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Entry point for authentication:
 *  (1) create authenticator by passing target authentication URL
 *  (2) authenticate your app by passing app name space, id, org
 *  (3) profit!
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationAuthenticator.class);

    private final String DEFAULT_JAVA_STORE_PASSWORD = "changeit";

    private String javaStorePassword;

    private String controllerApiEndpoint;

    private static CloseableHttpClient httpClient;

    private final ObjectMapper objectMapper;

    private KeyPair keyPair = null;

    /**
     * Builds the authenticator object
     * Use the default password. In the next release this constructor will be removed
     * @param controllerApiEndpoint complete URL where to contact the authentication endpoint
     */
    @Deprecated
    public EdgeApplicationAuthenticator(String controllerApiEndpoint) {
        this.javaStorePassword = DEFAULT_JAVA_STORE_PASSWORD;
        this.controllerApiEndpoint = controllerApiEndpoint;
        this.objectMapper = new ObjectMapper();
        httpClient = HttpClients.custom()
                .build();
    }

    /**
     * Builds the authenticator object
     * @param controllerApiEndpoint complete URL where to contact the authentication endpoint
     */
    public EdgeApplicationAuthenticator(String controllerApiEndpoint, String javaStorePassword) throws EdgeApplicationAuthenticatorException{

        if(javaStorePassword == null || javaStorePassword.length() == 0)
            throw new EdgeApplicationAuthenticatorException("Incorrect Java Store Password ! Null or Empty");

        this.javaStorePassword = javaStorePassword;
        this.controllerApiEndpoint = controllerApiEndpoint;
        this.objectMapper = new ObjectMapper();
        httpClient = HttpClients.custom()
                .build();
    }

    /**
     * Actually authenticate your app. The obtained configuration object MUST be passed along while creating the connector object
     *
     * @param nameSpace the namespace of your app
     * @param applicationId your app ID
     * @param organizationName your organisation name
     *
     * @return the configuration to pass along while creating the connector object
     *
     * @throws EdgeApplicationAuthenticatorException in case something goes bad while authenticating
     */
    public AuthorizedApplicationConfiguration authenticateApplication(String nameSpace, String applicationId, String organizationName) throws EdgeApplicationAuthenticatorException {

        // input validation
        if (this.controllerApiEndpoint == null)
            throw new EdgeApplicationAuthenticatorException("Invalid OpenNess Controller Endpoint ! Null Endpoint provided !");

        if (httpClient == null)
            throw new EdgeApplicationAuthenticatorException("Error ! HTTP Client = Null !");

        nameSpace = validateNamespace(nameSpace);
        applicationId = validateApplicationId(applicationId);
        organizationName = validateOrganizationName(organizationName);

        //Check if the certs folder is available or create it
        AuthenticatorFileUtils.checkOrCreateCertificatesFolder();

        try {

            // pre-processing
            logger.info("Authenticating Application -> Namespace: {} ApplicationId: {} OrganizationName:{}", nameSpace, applicationId, organizationName);
            String applicationUniqueIdentifier = generateApplicationUniqueIdentifier(applicationId, organizationName);
            checkApplicationKeyPair(applicationUniqueIdentifier);
            Optional<String> csrString = generateCertificateSigningRequest(organizationName, nameSpace, applicationId);
            if(csrString.isEmpty())
                throw new EdgeApplicationAuthenticatorException("Error Generating Certificate Signing Request !");

            // Create ApplicationAuthenticationRequest
            ApplicationAuthenticationRequest applicationAuthenticationRequest = new ApplicationAuthenticationRequest();
            applicationAuthenticationRequest.setCertificateSigningRequest(csrString.get());
            String jsonBody = objectMapper.writeValueAsString(applicationAuthenticationRequest);
            logger.debug("OpenNess Auth JsonBody: {}", jsonBody);
            String AUTH_API_RESOURCE = "auth";
            HttpPost authPost = new HttpPost(this.controllerApiEndpoint + AUTH_API_RESOURCE);
            authPost.setEntity(new StringEntity(jsonBody));
            CloseableHttpResponse response = httpClient.execute(authPost);

            // handle response
            if (response != null) {

                String bodyString = EntityUtils.toString(response.getEntity());
                logger.info("Application Authentication Response Code: {}", response.getStatusLine().getStatusCode());
                logger.debug("Response Body: {}", bodyString);
                String clientCertificateFilePath = AuthenticatorFileUtils.getClientCertificateFilePath(applicationUniqueIdentifier);
                String caChainFilePath = AuthenticatorFileUtils.getCaChainFilePath(applicationUniqueIdentifier);
                String caPoolFilePath = AuthenticatorFileUtils.getCaPoolFilePath(applicationUniqueIdentifier);
                handleAuthenticationResponse(bodyString, clientCertificateFilePath, caChainFilePath, caPoolFilePath);

                //Generate Java TrustStore and KeyStore management files
                String trustStoreOutputFilePath = AuthenticatorFileUtils.getTrustStoreOutputFilePath(applicationUniqueIdentifier);
                String clientChainCertificateOutputFilePath = AuthenticatorFileUtils.getClientChainOutputFilePath(applicationUniqueIdentifier);
                String keyStoreOutputFilePath = AuthenticatorFileUtils.getKeyStoreOutputFilePath(applicationUniqueIdentifier);
                String DOMAIN_ALIAS = "eaa.openness";
                generateJavaSecurityFiles(this.javaStorePassword,
                        DOMAIN_ALIAS,
                        clientCertificateFilePath,
                        AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier),
                        caChainFilePath,
                        trustStoreOutputFilePath,
                        clientChainCertificateOutputFilePath,
                        keyStoreOutputFilePath,
                        String.format("%s%s", applicationId, organizationName));

                // build returned configuration
                return new AuthorizedApplicationConfiguration(applicationUniqueIdentifier,
                        applicationId,
                        organizationName,
                        trustStoreOutputFilePath,
                        keyStoreOutputFilePath,
                        this.controllerApiEndpoint,
                        this.javaStorePassword);
            } else {
                String errorMsg = "Error Authenticating Application ! Response Body: NULL !";
                logger.error(errorMsg);
                throw new EdgeApplicationAuthenticatorException(errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    /**
     * Utility method to generate a unique app ID as SHA-256 encoding of app ID and org name
     *
     * @param applicationId your app ID
     * @param organizationName your org name
     *
     * @return the unique app id
     *
     * @throws NoSuchAlgorithmException in case of missing SHA-256 implementation
     * @throws EdgeApplicationAuthenticatorException in the case of invalid app ID and/or org name
     */
    public static String generateApplicationUniqueIdentifier(String applicationId, String organizationName) throws NoSuchAlgorithmException, EdgeApplicationAuthenticatorException {
        applicationId = validateApplicationId(applicationId);
        organizationName = validateOrganizationName(organizationName);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(String.format("%s%s", applicationId, organizationName).getBytes(StandardCharsets.UTF_8));
        String result = bytesToHex(encodedhash);
        logger.info("Unique ApplicationId for ApplicationId: {} and OrganizationName:{} -> {}", applicationId, organizationName, result);
        return result;
    }

    /**
     * Utility method to retrieve the configuration of your already authenticated app
     *
     * @param applicationId your app ID
     * @param organizationName your org name
     *
     * @return the configuration of your already authenticated app, if any
     */
    public Optional<AuthorizedApplicationConfiguration> loadExistingAuthorizedApplicationConfiguration(String applicationId, String organizationName) {
        try {
            applicationId = validateApplicationId(applicationId);
            organizationName = validateOrganizationName(organizationName);
            logger.info("Loading Authorized Application Configuration for ApplicationId: {} and OrganizationName:{}", applicationId, organizationName);
            String applicationUniqueIdentifier = generateApplicationUniqueIdentifier(applicationId, organizationName);
            if (AuthenticatorFileUtils.isJavaClientAuthenticationFilesAvailable(applicationUniqueIdentifier))
                return Optional.of(new AuthorizedApplicationConfiguration(
                        applicationUniqueIdentifier,
                        applicationId,
                        organizationName,
                        AuthenticatorFileUtils.getTrustStoreOutputFilePath(applicationUniqueIdentifier),
                        AuthenticatorFileUtils.getKeyStoreOutputFilePath(applicationUniqueIdentifier),
                        this.controllerApiEndpoint,
                        this.javaStorePassword));
            else
                return Optional.empty();
        } catch (Exception e) {
            logger.error("Error Loading Existing AuthorizedApplicationConfiguration !");
            return Optional.empty();
        }
    }

    private static String validateNamespace(String namespace) throws EdgeApplicationAuthenticatorException {
        if (namespace != null)
            return namespace.trim().replaceAll("\\s", "");
        else
            throw new EdgeApplicationAuthenticatorException("Null Namespace !");
    }

    private static String validateApplicationId(String appId) throws EdgeApplicationAuthenticatorException {
        if (appId != null)
            return appId.trim().replaceAll("\\s", "");
        else
            throw new EdgeApplicationAuthenticatorException("Null Application Id !");
    }

    private static String validateOrganizationName(String organizationName) throws EdgeApplicationAuthenticatorException {
        if (organizationName != null)
            return organizationName.trim().replaceAll("\\s", "");
        else
            throw new EdgeApplicationAuthenticatorException("Null OrganizationName !");
    }

    private void generateNewKeyPair(String applicationUniqueIdentifier) throws EdgeApplicationAuthenticatorException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();
            writePemFile(privateKey, "PRIVATE KEY", AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier));
            writePemFile(publicKey, "PUBLIC KEY", AuthenticatorFileUtils.getPublicKeyFilePath(applicationUniqueIdentifier));
            this.keyPair = pair;
        } catch (Exception e) {
            String errorMsg = String.format("Error Generating New KeyPair %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    private static void writePemFile(Key key, String description, String filename) throws IOException {
        PemFileManager pemFileManager = new PemFileManager(key, description);
        pemFileManager.write(filename);
        logger.info(String.format("%s successfully written in file %s.", description, filename));
    }

    private Optional<KeyPair> readLocalKeyPair(String applicationUniqueIdentifier) {
        try {
            PrivateKey privateKey = PemFileManager.loadPrivateKey(AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier));
            PublicKey publicKey = PemFileManager.loadPublicKey(AuthenticatorFileUtils.getPublicKeyFilePath(applicationUniqueIdentifier));
            if (privateKey != null && publicKey != null) {
                return Optional.of(new KeyPair(publicKey, privateKey));
            } else {
                logger.error("Error Loading existing KeyPair ! Null values !");
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error Loading existing KeyPair ! {}", e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private Optional<String> generateCertificateSigningRequest(String organization, String nameSpace, String appId) throws EdgeApplicationAuthenticatorException {
        try {
            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal(String.format("CN=%s:%s, O=%s", nameSpace, appId, organization)), this.keyPair.getPublic());  // commonName = nameSpace:app-id
            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withECDSA");
            ContentSigner signer = csBuilder.build(this.keyPair.getPrivate());
            PKCS10CertificationRequest csr = p10Builder.build(signer);
            logger.debug("CSR Encoded: {}", csr.getEncoded());
            StringWriter writer = new StringWriter();
            PemWriter pemWriter = new PemWriter(writer);
            PemObject pemObject = new PemObject("CERTIFICATE REQUEST", csr.getEncoded());
            pemWriter.writeObject(pemObject);
            pemWriter.flush();
            pemWriter.close();
            writer.close();
            return Optional.of(writer.toString());
        } catch (Exception e) {
            String errorMsg = String.format("Error Generating CertificateSigningRequest %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    private void checkApplicationKeyPair(String applicationUniqueIdentifier) throws EdgeApplicationAuthenticatorException {
        //Read local KeyPair
        Optional<KeyPair> localKeyPair = readLocalKeyPair(applicationUniqueIdentifier);
        if (localKeyPair.isPresent()) {
            logger.info("Local KeyPair correctly loaded ! Public File: {} and Private File: {}", AuthenticatorFileUtils.getPublicKeyFilePath(applicationUniqueIdentifier), AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier));
            this.keyPair = localKeyPair.get();
        } else {
            logger.warn("Local KeyPair not available or existing ! Generating a new KeyPair ....");
            generateNewKeyPair(applicationUniqueIdentifier);
        }
    }

    private void handleAuthenticationResponse(String responseBody, String clientCertificateFilePath, String caChainFilePath, String caPoolFilePath) throws EdgeApplicationAuthenticatorException {
        try {
            ApplicationAuthenticationResponse applicationAuthenticationResponse = objectMapper.readValue(responseBody, ApplicationAuthenticationResponse.class);
            //Save Certificate
            writeCertificateOnFile(clientCertificateFilePath, applicationAuthenticationResponse.getCertificate());
            //Save CA Chain files
            StringBuffer certChainStringBuffer = new StringBuffer();
            for (String caCertString : applicationAuthenticationResponse.getCaChainList())
                certChainStringBuffer.append(caCertString);
            //Save CA Chain Certificate
            writeCertificateOnFile(caChainFilePath, certChainStringBuffer.toString());
            //Save CA Pool files
            certChainStringBuffer = new StringBuffer();
            for (String caCertString : applicationAuthenticationResponse.getCaPoolList())
                certChainStringBuffer.append(caCertString);
            //Save CA Chain Certificate
            writeCertificateOnFile(caPoolFilePath, certChainStringBuffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    /**
     * Utility method to generate required security files
     * Currently it support only Linux environment with the following applications:
     *
     * - keytool
     * - openssl
     *
     * @param storePassword password associate to the security files
     * @param domainAlias domain alias for Java security files
     * @param clientCertificateFilePath path of the client certificate file
     * @param clientPrivateKey private key of the client
     * @param caCertificateFilePath CA Certificate file path
     * @param trustStoreOutputFilePath target Java TrustStore output File Path
     * @param clientChainCertificateOutputFilePath target Client Chain Certificate file path
     * @param keyStoreOutputFilePath target Java KeyStore output File Path
     * @param appId Id of the target application
     *
     * @throws CommandLineException
     * @throws EdgeApplicationAuthenticatorException
     * @throws IOException
     */
    private void generateJavaSecurityFiles(String storePassword,
                                          String domainAlias,
                                          String clientCertificateFilePath,
                                          String clientPrivateKey,
                                          String caCertificateFilePath,
                                          String trustStoreOutputFilePath,
                                          String clientChainCertificateOutputFilePath,
                                          String keyStoreOutputFilePath,
                                          String appId) throws CommandLineException, EdgeApplicationAuthenticatorException, IOException {
        CommandLineExecutor commandLineExecutor = new LinuxCliExecutor();
        //Remove Already Existing Java TrustStore File
        String removeExistingKeyStore = String.format("rm -f %s", trustStoreOutputFilePath);
        if (commandLineExecutor.executeCommand(removeExistingKeyStore) != 0)
            throw new EdgeApplicationAuthenticatorException(String.format("Error generating Java TrustStore with command: %s", removeExistingKeyStore));
        //Add CA Certificate to Java TrustStore
        String caTrustStoreCommand = String.format("keytool -noprompt -importcert -storetype jks -alias %s -keystore %s -file %s -storepass %s", domainAlias, trustStoreOutputFilePath, caCertificateFilePath, storePassword);
        if (commandLineExecutor.executeCommand(caTrustStoreCommand) != 0)
            throw new EdgeApplicationAuthenticatorException(String.format("Error generating Java TrustStore with command: %s", caTrustStoreCommand));
        //Add Client Certificates to Java KeyStore
        //The command line solution has a problem with the standard output redirect. A native Java solution has been added
        //String combinedCertificate = String.format("cat %s %s %s > %s", caCertificateFilePath, clientCertificateFilePath, clientPrivateKey, clientChainCertificateOutputFilePath);
        //if(commandLineExecutor.executeCommand(combinedCertificate) != 0);
        //throw new EdgeApplicationAuthenticatorException(String.format("Error generating Client Certificate Chain with command: %s", combinedCertificate));
        mergeClientFiles(caCertificateFilePath, clientCertificateFilePath, clientPrivateKey, clientChainCertificateOutputFilePath);
        //Create PKCS12 Java KeyStore File
        String keystoreCommand = String.format("openssl pkcs12 -export -in %s -out %s -password pass:%s -name %s -noiter -nomaciter", clientChainCertificateOutputFilePath, keyStoreOutputFilePath, storePassword, appId);
        if (commandLineExecutor.executeCommand(keystoreCommand) != 0)
            throw new EdgeApplicationAuthenticatorException(String.format("Error generating Java KeyStore with command: %s", keystoreCommand));
    }

    private void mergeClientFiles(String caCertificateFilePath,
                                  String clientCertificateFilePath,
                                  String clientPrivateKey,
                                  String clientChainCertificateOutputFilePath) throws IOException {
        // Input files
        List<Path> inputs = Arrays.asList(
                Paths.get(caCertificateFilePath),
                Paths.get(clientCertificateFilePath),
                Paths.get(clientPrivateKey)
        );
        // Output file
        Path output = Paths.get(clientChainCertificateOutputFilePath);
        // Charset for read and write
        Charset charset = StandardCharsets.UTF_8;
        // Join files (lines)
        for (Path path : inputs) {
            logger.debug("Merging File: {}", path.getFileName());
            List<String> lines = Files.readAllLines(path, charset);
            Files.write(output, lines, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void writeCertificateOnFile(String filePath, String contentString) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(contentString);
        writer.close();
    }

    /**
     * Gets the complete URL of the target authentication endpoint
     *
     * @return the complete URL of the target authentication endpoint
     */
    public String getControllerApiEndpoint() {
        return controllerApiEndpoint;
    }

}
