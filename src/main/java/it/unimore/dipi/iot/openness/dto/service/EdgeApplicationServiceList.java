package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 17:06
 */
public class EdgeApplicationServiceList {

    @JsonProperty("services")
    private List<EdgeApplicationServiceDescriptor> serviceList;

    public EdgeApplicationServiceList() {
    }

    public EdgeApplicationServiceList(List<EdgeApplicationServiceDescriptor> serviceList) {
        this.serviceList = serviceList;
    }

    public List<EdgeApplicationServiceDescriptor> getServiceList() {
        return serviceList;
    }

    public void setServiceList(List<EdgeApplicationServiceDescriptor> serviceList) {
        this.serviceList = serviceList;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EdgeApplicationServiceList{");
        sb.append("serviceList=").append(serviceList);
        sb.append('}');
        return sb.toString();
    }
}
