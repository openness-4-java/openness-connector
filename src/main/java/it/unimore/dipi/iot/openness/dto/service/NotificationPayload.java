package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 15:24
 */
public class NotificationPayload {

    @JsonProperty("payload")
    private String payload;

    public NotificationPayload() {
    }

    public NotificationPayload(final String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "NotificationPayload{" +
                "payload='" + payload + '\'' +
                '}';
    }

}
