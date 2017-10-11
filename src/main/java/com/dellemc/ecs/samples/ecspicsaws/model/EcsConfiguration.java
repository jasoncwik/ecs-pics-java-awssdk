package com.dellemc.ecs.samples.ecspicsaws.model;

public class EcsConfiguration {
    private static EcsConfiguration config;

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    public void setConfiguration(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
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

    public String getBucketName() {
        return bucketName;
    }

    public void fromCookie(String configCookie) {
        String[] parts = configCookie.split("\\|");
        if(parts.length == 4) {
            setConfiguration(parts[0], parts[1], parts[2], parts[3]);
        }
    }

    public String toCookie() {
        return endpoint + "|" + accessKey + "|" + secretKey + "|" + bucketName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        EcsConfiguration that = (EcsConfiguration) o;

        if (!endpoint.equals(that.endpoint)) return false;
        if (!accessKey.equals(that.accessKey)) return false;
        return secretKey.equals(that.secretKey);
    }

    @Override
    public int hashCode() {
        int result = endpoint.hashCode();
        result = 31 * result + accessKey.hashCode();
        result = 31 * result + secretKey.hashCode();
        return result;
    }
}
