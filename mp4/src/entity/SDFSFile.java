package entity;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class SDFSFile implements Serializable{
    private String filename;
    private int timestamp;
    private String SHA256;
    private byte[] file;
    private long fileSize;
    private long lastModificationTime;

    public SDFSFile(String filename, int timestamp, String SHA256, long fileSize, long lastModificationTime) {
        this.filename = filename;
        this.timestamp = timestamp;
        this.SHA256 = SHA256;
        this.fileSize = fileSize;
        this.lastModificationTime = lastModificationTime;
    }

    public SDFSFile(SDFSFile from, byte[] file) {
        this.filename = from.getFilename();
        this.timestamp = from.getTimestamp();
        this.SHA256 = from.getSHA256();
        this.fileSize = from.getFileSize();
        this.lastModificationTime = from.getLastModificationTime();
        this.file = file;
    }

    public SDFSFile(String filename, String localFilename, int timestamp) throws IOException {
        try {
            this.filename = filename;
            this.timestamp = timestamp;
            this.file = Files.readAllBytes(Paths.get(localFilename));
            this.fileSize = file.length;
            this.lastModificationTime = System.currentTimeMillis();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(this.file); // Change this to "UTF-16" if needed
            byte[] digest = md.digest();
            this.SHA256 = String.format("%064x", new java.math.BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(filename);
        out.writeInt(timestamp);
        out.writeObject(SHA256);
        out.writeObject(file);
        out.writeLong(fileSize);
        out.writeLong(lastModificationTime);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        filename = (String) in.readObject();
        timestamp = in.readInt();
        SHA256 = (String) in.readObject();
        file = (byte[]) in.readObject();
        fileSize = in.readLong();
        lastModificationTime = in.readLong();
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getSHA256() {
        return SHA256;
    }

    public void setSHA256(String SHA256) {
        this.SHA256 = SHA256;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getLastModificationTime() {
        return lastModificationTime;
    }

    public void setLastModificationTime(long lastModificationTime) {
        this.lastModificationTime = lastModificationTime;
    }
}
