package it.unimore.dipi.iot.openness.config;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 12:28
 */
public class AuthorizedApplicationConfiguration {

    private String applicationUniqueIdentifier;

    private String applicationId;

    private String organizationName;

    private String trustStoreFilePath;

    private String keyStoreFilePath;

    private String authenticationControllerEndpoint;

    private String storePassword;

    public AuthorizedApplicationConfiguration() {
    }

    public AuthorizedApplicationConfiguration(String applicationUniqueIdentifier, String applicationId, String organizationName, String trustStoreFilePath, String keyStoreFilePath, String authenticationControllerEndpoint, String storePassword) {
        this.applicationUniqueIdentifier = applicationUniqueIdentifier;
        this.applicationId = applicationId;
        this.organizationName = organizationName;
        this.trustStoreFilePath = trustStoreFilePath;
        this.keyStoreFilePath = keyStoreFilePath;
        this.authenticationControllerEndpoint = authenticationControllerEndpoint;
        this.storePassword = storePassword;
    }

    public String getApplicationUniqueIdentifier() {
        return applicationUniqueIdentifier;
    }

    public void setApplicationUniqueIdentifier(String applicationUniqueIdentifier) {
        this.applicationUniqueIdentifier = applicationUniqueIdentifier;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getTrustStoreFilePath() {
        return trustStoreFilePath;
    }

    public void setTrustStoreFilePath(String trustStoreFilePath) {
        this.trustStoreFilePath = trustStoreFilePath;
    }

    public String getKeyStoreFilePath() {
        return keyStoreFilePath;
    }

    public void setKeyStoreFilePath(String keyStoreFilePath) {
        this.keyStoreFilePath = keyStoreFilePath;
    }

    public String getAuthenticationControllerEndpoint() {
        return authenticationControllerEndpoint;
    }

    public void setAuthenticationControllerEndpoint(String authenticationControllerEndpoint) {
        this.authenticationControllerEndpoint = authenticationControllerEndpoint;
    }

    public String getStorePassword() {
        return storePassword;
    }

    public void setStorePassword(String storePassword) {
        this.storePassword = storePassword;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AuthorizedApplicationConfiguration{");
        sb.append("applicationUniqueIdentifier='").append(applicationUniqueIdentifier).append('\'');
        sb.append(", applicationId='").append(applicationId).append('\'');
        sb.append(", organizationName='").append(organizationName).append('\'');
        sb.append(", trustStoreFilePath='").append(trustStoreFilePath).append('\'');
        sb.append(", keyStoreFilePath='").append(keyStoreFilePath).append('\'');
        sb.append(", controllerEndpoint='").append(authenticationControllerEndpoint).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
