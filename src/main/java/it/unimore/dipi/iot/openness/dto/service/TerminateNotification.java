package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 14/10/2020 - 12:31
 */
public class TerminateNotification {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("payload")
    private NotificationPayload payload;

    public TerminateNotification() {
        this.name = "terminate";
        this.version = "1.0.0";
        this.payload = new NotificationPayload("");
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
        return "TerminateNotifcation{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }

}
