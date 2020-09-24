package it.unimore.dipi.iot.openness.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 14:50
 */
public class ApplicationAuthenticationRequest {

    @JsonProperty("csr")
    private String certificateSigningRequest = null;

    public ApplicationAuthenticationRequest() {
    }

    public ApplicationAuthenticationRequest(String certificateSigningRequest) {
        this.certificateSigningRequest = certificateSigningRequest;
    }

    public String getCertificateSigningRequest() {
        return certificateSigningRequest;
    }

    public void setCertificateSigningRequest(String certificateSigningRequest) {
        this.certificateSigningRequest = certificateSigningRequest;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ApplicationAuthenticationRequest{");
        sb.append("certificateSigningRequest='").append(certificateSigningRequest).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
