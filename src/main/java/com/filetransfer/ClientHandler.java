package com.filetransfer;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            // Receive login credentials
            String username = dis.readUTF();
            String password = dis.readUTF();

            // Check credentials
            if (!isAuthenticated(username, password)) {
                dos.writeUTF("AUTH_FAIL");
                System.out.println("❌ Authentication failed for: " + username);
                return;
            }

            dos.writeUTF("AUTH_SUCCESS");
            System.out.println("✅ Authenticated: " + username);

            // Receive file name and size
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            // Receive file content
            try (FileOutputStream fos = new FileOutputStream("received_" + fileName)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;

                while (totalRead < fileSize &&
                        (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }

            System.out.println("📥 File received and saved as: received_" + fileName);
            logUpload(username, fileName, fileSize);

        } catch (IOException e) {
            System.err.println("❌ Handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private boolean isAuthenticated(String username, String password) {
        try {
            MongoDatabase db = MongoConnection.getDatabase("filetransfer");
            MongoCollection<Document> users = db.getCollection("users");

            Document user = users.find(
                    new Document("username", username).append("password", password)
            ).first();

            return user != null;
        } catch (Exception e) {
            System.err.println("❌ MongoDB auth error: " + e.getMessage());
            return false;
        }
    }

    private void logUpload(String username, String fileName, long size) {
        try (FileWriter fw = new FileWriter("upload_log.csv", true)) {
            fw.write(username + "," + fileName + "," + size + "," + LocalDateTime.now() + "\n");
        } catch (IOException e) {
            System.err.println("❌ Failed to log upload: " + e.getMessage());
        }
    }
}
