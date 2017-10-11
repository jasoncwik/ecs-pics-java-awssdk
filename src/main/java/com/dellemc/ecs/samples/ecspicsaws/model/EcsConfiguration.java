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
}
