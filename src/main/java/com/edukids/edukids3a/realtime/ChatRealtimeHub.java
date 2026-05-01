package com.edukids.edukids3a.realtime;

import com.edukids.edukids3a.model.Message;
import com.edukids.edukids3a.model.MessageAttachment;
import com.edukids.edukids3a.service.ChatService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Petit hub WebSocket local pour diffuser les messages du chat en temps reel.
 * Le hub ecoute uniquement sur 127.0.0.1 pour rester local a l'application.
 */
public final class ChatRealtimeHub {

    public static final int DEFAULT_PORT = 17890;
    private static final String WEB_SOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Logger LOG = LoggerFactory.getLogger(ChatRealtimeHub.class);
    private static final ChatRealtimeHub INSTANCE = new ChatRealtimeHub();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Set<ClientConnection> clients = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    src == null ? com.google.gson.JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(DATE_TIME_FORMATTER.format(src)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER))
            .create();

    private volatile ServerSocket serverSocket;

    private ChatRealtimeHub() {
    }

    public static ChatRealtimeHub getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        try {
            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(loopback, DEFAULT_PORT), 50);
        } catch (IOException exception) {
            started.set(false);
            throw new IllegalStateException("Impossible de demarrer le serveur WebSocket du chat.", exception);
        }

        Thread thread = new Thread(this::acceptLoop, "edukids-chat-ws-server");
        thread.setDaemon(true);
        thread.start();
        LOG.info("Chat WebSocket hub started on ws://127.0.0.1:{}/chat", DEFAULT_PORT);
    }

    public void stop() {
        started.set(false);
        ServerSocket socket = serverSocket;
        serverSocket = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        for (ClientConnection client : clients) {
            closeQuietly(client.socket);
        }
        clients.clear();
    }

    public void ensureStarted() {
        if (!started.get()) {
            start();
        }
    }

    public void broadcastMessageSaved(Message message) {
        if (message == null || message.getConversationId() == null) {
            return;
        }

        if (!ensureBroadcastReady()) {
            return;
        }
        JsonObject event = new JsonObject();
        event.addProperty("type", "message_created");
        event.addProperty("conversationId", message.getConversationId());
        if (message.getId() != null) {
            event.addProperty("messageId", message.getId());
        }
        if (message.getSenderId() != null) {
            event.addProperty("senderId", message.getSenderId());
        }
        if (message.getContent() != null) {
            event.addProperty("content", message.getContent());
        }
        if (message.getType() != null) {
            event.addProperty("messageType", message.getType());
        }
        if (message.getCreatedAt() != null) {
            event.addProperty("createdAt", message.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (message.getFilePath() != null) {
            event.addProperty("filePath", message.getFilePath());
        }

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            MessageAttachment attachment = message.getAttachments().get(0);
            JsonObject attachmentJson = new JsonObject();
            attachmentJson.addProperty("originalName", attachment.getOriginalName());
            attachmentJson.addProperty("storedName", attachment.getStoredName());
            attachmentJson.addProperty("storagePath", attachment.getStoragePath());
            attachmentJson.addProperty("mimeType", attachment.getMimeType());
            attachmentJson.addProperty("size", attachment.getSize());
            attachmentJson.addProperty("image", attachment.isImage());
            attachmentJson.addProperty("type", attachment.getType());
            if (attachment.getDuration() != null) {
                attachmentJson.addProperty("duration", attachment.getDuration());
            }
            event.add("attachment", attachmentJson);
        }

        broadcast(event.toString());
    }

    public void broadcastError(long senderId, String message) {
        if (!ensureBroadcastReady()) {
            return;
        }
        JsonObject event = new JsonObject();
        event.addProperty("type", "error");
        event.addProperty("senderId", senderId);
        event.addProperty("message", message == null ? "Une erreur est survenue." : message);
        broadcast(event.toString());
    }

    public void broadcastMessageUpdated(Message message) {
        if (message == null || message.getConversationId() == null) {
            return;
        }

        if (!ensureBroadcastReady()) {
            return;
        }
        JsonObject event = buildMessageEvent("message_updated", message);
        broadcast(event.toString());
    }

    public void broadcastMessageDeleted(long conversationId, long messageId, long senderId) {
        if (!ensureBroadcastReady()) {
            return;
        }
        JsonObject event = new JsonObject();
        event.addProperty("type", "message_deleted");
        event.addProperty("conversationId", conversationId);
        event.addProperty("messageId", messageId);
        event.addProperty("senderId", senderId);
        broadcast(event.toString());
    }

    private void acceptLoop() {
        while (started.get()) {
            try {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> handleClient(socket), "edukids-chat-ws-client");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException exception) {
                if (started.get()) {
                    LOG.warn("WebSocket accept loop stopped unexpectedly.", exception);
                }
                break;
            }
        }
    }

    private void handleClient(Socket socket) {
        ClientConnection connection = new ClientConnection(socket);
        try (Socket autoClose = socket;
             InputStream rawInput = new BufferedInputStream(socket.getInputStream());
             OutputStream rawOutput = new BufferedOutputStream(socket.getOutputStream())) {

            handshake(rawInput, rawOutput);
            rawOutput.flush();
            connection.output = rawOutput;
            clients.add(connection);

            while (!socket.isClosed()) {
                WebSocketFrame frame = readFrame(rawInput);
                if (frame == null) {
                    break;
                }

                switch (frame.opcode) {
                    case 0x1 -> handleTextMessage(connection, frame.textPayload());
                    case 0x8 -> {
                        sendClose(rawOutput);
                        return;
                    }
                    case 0x9 -> sendPong(rawOutput, frame.payload);
                    case 0xA -> {
                        // pong, ignore
                    }
                    default -> {
                        // ignore unsupported frames
                    }
                }
            }
        } catch (Exception exception) {
            LOG.debug("WebSocket client disconnected: {}", exception.getMessage());
        } finally {
            clients.remove(connection);
            closeQuietly(socket);
        }
    }

    private void handleTextMessage(ClientConnection connection, String payload) {
        try {
            JsonObject request = JsonParser.parseString(payload).getAsJsonObject();
            String action = request.has("action") ? request.get("action").getAsString() : "";
            switch (action.toLowerCase()) {
                case "send_message" -> {
                    long conversationId = request.get("conversationId").getAsLong();
                    long senderId = request.get("senderId").getAsLong();
                    String content = request.has("content") && !request.get("content").isJsonNull()
                            ? request.get("content").getAsString()
                            : "";
                    String type = request.has("type") && !request.get("type").isJsonNull()
                            ? request.get("type").getAsString()
                            : null;
                    String filePath = request.has("filePath") && !request.get("filePath").isJsonNull()
                            ? request.get("filePath").getAsString()
                            : null;

                    MessageAttachment attachment = null;
                    if (request.has("attachment") && request.get("attachment").isJsonObject()) {
                        attachment = gson.fromJson(request.get("attachment"), MessageAttachment.class);
                    }

                    if (attachment != null) {
                        chatService().sendMessage(conversationId, senderId, content, attachment);
                    } else {
                        chatService().sendMessage(conversationId, senderId, content, type, filePath);
                    }
                }
                case "edit_message" -> {
                    long messageId = request.get("messageId").getAsLong();
                    long senderId = request.get("senderId").getAsLong();
                    String content = request.has("content") && !request.get("content").isJsonNull()
                            ? request.get("content").getAsString()
                            : "";
                    Message updated = chatService().editMessage(messageId, senderId, content);
                    broadcastMessageUpdated(updated);
                }
                case "delete_message" -> {
                    long conversationId = request.has("conversationId") ? request.get("conversationId").getAsLong() : -1L;
                    long messageId = request.get("messageId").getAsLong();
                    long senderId = request.get("senderId").getAsLong();
                    chatService().deleteMessage(messageId, senderId);
                    if (conversationId > 0) {
                        broadcastMessageDeleted(conversationId, messageId, senderId);
                    }
                }
                default -> {
                    return;
                }
            }
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "Impossible d'envoyer le message.";
            }
            try {
                sendText(connection.output, gson.toJson(errorPayload(message)));
            } catch (IOException ignored) {
            }
            LOG.warn("WebSocket chat request failed: {}", message);
        }
    }

    private JsonObject buildMessageEvent(String type, Message message) {
        JsonObject event = new JsonObject();
        event.addProperty("type", type);
        event.addProperty("conversationId", message.getConversationId());
        if (message.getId() != null) {
            event.addProperty("messageId", message.getId());
        }
        if (message.getSenderId() != null) {
            event.addProperty("senderId", message.getSenderId());
        }
        if (message.getContent() != null) {
            event.addProperty("content", message.getContent());
        }
        if (message.getType() != null) {
            event.addProperty("messageType", message.getType());
        }
        if (message.getUpdatedAt() != null) {
            event.addProperty("updatedAt", message.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (message.getFilePath() != null) {
            event.addProperty("filePath", message.getFilePath());
        }
        return event;
    }

    private ChatService chatService() {
        return ChatServiceHolder.INSTANCE;
    }

    private JsonObject errorPayload(String message) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "error");
        event.addProperty("message", message);
        return event;
    }

    private void broadcast(String text) {
        byte[] frame = encodeTextFrame(text);
        for (ClientConnection client : clients) {
            try {
                sendRaw(client.output, frame);
            } catch (IOException exception) {
                clients.remove(client);
                closeQuietly(client.socket);
            }
        }
    }

    private void handshake(InputStream input, OutputStream output) throws IOException, NoSuchAlgorithmException {
        String headers = readHttpHeaders(input);
        String[] lines = headers.split("\r\n");
        String key = null;
        String path = "/";

        if (lines.length > 0) {
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length >= 2) {
                path = requestLine[1];
            }
        }

        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if ("Sec-WebSocket-Key".equalsIgnoreCase(name)) {
                    key = value;
                }
            }
        }

        if (key == null || !path.startsWith("/chat")) {
            throw new IOException("Handshake WebSocket invalide.");
        }

        String accept = acceptKey(key);
        String response = """
                HTTP/1.1 101 Switching Protocols\r
                Upgrade: websocket\r
                Connection: Upgrade\r
                Sec-WebSocket-Accept: %s\r
                \r
                """.formatted(accept);
        output.write(response.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String readHttpHeaders(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        int matches = 0;
        while ((current = input.read()) != -1) {
            buffer.write(current);
            if (matches == 0 && current == '\r' && previous != '\r') {
                matches = 1;
            } else if (matches == 1 && current == '\n') {
                matches = 2;
            } else if (matches == 2 && current == '\r') {
                matches = 3;
            } else if (matches == 3 && current == '\n') {
                break;
            } else {
                matches = 0;
            }
            previous = current;
        }
        return buffer.toString(StandardCharsets.ISO_8859_1);
    }

    private WebSocketFrame readFrame(InputStream input) throws IOException {
        int first = input.read();
        if (first < 0) {
            return null;
        }
        int second = input.read();
        if (second < 0) {
            return null;
        }

        boolean masked = (second & 0x80) != 0;
        long length = second & 0x7F;
        if (length == 126) {
            length = readUnsignedShort(input);
        } else if (length == 127) {
            length = readLong(input);
        }

        byte[] mask = masked ? input.readNBytes(4) : new byte[0];
        if (masked && mask.length != 4) {
            throw new EOFException("Unexpected end of stream while reading mask.");
        }

        byte[] payload = input.readNBytes((int) length);
        if (payload.length != length) {
            throw new EOFException("Unexpected end of stream while reading payload.");
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
        }

        return new WebSocketFrame(first & 0x0F, payload);
    }

    private void sendPong(OutputStream output, byte[] payload) throws IOException {
        byte[] frame = encodeFrame(0xA, payload == null ? new byte[0] : payload);
        sendRaw(output, frame);
    }

    private void sendClose(OutputStream output) throws IOException {
        sendRaw(output, encodeFrame(0x8, new byte[0]));
    }

    private void sendText(OutputStream output, String text) throws IOException {
        sendRaw(output, encodeTextFrame(text));
    }

    private void sendRaw(OutputStream output, byte[] frame) throws IOException {
        synchronized (output) {
            output.write(frame);
            output.flush();
        }
    }

    private byte[] encodeTextFrame(String text) {
        return encodeFrame(0x1, text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] encodeFrame(int opcode, byte[] payload) {
        int length = payload.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x80 | (opcode & 0x0F));
        if (length < 126) {
            out.write(length);
        } else if (length <= 0xFFFF) {
            out.write(126);
            out.write((length >>> 8) & 0xFF);
            out.write(length & 0xFF);
        } else {
            out.write(127);
            long longLength = length & 0xFFFFFFFFL;
            for (int shift = 56; shift >= 0; shift -= 8) {
                out.write((int) ((longLength >>> shift) & 0xFF));
            }
        }
        out.writeBytes(payload);
        return out.toByteArray();
    }

    private long readUnsignedShort(InputStream input) throws IOException {
        int high = input.read();
        int low = input.read();
        if ((high | low) < 0) {
            throw new EOFException("Unexpected end of stream while reading length.");
        }
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    private long readLong(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(8);
        if (bytes.length != 8) {
            throw new EOFException("Unexpected end of stream while reading length.");
        }
        long value = 0L;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFFL);
        }
        return value;
    }

    private String acceptKey(String key) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashed = digest.digest((key + WEB_SOCKET_GUID).getBytes(StandardCharsets.ISO_8859_1));
        return Base64.getEncoder().encodeToString(hashed);
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static final class ClientConnection {
        private final Socket socket;
        private volatile OutputStream output;

        private ClientConnection(Socket socket) {
            this.socket = socket;
        }
    }

    private record WebSocketFrame(int opcode, byte[] payload) {
        private String textPayload() {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }

    private boolean ensureBroadcastReady() {
        try {
            ensureStarted();
            return true;
        } catch (RuntimeException exception) {
            LOG.warn("Chat WebSocket hub unavailable, falling back to database-only mode: {}", exception.getMessage());
            return false;
        }
    }

    private static final class ChatServiceHolder {
        private static final ChatService INSTANCE = new ChatService();
    }
}
