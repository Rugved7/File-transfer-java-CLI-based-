package com.filetransfer;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class client {
    public static void main(String[] args) {
        String server = "localhost";
        int port = 5000;

        try (
                Socket socket = new Socket(server, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                Scanner sc = new Scanner(System.in)
        ) {
            System.out.print("Do you want to (1) Login or (2) Signup? Enter 1 or 2: ");
            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            System.out.print("Enter username: ");
            String username = sc.nextLine();

            System.out.print("Enter password: ");
            String password = sc.nextLine();

            if (choice == 2) {
                registerUser(username, password);
                return; // Exit after registration
            }

            // Send login credentials to server
            dos.writeUTF(username);
            dos.writeUTF(password);

            System.out.println("🔁 Waiting for server response...");
            String response = dis.readUTF();

            if (!"AUTH_SUCCESS".equals(response)) {
                System.out.println("❌ Authentication failed. Please check credentials.");
                return;
            }

            System.out.println("✅ Authentication successful!");

            System.out.print("Enter full path of the file to send: ");
            File file = new File(sc.nextLine());

            if (!file.exists()) {
                System.out.println("❌ File does not exist.");
                return;
            }

            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("📤 File sent successfully.");

        } catch (IOException e) {
            e.printStackTrace(); // Show detailed error
        }
    }

    private static void registerUser(String username, String password) {
        try {
            MongoDatabase db = MongoConnection.getDatabase("filetransfer");
            MongoCollection<Document> users = db.getCollection("users");

            Document existing = users.find(new Document("username", username)).first();
            if (existing != null) {
                System.out.println("❌ Username already exists. Try another one.");
                return;
            }

            Document user = new Document("username", username).append("password", password);
            users.insertOne(user);

            System.out.println("✅ User registered successfully!");
        } catch (Exception e) {
            System.err.println("❌ Registration failed: " + e.getMessage());
        }
    }
}
