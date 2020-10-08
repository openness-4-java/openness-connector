package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 08/10/2020 - 10:03
 */
public class NotificationToSubscriber {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("payload")
    private NotificationPayload payload;

    @JsonProperty("urn")
    private EdgeApplicationServiceUrn urn;

    public NotificationToSubscriber() {
    }

    public NotificationToSubscriber(final String name, final String version, final NotificationPayload payload, final EdgeApplicationServiceUrn urn) {
        this.name = name;
        this.version = version;
        this.payload = payload;
        this.urn = urn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public NotificationPayload getPayload() {
        return payload;
    }

    public void setPayload(final NotificationPayload payload) {
        this.payload = payload;
    }

    public EdgeApplicationServiceUrn getUrn() {
        return urn;
    }

    public void setUrn(final EdgeApplicationServiceUrn urn) {
        this.urn = urn;
    }

    @Override
    public String toString() {
        return "NotificationToSubscriber{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", payload=" + payload +
                ", urn=" + urn +
                '}';
    }

}
