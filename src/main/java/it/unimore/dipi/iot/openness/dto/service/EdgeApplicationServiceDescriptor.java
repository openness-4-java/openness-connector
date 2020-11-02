package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 17:03
 */
public class EdgeApplicationServiceDescriptor {

    @JsonProperty("urn")
    private EdgeApplicationServiceUrn serviceUrn;

    @JsonProperty("description")
    private String description;

    @JsonProperty("endpoint_uri")
    private String endpointUri;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notifications")
    private List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList;

    @JsonProperty("info")
    private JsonNode info;

    public EdgeApplicationServiceDescriptor() {
    }

    public EdgeApplicationServiceDescriptor(EdgeApplicationServiceUrn serviceUrn, String description, String endpointUri, final String status, List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList, final Object info) {
        this.serviceUrn = serviceUrn;
        this.description = description;
        this.endpointUri = endpointUri;
        this.status = status;
        this.notificationDescriptorList = notificationDescriptorList;
        final ObjectMapper om = new ObjectMapper();
        this.info = om.valueToTree(info);
    }

    public EdgeApplicationServiceUrn getServiceUrn() {
        return serviceUrn;
    }

    public void setServiceUrn(EdgeApplicationServiceUrn serviceUrn) {
        this.serviceUrn = serviceUrn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpointUri() {
        return endpointUri;
    }

    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<EdgeApplicationServiceNotificationDescriptor> getNotificationDescriptorList() {
        return notificationDescriptorList;
    }

    public void setNotificationDescriptorList(List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList) {
        this.notificationDescriptorList = notificationDescriptorList;
    }

    public JsonNode getInfo() {
        return info;
    }

    public void setInfo(JsonNode info) {
        this.info = info;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EdgeApplicationServiceDescriptor{");
        sb.append("serviceUrn=").append(serviceUrn);
        sb.append(", description='").append(description).append('\'');
        sb.append(", endpointUri='").append(endpointUri).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", notificationDescriptorList=").append(notificationDescriptorList);
        sb.append(", info='").append(info).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
