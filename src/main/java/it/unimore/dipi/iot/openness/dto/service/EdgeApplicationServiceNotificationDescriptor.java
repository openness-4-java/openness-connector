package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 17:00
 */
public class EdgeApplicationServiceNotificationDescriptor {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("description")
    private String description;

    public EdgeApplicationServiceNotificationDescriptor() {
    }

    public EdgeApplicationServiceNotificationDescriptor(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public static EdgeApplicationServiceNotificationDescriptor defaultTerminateDescriptor() {
        return new EdgeApplicationServiceNotificationDescriptor(
                "terminate",
                "1.0.0",
                "To get termination requests");
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

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EdgeApplicationServiceNotificationDescriptor{");
        sb.append("name='").append(name).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
