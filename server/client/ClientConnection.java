package client;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;

/**
 * Represents a connected client
 * Wraps socket, streams, and encryption key
 */
public class ClientConnection {
    public final Socket socket;
    public final BufferedReader in;
    public final PrintWriter out;
    
    public SecretKey aesKey;
    public String username;
    
    /**
     * Creates connection wrapper from socket
     */
    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }
    
    /**
     * Closes all resources
     */
    public void close() {
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            System.err.println("Error closing input stream: " + e.getMessage());
        }
        
        try {
            if (out != null) out.close();
        } catch (Exception e) {
            System.err.println("Error closing output stream: " + e.getMessage());
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    
    /**
     * Checks if connection is secure (AES key established)
     */
    public boolean isSecure() {
        return aesKey != null;
    }
    
    /**
     * Checks if user is authenticated
     */
    public boolean isAuthenticated() {
        return username != null;
    }
    
    @Override
    public String toString() {
        return username != null ? username : socket.getInetAddress().toString();
    }
}
