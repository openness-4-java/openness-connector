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
import java.io.*;
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
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationAuthenticator.class);

    private String JAVA_STORE_PASSWORD = "changeit";

    private String DOMAIN_ALIAS = "eaa.openness";

    private String AUTH_API_RESOURCE = "auth";

    private String controllerApiEndpoint = null;

    private static CloseableHttpClient httpClient;

    private ObjectMapper objectMapper = null;

    private KeyPair keyPair = null;

    public EdgeApplicationAuthenticator(String controllerApiEndpoint){
        this.controllerApiEndpoint = controllerApiEndpoint;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.custom()
                .build();

    }

    private void generateNewKeyPair(String applicationUniqueIdentifier) throws EdgeApplicationAuthenticatorException {

        try{

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            KeyPair pair = keyGen.generateKeyPair();

            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            writePemFile(privateKey, "PRIVATE KEY", AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier));
            writePemFile(publicKey, "PUBLIC KEY", AuthenticatorFileUtils.getPublicKeyFilePath(applicationUniqueIdentifier));

            this.keyPair = pair;

        }catch (Exception e){
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

    private Optional<KeyPair> readLocalKeyPair(String applicationUniqueIdentifier){

        try{

            PrivateKey privateKey = PemFileManager.loadPrivateKey(AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier));
            PublicKey publicKey = PemFileManager.loadPublicKey(AuthenticatorFileUtils.getPublicKeyFilePath(applicationUniqueIdentifier));

            if(privateKey != null && publicKey != null){
                return Optional.of(new KeyPair(publicKey, privateKey));
            }
            else {
                logger.error("Error Loading existing KeyPair ! Null values !");
                return Optional.empty();
            }

        } catch (Exception e){
            logger.error("Error Loading existing KeyPair ! {}", e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private Optional<String> generateCertificateSigningRequest(String organization, String commonName) throws EdgeApplicationAuthenticatorException {

        try {

            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal(String.format("CN=%s, O=%s", organization, commonName)), this.keyPair.getPublic());
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

        }catch (Exception e){
            String errorMsg = String.format("Error Generating CertificateSigningRequest %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }

    }

    private void checkApplicationKeyPair(String applicationUniqueIdentifier) throws EdgeApplicationAuthenticatorException {

        //Read local KeyPair
        Optional<KeyPair> localKeyPair = readLocalKeyPair(applicationUniqueIdentifier);

        if(localKeyPair.isPresent()){
            logger.info("Local KeyPair correctly loaded ! Public File: {} and Private File: {}", AuthenticatorFileUtils.getPublicKeyFilePath(applicationUniqueIdentifier), AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier));
            this.keyPair = localKeyPair.get();
        }
        else {
            logger.warn("Local KeyPair not available or existing ! Generating a new KeyPair ....");
            generateNewKeyPair(applicationUniqueIdentifier);
        }
    }

    public AuthorizedApplicationConfiguration authenticateApplication(String applicationId, String organizationName) throws EdgeApplicationAuthenticatorException {

        if(this.controllerApiEndpoint == null)
            throw new EdgeApplicationAuthenticatorException("Invalid OpenNess Controller Endpoint ! Null Endpoint provided !");

        if(httpClient == null)
            throw new EdgeApplicationAuthenticatorException("Error ! HTTP Client = Null !");

        try{

            String applicationUniqueIdentifier = generateApplicationUniqueIdentifier(applicationId, organizationName);

            checkApplicationKeyPair(applicationUniqueIdentifier);

            Optional<String> csrString = generateCertificateSigningRequest(organizationName, applicationId);

            if(!csrString.isPresent())
                throw new EdgeApplicationAuthenticatorException("Error Generating Certificate Signing Request !");

            //Create ApplicationAuthenticationRequest
            ApplicationAuthenticationRequest applicationAuthenticationRequest = new ApplicationAuthenticationRequest();
            applicationAuthenticationRequest.setCertificateSigningRequest(csrString.get());

            String jsonBody = objectMapper.writeValueAsString(applicationAuthenticationRequest);

            logger.debug("OpenNess Auth JsonBody: {}", jsonBody);

            HttpPost authPost = new HttpPost(this.controllerApiEndpoint+AUTH_API_RESOURCE);
            authPost.setEntity(new StringEntity(jsonBody));

            CloseableHttpResponse response = httpClient.execute(authPost);

            if(response != null){

                String bodyString = EntityUtils.toString(response.getEntity());

                logger.info("Application Authentication Response Code: {}", response.getStatusLine().getStatusCode());
                logger.info("Response Body: {}", bodyString);

                String clientCertificateFilePath = AuthenticatorFileUtils.getClientCertificateFilePath(applicationUniqueIdentifier);
                String caChainFilePath = AuthenticatorFileUtils.getCaChainFilePath(applicationUniqueIdentifier);
                String caPoolFilePath = AuthenticatorFileUtils.getCaPoolFilePath(applicationUniqueIdentifier);

                //Handle Authentication Response
                handleAuthenticationResponse(bodyString, clientCertificateFilePath, caChainFilePath, caPoolFilePath);

                //Generate Java TrustStore and KeyStore management files
                String trustStoreOutputFilePath = AuthenticatorFileUtils.getTrustStoreOutputFilePath(applicationUniqueIdentifier);
                String clientChainCertificateOutputFilePath = AuthenticatorFileUtils.getClientChainOutputFilePath(applicationUniqueIdentifier);
                String keyStoreOutputFilePath = AuthenticatorFileUtils.getKeyStoreOutputFilePath(applicationUniqueIdentifier);

                generateJavaSecurityFiles(JAVA_STORE_PASSWORD,
                        DOMAIN_ALIAS,
                        clientCertificateFilePath,
                        AuthenticatorFileUtils.getPrivateKeyFilePath(applicationUniqueIdentifier),
                        caChainFilePath,
                        trustStoreOutputFilePath,
                        clientChainCertificateOutputFilePath,
                        keyStoreOutputFilePath,
                        String.format("%s%s", applicationId, organizationName));

                return new AuthorizedApplicationConfiguration(applicationUniqueIdentifier,
                        applicationId,
                        organizationName,
                        trustStoreOutputFilePath,
                        keyStoreOutputFilePath,
                        this.controllerApiEndpoint,
                        JAVA_STORE_PASSWORD);

            }
            else {
                String errorMsg = String.format("Error Authenticating Application ! Response Body: NULL !");
                logger.error(errorMsg);
                throw new EdgeApplicationAuthenticatorException(errorMsg);
            }

        }catch (Exception e){
            e.printStackTrace();
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    private void handleAuthenticationResponse(String responseBody, String clientCertificateFilePath, String caChainFilePath, String caPoolFilePath) throws EdgeApplicationAuthenticatorException {

        try{

            ApplicationAuthenticationResponse applicationAuthenticationResponse = objectMapper.readValue(responseBody, ApplicationAuthenticationResponse.class);

            //Save Certificate
            writeCertificateOnFile(clientCertificateFilePath, applicationAuthenticationResponse.getCertificate());

            //Save CA Chain files
            StringBuffer certChainStringBuffer = new StringBuffer();
            for(String caCertString : applicationAuthenticationResponse.getCaChainList())
                certChainStringBuffer.append(caCertString);

            //Save CA Chain Certificate
            writeCertificateOnFile(caChainFilePath, certChainStringBuffer.toString());

            //Save CA Pool files
            certChainStringBuffer = new StringBuffer();
            for(String caCertString : applicationAuthenticationResponse.getCaPoolList())
                certChainStringBuffer.append(caCertString);

            //Save CA Chain Certificate
            writeCertificateOnFile(caPoolFilePath, certChainStringBuffer.toString());

        } catch (Exception e){
            e.printStackTrace();
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    public static String generateApplicationUniqueIdentifier(String applicationId, String organizationName) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(String.format("%s%s", applicationId, organizationName).getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
        //return UUID.fromString(String.format("%s%s", applicationId, organizationName)).toString();
    }

    public void generateJavaSecurityFiles(String storePassword,
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
        if(commandLineExecutor.executeCommand(removeExistingKeyStore) != 0)
            throw new EdgeApplicationAuthenticatorException(String.format("Error generating Java TrustStore with command: %s", removeExistingKeyStore));

        //Add CA Certificate to Java TrustStore
        String caTrustStoreCommand = String.format("keytool -noprompt -importcert -storetype jks -alias %s -keystore %s -file %s -storepass %s", domainAlias, trustStoreOutputFilePath, caCertificateFilePath, storePassword);
        if(commandLineExecutor.executeCommand(caTrustStoreCommand) != 0)
            throw new EdgeApplicationAuthenticatorException(String.format("Error generating Java TrustStore with command: %s", caTrustStoreCommand));

        //Add Client Certificates to Java KeyStore
        //The command line solution has a problem with the standard output redirect. A native Java solution has been added
        //String combinedCertificate = String.format("cat %s %s %s > %s", caCertificateFilePath, clientCertificateFilePath, clientPrivateKey, clientChainCertificateOutputFilePath);
        //if(commandLineExecutor.executeCommand(combinedCertificate) != 0);
        //throw new EdgeApplicationAuthenticatorException(String.format("Error generating Client Certificate Chain with command: %s", combinedCertificate));

        mergeClientFiles(caCertificateFilePath, clientCertificateFilePath, clientPrivateKey, clientChainCertificateOutputFilePath);

        //Create PKCS12 Java KeyStore File
        String keystoreCommand = String.format("openssl pkcs12 -export -in %s -out %s -password pass:%s -name %s -noiter -nomaciter", clientChainCertificateOutputFilePath, keyStoreOutputFilePath, storePassword, appId);
        if(commandLineExecutor.executeCommand(keystoreCommand) != 0)
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

    public String getControllerApiEndpoint() {
        return controllerApiEndpoint;
    }

    public void setControllerApiEndpoint(String controllerApiEndpoint) {
        this.controllerApiEndpoint = controllerApiEndpoint;
    }

    public Optional<AuthorizedApplicationConfiguration> loadExistingAuthorizedApplicationConfiguration(String applicationId, String organizationName){

        try{

            String applicationUniqueIdentifier = generateApplicationUniqueIdentifier(applicationId, organizationName);

            if(AuthenticatorFileUtils.isJavaClientAuthenticationFilesAvailable(applicationUniqueIdentifier))
               return Optional.of(new AuthorizedApplicationConfiguration(
                       applicationUniqueIdentifier,
                       applicationId,
                       organizationName,
                       AuthenticatorFileUtils.getTrustStoreOutputFilePath(applicationUniqueIdentifier),
                       AuthenticatorFileUtils.getKeyStoreOutputFilePath(applicationUniqueIdentifier),
                       this.controllerApiEndpoint,
                       JAVA_STORE_PASSWORD));
            else
                return Optional.empty();

        }catch (Exception e){
            logger.error("Error Loading Existing AuthorizedApplicationConfiguration !");
            return Optional.empty();
        }
    }

    public static void main(String[] args) {

        try {

            logger.info("Testing EdgeApplicationAuthenticator ....");


            String OPENNESS_CONTROLLER_BASE_URL = "http://127.0.0.1:7080/auth";

            EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator(OPENNESS_CONTROLLER_BASE_URL);
            AuthorizedApplicationConfiguration authorizedApplicationConfiguration = edgeApplicationAuthenticator.authenticateApplication("authTestConnector", "DIPIUniMore");
            logger.info("Authorized Application Configuration: {}", authorizedApplicationConfiguration);

            /*
            String storePassword = "changeit";
            String domainAlias = "eaa.openness";
            String clientCertificateFilePath = "certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt";
            String clientPrivateKey = "certs/id_ec";
            String caCertificateFilePath = "certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt";
            String trustStoreOutputFilePath = "certs/test2.ca.jks";
            String clientChainCertificateOutputFilePath = "certs/test2.client.chain.crt";
            String keyStoreOutputFilePath = "certs/test2.client.chain.p12";

            edgeApplicationAuthenticator.generateJavaSecurityFiles(storePassword,
                    domainAlias,
                    clientCertificateFilePath,
                    clientPrivateKey,
                    caCertificateFilePath,
                    trustStoreOutputFilePath,
                    clientChainCertificateOutputFilePath,
                    keyStoreOutputFilePath,
                    "TestUniMoreClientApp");
            */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
