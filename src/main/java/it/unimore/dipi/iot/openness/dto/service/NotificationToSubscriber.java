package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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

    @JsonProperty("producer")
    private EdgeApplicationServiceUrn producer;

    public NotificationToSubscriber() {
    }

    public NotificationToSubscriber(final String name, final String version, final NotificationPayload payload, final EdgeApplicationServiceUrn producer) {
        this.name = name;
        this.version = version;
        this.payload = payload;
        this.producer = producer;
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

    public EdgeApplicationServiceUrn getProducer() {
        return producer;
    }

    public void setProducer(final EdgeApplicationServiceUrn producer) {
        this.producer = producer;
    }

    @Override
    public String toString() {
        return "NotificationToSubscriber{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", payload=" + payload +
                ", urn=" + producer +
                '}';
    }

}
