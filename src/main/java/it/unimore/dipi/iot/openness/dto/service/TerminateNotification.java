package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * "Poison pill" to close the websocket channel between Openness producer and consumer:
 *  (1) the producer builds the poison pill
 *  (2) sends the pill through the websocket channel
 *  (3) the consumer handle reacts by closing the connection
 *
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

    /**
     * Builds the poison pill:
     *  - name = terminate
     *  - version = 1.0.0
     *  - payload = ""
     */
    public TerminateNotification() {
        this.name = "terminate";
        this.version = "1.0.0";
        final ObjectMapper om = new ObjectMapper();
        this.payload = om.valueToTree("");
    }

    /**
     * Gets the name of the notification (always "terminate")
     *
     * @return the name of the notification
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the version of the notification (always "1.0.0")
     *
     * @return the version of the notification
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the payload of the notification (always "")
     *
     * @return the payload of the notification
     */
    public JsonNode getPayload() {
        return payload;
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
