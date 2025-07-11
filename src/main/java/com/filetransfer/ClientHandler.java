package com.filetransfer;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

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
            String username = dis.readUTF();
            String password = dis.readUTF();

            if (!isAuthenticated(username, password)) {
                dos.writeUTF("AUTH_FAIL");
                System.out.println("❌ Authentication failed for: " + username);
                return;
            }

            dos.writeUTF("AUTH_SUCCESS");
            System.out.println("✅ Authenticated: " + username);

            int action = dis.readInt(); // 1 = upload, 2 = download

            if (action == 1) {
                // Upload
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();

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

                System.out.println("📥 File received: " + fileName);
                logUploadToMongo(username, fileName, fileSize);

            } else if (action == 2) {
                // Download
                String fileName = dis.readUTF();
                File file = new File("received_" + fileName);

                if (!file.exists()) {
                    dos.writeUTF("FILE_NOT_FOUND");
                    System.out.println("❌ File not found for download: " + fileName);
                    return;
                }

                dos.writeUTF("FILE_FOUND");
                dos.writeLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("📤 File sent to client: " + fileName);
            }

        } catch (IOException e) {
            System.err.println("❌ Handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isAuthenticated(String username, String password) {
        try {
            MongoDatabase db = MongoConnection.getDatabase("filetransfer");
            MongoCollection<Document> users = db.getCollection("users");

            Document user = users.find(new Document("username", username)).first();
            if (user != null) {
                String hashed = user.getString("password");
                return BCrypt.checkpw(password, hashed);
            }
        } catch (Exception e) {
            System.err.println("❌ MongoDB auth error: " + e.getMessage());
        }
        return false;
    }

    private void logUploadToMongo(String username, String fileName, long fileSize) {
        try {
            MongoDatabase db = MongoConnection.getDatabase("filetransfer");
            MongoCollection<Document> uploads = db.getCollection("uploads");

            Document logEntry = new Document("username", username)
                    .append("fileName", fileName)
                    .append("fileSize", fileSize)
                    .append("uploadTime", LocalDateTime.now().toString());

            uploads.insertOne(logEntry);
            System.out.println("📝 Upload logged in MongoDB.");
        } catch (Exception e) {
            System.err.println("❌ Failed to log upload to MongoDB: " + e.getMessage());
        }
    }
}
