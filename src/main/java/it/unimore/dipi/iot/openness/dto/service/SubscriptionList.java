package it.unimore.dipi.iot.openness.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 14:03
 */
public class SubscriptionList {

    @JsonProperty("subscriptions")
    private List<Subscription> subscriptions;

    public SubscriptionList() {
    }

    public SubscriptionList(final List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(final List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public String toString() {
        return "EdgeApplicationSubscriptionList{" +
                "subscriptions=" + subscriptions +
                '}';
    }

}
