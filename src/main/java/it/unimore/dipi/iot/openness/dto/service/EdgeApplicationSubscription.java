package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 14:03
 */
public class EdgeApplicationSubscription {

    @JsonProperty("urn")
    private EdgeApplicationServiceUrn urn;

    @JsonProperty("notifications")
    private List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList;

    public EdgeApplicationSubscription() { }

    public EdgeApplicationSubscription(final EdgeApplicationServiceUrn urn, final List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList) {
        this.urn = urn;
        this.notificationDescriptorList = notificationDescriptorList;
    }

    public EdgeApplicationServiceUrn getUrn() {
        return urn;
    }

    public void setUrn(final EdgeApplicationServiceUrn urn) {
        this.urn = urn;
    }

    public List<EdgeApplicationServiceNotificationDescriptor> getNotificationDescriptorList() {
        return notificationDescriptorList;
    }

    public void setNotificationDescriptorList(final List<EdgeApplicationServiceNotificationDescriptor> notificationDescriptorList) {
        this.notificationDescriptorList = notificationDescriptorList;
    }

    @Override
    public String toString() {
        return "EdgeApplicationSubscription{" +
                "urn=" + urn +
                ", notificationDescriptorList=" + notificationDescriptorList +
                '}';
    }

}
