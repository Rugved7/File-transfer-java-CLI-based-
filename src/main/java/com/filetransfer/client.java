package com.filetransfer;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

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
            sc.nextLine();

            System.out.print("Enter username: ");
            String username = sc.nextLine();

            System.out.print("Enter password: ");
            String password = sc.nextLine();

            if (choice == 2) {
                registerUser(username, password);
                return;
            }

            dos.writeUTF(username);
            dos.writeUTF(password);

            System.out.println("🔁 Waiting for server response...");
            String response = dis.readUTF();

            if (!"AUTH_SUCCESS".equals(response)) {
                System.out.println("❌ Authentication failed. Please check credentials.");
                return;
            }

            System.out.println("✅ Authentication successful!");

            System.out.println("What would you like to do?");
            System.out.println("1. Upload a file");
            System.out.println("2. Download a file");
            System.out.print("Enter choice: ");
            int action = sc.nextInt();
            sc.nextLine();

            dos.writeInt(action); // Send action to server

            if (action == 1) {
                // Upload
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
            } else if (action == 2) {
                // Download
                System.out.print("Enter filename to download: ");
                String fileName = sc.nextLine();
                dos.writeUTF(fileName);

                String serverResp = dis.readUTF();
                if ("FILE_NOT_FOUND".equals(serverResp)) {
                    System.out.println("❌ File not found on server.");
                    return;
                }

                long fileSize = dis.readLong();
                try (FileOutputStream fos = new FileOutputStream("downloaded_" + fileName)) {
                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int bytesRead;

                    while (totalRead < fileSize &&
                            (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                }

                System.out.println("📥 File downloaded as: downloaded_" + fileName);
            }

        } catch (IOException e) {
            e.printStackTrace();
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

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            Document user = new Document("username", username).append("password", hashedPassword);
            users.insertOne(user);
            System.out.println("✅ User registered successfully!");
        } catch (Exception e) {
            System.err.println("❌ Registration failed: " + e.getMessage());
        }
    }
}
