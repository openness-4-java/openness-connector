package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 15:24
 */
public class ServiceInfo {

    @JsonProperty("info")
    private String info;

    public ServiceInfo() {
    }

    public ServiceInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "info='" + info + '\'' +
                '}';
    }

}
