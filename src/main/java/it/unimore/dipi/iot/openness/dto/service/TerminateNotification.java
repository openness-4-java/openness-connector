package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private JsonNode payload;

    public TerminateNotification() {
        this.name = "terminate";
        this.version = "1.0.0";
        final ObjectMapper om = new ObjectMapper();
        this.payload = om.valueToTree("");
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

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(final JsonNode payload) {
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
