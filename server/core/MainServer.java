package core;

import client.ClientHandler;
import config.ServerConfig;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Main server entry point
 * Accepts connections and spawns client handlers
 */
public class MainServer {
    private static final Semaphore clientSlots = new Semaphore(ServerConfig.MAX_CLIENTS);
    
    public static void main(String[] args) {
        printBanner();
        ServerState.init();
        setupShutdownHook();
        startServer();
    }
    
    /**
     * Prints server startup banner
     */
    private static void printBanner() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("        Synq Secure Chat Server");
        System.out.println("═══════════════════════════════════════");
        System.out.println("  Port:        " + ServerConfig.PORT);
        System.out.println("  Max Clients: " + ServerConfig.MAX_CLIENTS);
        System.out.println("  Encryption:  AES-GCM + DH Key Exchange");
        System.out.println("═══════════════════════════════════════\n");
    }
    
    /**
     * Registers shutdown hook for clean exit
     */
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\n═══════════════════════════════════════");
            System.out.println("        Server Shutdown Initiated");
            System.out.println("═══════════════════════════════════════");
            ServerState.shutdown();
            System.out.println("═══════════════════════════════════════");
            System.out.println("        Shutdown Complete");
            System.out.println("═══════════════════════════════════════\n");
        }));
    }
    
    /**
     * Starts server and accepts connections
     */
    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(ServerConfig.PORT)) {
            System.out.println("✓ Server started successfully!\n");
            System.out.println("Listening for connections...\n");
            
            acceptConnections(serverSocket);
            
        } catch (Exception e) {
            System.err.println("✗ Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main connection acceptance loop
     */
    private static void acceptConnections(ServerSocket serverSocket) {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Try to acquire connection slot
                if (clientSlots.tryAcquire()) {
                    // Slot available - accept client
                    System.out.println("→ Accepting connection from: " + 
                        clientSocket.getInetAddress() + 
                        " [" + (ServerConfig.MAX_CLIENTS - clientSlots.availablePermits()) + 
                        "/" + ServerConfig.MAX_CLIENTS + "]");
                    
                    ClientHandler handler = new ClientHandler(clientSocket, clientSlots);
                    handler.start();
                    
                } else {
                    // Server full - reject
                    System.out.println("✗ Connection rejected (server full): " + 
                        clientSocket.getInetAddress());
                    clientSocket.close();
                }
                
            } catch (Exception e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets current server status
     */
    public static String getStatus() {
        return String.format(
            "Users: %d | Binds: %d | Slots: %d/%d",
            ServerState.getUserCount(),
            ServerState.getActiveBindCount(),
            (ServerConfig.MAX_CLIENTS - clientSlots.availablePermits()),
            ServerConfig.MAX_CLIENTS
        );
    }
}
