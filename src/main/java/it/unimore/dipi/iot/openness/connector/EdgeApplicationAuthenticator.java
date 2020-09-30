package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.dto.ApplicationAuthenticationRequest;
import it.unimore.dipi.iot.openness.dto.ApplicationAuthenticationResponse;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.utils.PemFileManager;
import okhttp3.*;
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
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationAuthenticator.class);

    public static final String OPENNESS_CONTROLLER_BASE_URL = "http://127.0.0.1:7080/auth";

    private static final String PRIVATE_KEY_FILE_PATH = "certs/id_ec";

    private static final String PUBLIC_KEY_FILE_PATH = "certs/id_ec.pub";

    private static final String CERT_BASE_FOLDER = "certs/";

    private OkHttpClient httpClient = null;

    private ObjectMapper objectMapper = null;

    private KeyPair keyPair = null;

    public EdgeApplicationAuthenticator(){
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void init(String controllerApiEndpoint) throws EdgeApplicationAuthenticatorException {

        if(controllerApiEndpoint != null){

            //Read local KeyPair
            Optional<KeyPair> localKeyPair = readLocalKeyPair();

            if(localKeyPair.isPresent()){
                logger.info("Local KeyPair correctly loaded ! Public File: {} and Private File: {}", PUBLIC_KEY_FILE_PATH, PRIVATE_KEY_FILE_PATH);
                this.keyPair = localKeyPair.get();
            }
            else {
                logger.warn("Local KeyPair not available or existing ! Generating a new KeyPair ....");
                generateNewKeyPair();
            }
        }
        else
            throw new EdgeApplicationAuthenticatorException("OpenNess Controller Endpoint = null !");
    }

    private void generateNewKeyPair() throws EdgeApplicationAuthenticatorException {

        try{

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            KeyPair pair = keyGen.generateKeyPair();

            PrivateKey priv = pair.getPrivate();
            PublicKey pub = pair.getPublic();

            writePemFile(priv, "PRIVATE KEY", PRIVATE_KEY_FILE_PATH);
            writePemFile(pub, "PUBLIC KEY", PUBLIC_KEY_FILE_PATH);

            this.keyPair = pair;

        }catch (Exception e){
            String errorMsg = String.format("Error Generating New KeyPair %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    private static void writePemFile(Key key, String description, String filename) throws FileNotFoundException, IOException {

        PemFileManager pemFileManager = new PemFileManager(key, description);
        pemFileManager.write(filename);

        logger.info(String.format("%s successfully writen in file %s.", description, filename));
    }

    private Optional<KeyPair> readLocalKeyPair(){

        try{

            PrivateKey privateKey = PemFileManager.loadPrivateKey(PRIVATE_KEY_FILE_PATH);
            PublicKey publicKey = PemFileManager.loadPublicKey(PUBLIC_KEY_FILE_PATH);

            if(privateKey != null && publicKey != null){
                //logger.debug("Read Private: {}", privateKey.getEncoded());
                //logger.debug("Read Public: {}", publicKey.getEncoded());
                return Optional.of(new KeyPair(publicKey, privateKey));
            }
            else {
                logger.error("Error Loading existing KeyPair ! {} and/or {} are null", PRIVATE_KEY_FILE_PATH, PUBLIC_KEY_FILE_PATH);
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

    public void authenticateApplication(String applicationId, String organizationName) throws EdgeApplicationAuthenticatorException {

        if(this.keyPair == null)
            throw new EdgeApplicationAuthenticatorException("KeyPair not avaible ! Check EdgeApplicationAuthenticator init procedure ...");

        if(httpClient == null)
            throw new EdgeApplicationAuthenticatorException("Error ! HTTP Client = Null !");

        Optional<String> csrString = generateCertificateSigningRequest(organizationName, applicationId);

        if(!csrString.isPresent())
            throw new EdgeApplicationAuthenticatorException("Error Generating Certificate Signing Request !");

        //Create ApplicationAuthenticationRequest
        ApplicationAuthenticationRequest applicationAuthenticationRequest = new ApplicationAuthenticationRequest();
        applicationAuthenticationRequest.setCertificateSigningRequest(csrString.get());

        try{

            String jsonBody = objectMapper.writeValueAsString(applicationAuthenticationRequest);

            logger.debug("OpenNess Auth JsonBody: {}", jsonBody);

            //Create JSON Body
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonBody);

            //Create POST Request
            Request request = new Request.Builder()
                    .url(OPENNESS_CONTROLLER_BASE_URL)
                    .post(body)
                    .build();

            Call call = this.httpClient.newCall(request);
            Response response = call.execute();

            if(response != null ){

                String bodyString = response.body().string();

                logger.info("Application Authentication Response Code: {}", response.code());
                logger.info("Response Body: {}", bodyString);

                handleAuthenticationResponse(generateCertificateFileIdString(applicationId, organizationName), bodyString);

                /*
                String body = response.body().string();

                logger.info("Received Response Code: {} -> Body: {}", response.code(), body);

                DataStoreResponse<DataStoreWeatherStationResult> dataStoreResult = new ObjectMapper()
                        .readerFor(new TypeReference<DataStoreResponse<DataStoreWeatherStationResult>>() {})
                        .readValue(body);

                dataStoreResult.getResult().getWeatherStationList().forEach(weatherStationRecord -> logger.info("Weather Station: {}", weatherStationRecord));
                */
            }
            else
                logger.error("NULL Response Received for request: {}", request);

        }catch (Exception e){
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }
    }

    private void handleAuthenticationResponse(String certificateUuid, String responseBody) throws EdgeApplicationAuthenticatorException {

        try{

            ApplicationAuthenticationResponse applicationAuthenticationResponse = objectMapper.readValue(responseBody, ApplicationAuthenticationResponse.class);

            //PemFileManager.pemExport(String.format("%s%s.crt", CERT_BASE_FOLDER, applicationAuthenticationResponse.getApplicationId()), PemFileManager.PEM_TYPE_CERTIFICATE, applicationAuthenticationResponse.getCertificate().getBytes());
            //PemFileManager.pemExport(String.format("%s%s_ca_chain.crt", CERT_BASE_FOLDER, applicationAuthenticationResponse.getApplicationId()), PemFileManager.PEM_TYPE_CERTIFICATE, applicationAuthenticationResponse.getCaChain().getBytes());
            //PemFileManager.pemExport(String.format("%s%s_ca_pool.crt", CERT_BASE_FOLDER, applicationAuthenticationResponse.getApplicationId()), PemFileManager.PEM_TYPE_CERTIFICATE, applicationAuthenticationResponse.getCaPool().getBytes());

            //Save Certificate
            writeCertificateOnFile(String.format("%s%s.crt", CERT_BASE_FOLDER, certificateUuid), applicationAuthenticationResponse.getCertificate());

            //Save CA Chain files
            StringBuffer certChainStringBuffer = new StringBuffer();
            for(String caCertString : applicationAuthenticationResponse.getCaChainList())
                certChainStringBuffer.append(caCertString);

            //Save CA Chain Certificate
            writeCertificateOnFile(String.format("%s%s_ca_chain.crt", CERT_BASE_FOLDER, certificateUuid), certChainStringBuffer.toString());

            //Save CA Pool files
            certChainStringBuffer = new StringBuffer();
            for(String caCertString : applicationAuthenticationResponse.getCaPoolList())
                certChainStringBuffer.append(caCertString);

            //Save CA Chain Certificate
            writeCertificateOnFile(String.format("%s%s_ca_pool.crt", CERT_BASE_FOLDER, certificateUuid), certChainStringBuffer.toString());

        } catch (Exception e){
            String errorMsg = String.format("Error Authenticating Application ! Error: %s", e.getLocalizedMessage());
            logger.error(errorMsg);
            throw new EdgeApplicationAuthenticatorException(errorMsg);
        }

    }

    private String generateCertificateFileIdString(String applicationId, String organizationName) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(String.format("%s%s", applicationId, organizationName).getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
        //return UUID.fromString(String.format("%s%s", applicationId, organizationName)).toString();
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

    public static void main(String[] args) {

        try {

            logger.info("Testing EdgeApplicationAuthenticator ....");

            EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator();
            edgeApplicationAuthenticator.init(OPENNESS_CONTROLLER_BASE_URL);
            edgeApplicationAuthenticator.authenticateApplication("authTestConnector", "DIPI-UniMore");

        } catch (EdgeApplicationAuthenticatorException e) {
            e.printStackTrace();
        }
    }
}
