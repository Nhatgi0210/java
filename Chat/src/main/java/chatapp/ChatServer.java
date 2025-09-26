package chatapp;

import java.io.*; import java.net.*; import java.util.*; import java.util.concurrent.*;

public class ChatServer {
    private static final int MAX_FILE_BYTES = 50 * 1024 * 1024; // 50MB
    private final int port;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public ChatServer(int port) { this.port = port; }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ChatServer started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private String username = null;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override public void run() {
            try (socket) {
                in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                while (true) {
                    String line = in.readUTF();
                    if (line == null) break;
                    String[] parts = line.split(" ", 2);
                    String cmd = parts[0];
                    String payload = parts.length > 1 ? parts[1] : "";
                    switch (cmd) {
                        case Protocol.USER -> handleUser(payload);
                        case Protocol.MSG  -> handleMsg(payload);
                        case Protocol.DM   -> handleDm(payload);
                        case Protocol.FILE -> handleFileHeader(payload);
                        case Protocol.BYE  -> { disconnect(); return; }
                        default -> System.out.println("Unknown: " + line);
                    }
                }
            } catch (EOFException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private void handleUser(String name) throws IOException {
            name = name.trim();
            if (name.isEmpty() || clients.containsKey(name)) {
                sendSelf("ERROR username taken or empty");
                return;
            }
            username = name;
            clients.put(username, this);
            safeBroadcast("SYS " + username + " joined. Online: " + clients.keySet());
        }

        private void handleMsg(String text) { // broadcast
            if (username == null) return;
            safeBroadcast("MSG [" + username + "]: " + text);
        }

        private void handleDm(String payload) throws IOException {
            if (username == null) return;
            int sp = payload.indexOf(' ');
            if (sp <= 0) { sendSelf("SYS Usage: DM <username> <text>"); return; }
            String target = payload.substring(0, sp).trim();
            String text   = payload.substring(sp + 1);
            ClientHandler dest = clients.get(target);
            if (dest == null) {
                sendSelf("SYS User not found: " + target);
                return;
            }
            safeSendText(dest, "DM [" + username + " → you]: " + text);
            sendSelf("DM [you → " + target + "]: " + text);
        }

        private void handleFileHeader(String header) throws IOException {
            if (username == null) return;
            int lastSpace = header.lastIndexOf(' ');
            if (lastSpace <= 0) return;
            String sizeStr = header.substring(lastSpace + 1).trim();
            String head2   = header.substring(0, lastSpace);
            int firstSpace = head2.indexOf(' ');
            if (firstSpace <= 0) return;
            String target   = head2.substring(0, firstSpace).trim();
            String filename = head2.substring(firstSpace + 1).trim();

            int size;
            try { size = Integer.parseInt(sizeStr); }
            catch (NumberFormatException nfe) { sendSelf("SYS Invalid file header (size): " + sizeStr); return; }

            if (size < 0 || size > MAX_FILE_BYTES) {
                sendSelf("SYS File rejected: size " + size + " bytes exceeds limit " + MAX_FILE_BYTES);
                in.readNBytes(Math.max(0, size));
                return;
            }

            byte[] data = in.readNBytes(size);

            if ("ALL".equalsIgnoreCase(target)) {
                int sent = 0;
                for (String name : new ArrayList<>(clients.keySet())) {
                    ClientHandler ch = clients.get(name);
                    if (ch == null || ch == this) continue;
                    if (safeSendFile(ch, username, filename, data)) sent++;
                }
                sendSelf("SYS File '" + filename + "' sent to ALL (" + sent + " clients).");
            } else {
                ClientHandler dest = clients.get(target);
                if (dest == null) {
                    sendSelf("SYS User not found: " + target);
                } else if (safeSendFile(dest, username, filename, data)) {
                    sendSelf("SYS File '" + filename + "' sent to " + target);
                } else {
                    sendSelf("SYS Failed to deliver to " + target + " (disconnected).");
                }
            }
        }

        private void sendSelf(String s) throws IOException { out.writeUTF(s); out.flush(); }
        private boolean safeSendText(ClientHandler ch, String msg) {
            try { ch.out.writeUTF(msg); ch.out.flush(); return true; }
            catch (IOException e) { clients.values().removeIf(v -> v == ch); try { ch.socket.close(); } catch (IOException ignored) {} return false; }
        }
        private boolean safeSendFile(ClientHandler ch, String from, String filename, byte[] data) {
            try { ch.out.writeUTF(Protocol.FILE + " " + from + " " + filename + " " + data.length); ch.out.flush(); ch.out.write(data); ch.out.flush(); return true; }
            catch (IOException e) { clients.values().removeIf(v -> v == ch); try { ch.socket.close(); } catch (IOException ignored) {} return false; }
        }
        private void safeBroadcast(String msg) {
            for (String name : new ArrayList<>(clients.keySet())) {
                ClientHandler ch = clients.get(name);
                if (ch == null) continue;
                if (!safeSendText(ch, msg)) { /* removed */ }
            }
        }
        private void disconnect() {
            if (username != null) {
                clients.remove(username);
                safeBroadcast("SYS " + username + " left. Online: " + clients.keySet());
            }
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 5000;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        new ChatServer(port).start();
    }
}
