package it.unimore.dipi.iot.openness.utils;

import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;

import java.io.File;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 14:28
 */
public class AuthenticatorFileUtils {

    public static final String CERT_BASE_FOLDER = "certs/";

    /**
     * Check and create if the target certificate folder exists or needs to be created
     * @return
     */
    public static File checkOrCreateCertificatesFolder() throws EdgeApplicationAuthenticatorException {

        try{
            File metricsFolder = new File(CERT_BASE_FOLDER);

            boolean result = false;

            if(!metricsFolder.exists()){
                result = metricsFolder.mkdir();
                if(!result)
                    throw new EdgeApplicationAuthenticatorException("Error creating target Certificate Folder ! Target Path: " + CERT_BASE_FOLDER);
            }

            return metricsFolder;

        } catch (Exception e){
            throw new EdgeApplicationAuthenticatorException(e.getLocalizedMessage());
        }
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getClientCertificateFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s.crt", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getPrivateKeyFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s_id", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getPublicKeyFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s_id.pub", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getCaChainFilePath(String applicationUniqueIdentifier) {
        return String.format("%s%s_ca_chain.crt", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getCaPoolFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s_ca_pool.crt", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getTrustStoreOutputFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s.ca.jks", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getClientChainOutputFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s.client.chain.crt", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static String getKeyStoreOutputFilePath(String applicationUniqueIdentifier){
        return String.format("%s%s.client.p12", CERT_BASE_FOLDER, applicationUniqueIdentifier);
    }

    /**
     *
     * @param applicationUniqueIdentifier
     * @return
     */
    public static boolean isJavaClientAuthenticationFilesAvailable(String applicationUniqueIdentifier){
        return isFileAvailable(getTrustStoreOutputFilePath(applicationUniqueIdentifier))
                && isFileAvailable(getKeyStoreOutputFilePath(applicationUniqueIdentifier));
    }

    /**
     *
     * @param filePathString
     * @return
     */
    private static boolean isFileAvailable(String filePathString){
        File f = new File(filePathString);
        return (f.exists() && !f.isDirectory());
    }

}
