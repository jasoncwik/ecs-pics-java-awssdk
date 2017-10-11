package com.dellemc.ecs.samples.ecspicsaws.model;

public class EcsConfiguration {
    private static EcsConfiguration config;

    private String endpoint;
    private String accessKey;
    private String secretKey;

    public void setConfiguration(String endpoint, String accessKey, String secretKey) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EcsConfiguration that = (EcsConfiguration) o;

        if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null) return false;
        if (accessKey != null ? !accessKey.equals(that.accessKey) : that.accessKey != null) return false;
        return secretKey != null ? secretKey.equals(that.secretKey) : that.secretKey == null;
    }

    @Override
    public int hashCode() {
        int result = endpoint != null ? endpoint.hashCode() : 0;
        result = 31 * result + (accessKey != null ? accessKey.hashCode() : 0);
        result = 31 * result + (secretKey != null ? secretKey.hashCode() : 0);
        return result;
    }

    public void fromCookie(String configCookie) {
        String[] parts = configCookie.split("\\|");
        setConfiguration(parts[0], parts[1], parts[2]);
    }

    public String toCookie() {
        return endpoint + "|" + accessKey + "|" + secretKey;
    }
}
