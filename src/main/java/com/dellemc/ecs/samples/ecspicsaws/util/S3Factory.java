package com.dellemc.ecs.samples.ecspicsaws.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ClientOptions;
import com.dellemc.ecs.samples.ecspicsaws.model.EcsConfiguration;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Generally, the S3 client instances handle things like connection pooling and can be expensive to create.  Therefore,
 * when possible you should make them singletons.  For many applications, there is only one set of credentials and
 * security is enforced by the application.  Here, since users are setting the credentials in a web page, we can't
 * assume only one configuration, but we'll try to cache them.
 */
public class S3Factory {
    private static S3Factory ourInstance = new S3Factory();

    public static S3Factory getInstance() {
        return ourInstance;
    }
    private Map<EcsConfiguration, AmazonS3> clients;

    private S3Factory() {
        clients = new WeakHashMap<>();
    }

    public AmazonS3 getClient(EcsConfiguration config) {
        synchronized (clients) {
            if(clients.containsKey(config)) {
                return clients.get(config);
            }
        }

        // Static S3 signing credentials go here.
        BasicAWSCredentials creds = new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey());

        // Endpoint configuration.  Since ECS doesn't use AWS's regions, we always use the default us-east-1 region
        // to sign requests for v4 auth.
        AwsClientBuilder.EndpointConfiguration ec =
                new AwsClientBuilder.EndpointConfiguration(config.getEndpoint(),"us-east-1");

        // Construct the client.
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withEndpointConfiguration(ec)
                // Path-style access is preferred for ECS since it doesn't require wildcard
                // certificates and virtual host names.
                .withPathStyleAccessEnabled(true)
                .build();

        // cache it
        synchronized (clients) {
            clients.put(config, client);
        }
        return client;
    }
}
