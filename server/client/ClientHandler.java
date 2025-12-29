package client;

import config.ServerConfig;
import core.ServerState;
import protocol.BindManager;
import protocol.MessageHandler;
import protocol.MessageRouter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Handles individual client connection lifecycle
 * Runs in separate thread per client
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final Semaphore clientSlot;
    private ClientConnection client;
    private ClientSession session;
    private MessageHandler messageHandler;
    
    public ClientHandler(Socket socket, Semaphore clientSlot) {
        this.socket = socket;
        this.clientSlot = clientSlot;
    }
    
    @Override
    public void run() {
        try {
            // Setup connection
            setupConnection();
            
            // Login phase
            session.performLogin();
            
            // Key exchange phase
            session.performKeyExchange();
            
            // Message loop
            messageLoop();
            
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * Initializes connection and sets timeout
     */
    private void setupConnection() throws IOException {
        socket.setSoTimeout(ServerConfig.SOCKET_TIMEOUT_MS);
        client = new ClientConnection(socket);
        session = new ClientSession(client);
        messageHandler = new MessageHandler(client, session);
        
        System.out.println("→ Connection from: " + socket.getInetAddress());
    }
    
    /**
     * Main message processing loop
     */
    private void messageLoop() throws IOException {
        System.out.println("✓ Ready: " + client.username);
        
        while (true) {
            String encryptedLine = client.in.readLine();
            
            // Check for disconnect
            if (encryptedLine == null) {
                System.out.println("← Disconnect: " + client.username);
                break;
            }
            
            // Process message
            messageHandler.handleEncryptedMessage(encryptedLine);
        }
    }
    
    /**
     * Cleans up connection and notifies partner
     */
    private void cleanup() {
        try {
            if (client != null && client.isAuthenticated()) {
                String username = client.username;
                
                // Remove from user list
                ServerState.users.remove(username);
                
                // Handle unbinding
                String partner = BindManager.unbindUser(username);
                if (partner != null) {
                    MessageRouter.notifyPartnerDisconnected(username, partner);
                }
                
                System.out.println("✗ Cleanup: " + username);
            }
            
            // Close connection
            if (client != null) {
                client.close();
            }
            
        } catch (Exception e) {
            System.err.println("Cleanup error: " + e.getMessage());
        } finally {
            // Release connection slot
            if (clientSlot != null) {
                clientSlot.release();
            }
        }
    }
}
