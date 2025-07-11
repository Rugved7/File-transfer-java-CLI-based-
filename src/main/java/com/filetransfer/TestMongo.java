package com.filetransfer;

import com.filetransfer.MongoConnection;
import com.mongodb.client.MongoDatabase;

public class TestMongo {
    public static void main(String[] args) {
        MongoDatabase db = MongoConnection.getDatabase("filetransfer");
        System.out.println("Connected to DB: " + db.getName());
    }
}
