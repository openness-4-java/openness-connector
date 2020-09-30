package it.unimore.dipi.iot.openness.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 14:50
 */
public class ApplicationAuthenticationResponse {

    @JsonProperty("id")
    private String applicationId;

    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("ca_chain")
    private List<String> caChainList;

    @JsonProperty("ca_pool")
    private List<String> caPoolList;

    public ApplicationAuthenticationResponse() {
    }

    public ApplicationAuthenticationResponse(String applicationId, String certificate, List<String> caChain, List<String> caPool) {
        this.applicationId = applicationId;
        this.certificate = certificate;
        this.caChainList = caChain;
        this.caPoolList = caPool;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public List<String> getCaChainList() {
        return caChainList;
    }

    public void setCaChainList(List<String> caChainList) {
        this.caChainList = caChainList;
    }

    public List<String> getCaPoolList() {
        return caPoolList;
    }

    public void setCaPoolList(List<String> caPoolList) {
        this.caPoolList = caPoolList;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ApplicationAuthenticationResponse{");
        sb.append("applicationId='").append(applicationId).append('\'');
        sb.append(", certificate='").append(certificate).append('\'');
        sb.append(", caChainList=").append(caChainList);
        sb.append(", caPoolList=").append(caPoolList);
        sb.append('}');
        return sb.toString();
    }
}
