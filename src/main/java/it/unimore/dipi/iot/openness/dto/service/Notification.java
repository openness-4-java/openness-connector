package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 15:24
 */
public class Notification {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("payload")
    private NotificationPayload payload;

    public Notification() {
    }

    public Notification(final String name, final String version, final NotificationPayload payload) {
        this.name = name;
        this.version = version;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
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

    @Override
    public String toString() {
        return "Notification{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", payload=" + payload +
                '}';
    }

}
