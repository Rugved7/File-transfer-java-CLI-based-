package com.filetransfer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {
    private static final String URI = "mongodb+srv://rugved7dev:2EOIuyqZUuJAB5Em@cluster0.nmgzuyg.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"; // Replace this

    private static MongoClient mongoClient;

    static {
        try {
            mongoClient = MongoClients.create(URI);
            System.out.println("MongoDB connected successfully.");
        } catch (Exception e) {
            System.err.println("MongoDB connection failed: " + e.getMessage());
        }
    }

    public static MongoDatabase getDatabase(String dbName) {
        return mongoClient.getDatabase(dbName);
    }
}
