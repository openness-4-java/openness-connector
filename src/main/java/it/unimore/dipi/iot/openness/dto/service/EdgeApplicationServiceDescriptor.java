package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("notifications")
    private List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList;

    public EdgeApplicationServiceDescriptor() {
    }

    public EdgeApplicationServiceDescriptor(EdgeApplicationServiceUrn serviceUrn, String description, String endpointUri, List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList) {
        this.serviceUrn = serviceUrn;
        this.description = description;
        this.endpointUri = endpointUri;
        this.notificationDescriptorList = notificationDescriptorList;
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

    public List<EdgeApplicationServiceNotificationDescriptor> getNotificationDescriptorList() {
        return notificationDescriptorList;
    }

    public void setNotificationDescriptorList(List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList) {
        this.notificationDescriptorList = notificationDescriptorList;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EdgeApplicationServiceDescriptor{");
        sb.append("serviceUrn=").append(serviceUrn);
        sb.append(", description='").append(description).append('\'');
        sb.append(", endpointUri='").append(endpointUri).append('\'');
        sb.append(", notificationDescriptorList=").append(notificationDescriptorList);
        sb.append('}');
        return sb.toString();
    }
}
