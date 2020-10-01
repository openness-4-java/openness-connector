package it.unimore.dipi.iot.openness.dto.service;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 17:01
 */
public class EdgeApplicationServiceUrn {

    private String id;

    private String namespace;

    public EdgeApplicationServiceUrn() {
    }

    public EdgeApplicationServiceUrn(String id, String namespace) {
        this.id = id;
        this.namespace = namespace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EdgeApplicationServiceUrn{");
        sb.append("id='").append(id).append('\'');
        sb.append(", namespace='").append(namespace).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
