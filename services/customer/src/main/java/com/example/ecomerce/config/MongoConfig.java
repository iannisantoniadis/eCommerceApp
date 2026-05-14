package com.example.ecomerce.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.mongodb.database}")
    private String database;

    @Value("${spring.mongodb.username}")
    private String username;

    @Value("${spring.mongodb.password}")
    private String password;

    @Value("${spring.mongodb.host}")
    private String host;

    @Value("${spring.mongodb.port}")
    private int port;

    @Value("${spring.mongodb.authentication-database}")
    private String authDatabase;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient mongoClient() {
        String uri = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                username, password, host, port, database, authDatabase
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToConnectionPoolSettings(pool -> pool
                        .maxSize(20)
                        .minSize(5)
                )
                .build();

        return MongoClients.create(settings);
    }
}