package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 14:03
 */
public class EdgeApplicationSubscriptionList {

    @JsonProperty("subscriptions")
    private List<EdgeApplicationSubscription> subscriptions;

    public EdgeApplicationSubscriptionList() {
    }

    public EdgeApplicationSubscriptionList(final List<EdgeApplicationSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<EdgeApplicationSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(final List<EdgeApplicationSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public String toString() {
        return "EdgeApplicationSubscriptionList{" +
                "subscriptions=" + subscriptions +
                '}';
    }

}
