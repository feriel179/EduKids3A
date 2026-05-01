package com.edukids.edukids3a.realtime;

import com.edukids.edukids3a.model.MessageAttachment;
import com.edukids.edukids3a.service.ChatService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Client WebSocket du chat.
 * Les messages partent vers le hub local, puis le hub les persiste en base et les diffuse.
 */
public final class ChatRealtimeService {

    private static final ChatRealtimeService INSTANCE = new ChatRealtimeService();
    private static final URI HUB_URI = URI.create("ws://127.0.0.1:" + ChatRealtimeHub.DEFAULT_PORT + "/chat");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    src == null ? com.google.gson.JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(DATE_TIME_FORMATTER.format(src)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER))
            .create();
    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();

    private volatile Consumer<RealtimeEvent> eventConsumer = event -> {
    };
    private volatile long currentUserId;

    private ChatRealtimeService() {
    }

    public static ChatRealtimeService getInstance() {
        return INSTANCE;
    }

    public void connect(long currentUserId, Consumer<RealtimeEvent> consumer) {
        this.currentUserId = currentUserId;
        this.eventConsumer = consumer == null ? event -> {
        } : consumer;
        try {
            ChatRealtimeHub.getInstance().ensureStarted();
        } catch (RuntimeException exception) {
            // If another process already runs the hub, keep trying to connect anyway.
        }

        if (webSocketRef.get() != null) {
            return;
        }

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(HUB_URI, new Listener())
                .whenComplete((webSocket, throwable) -> {
                    if (throwable != null) {
                        return;
                    }
                    webSocketRef.set(webSocket);
                });
    }

    public void disconnect() {
        WebSocket ws = webSocketRef.getAndSet(null);
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    public boolean isConnected() {
        return webSocketRef.get() != null;
    }

    public CompletionStage<Void> sendMessage(MessageRequest request) {
        Objects.requireNonNull(request, "request");
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "send_message");
        payload.addProperty("conversationId", request.conversationId());
        payload.addProperty("senderId", request.senderId());
        payload.addProperty("content", request.content());
        if (request.type() != null) {
            payload.addProperty("type", request.type());
        }
        if (request.filePath() != null) {
            payload.addProperty("filePath", request.filePath());
        }
        if (request.attachment() != null) {
            payload.add("attachment", gson.toJsonTree(request.attachment()));
        }
        return sendAction(payload, () -> persistLocally(request));
    }

    public CompletionStage<Void> editMessage(long messageId, long senderId, String content) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "edit_message");
        payload.addProperty("messageId", messageId);
        payload.addProperty("senderId", senderId);
        payload.addProperty("content", content);
        return sendAction(payload, () -> fallbackChatService().editMessage(messageId, senderId, content));
    }

    public CompletionStage<Void> deleteMessage(long conversationId, long messageId, long senderId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "delete_message");
        payload.addProperty("conversationId", conversationId);
        payload.addProperty("messageId", messageId);
        payload.addProperty("senderId", senderId);
        return sendAction(payload, () -> fallbackChatService().deleteMessage(messageId, senderId));
    }

    private void persistLocally(MessageRequest request) {
        MessageAttachment attachment = null;
        if (request.attachment() != null) {
            attachment = toAttachment(request.attachment());
        }

        if (attachment != null) {
            fallbackChatService().sendMessage(request.conversationId(), request.senderId(), request.content(), attachment);
        } else {
            fallbackChatService().sendMessage(request.conversationId(), request.senderId(), request.content(), request.type(), request.filePath());
        }
    }

    private ChatService fallbackChatService() {
        return FallbackChatServiceHolder.INSTANCE;
    }

    private CompletionStage<Void> sendAction(JsonObject payload, Runnable fallback) {
        Objects.requireNonNull(payload, "payload");
        WebSocket ws = webSocketRef.get();
        if (ws == null) {
            return CompletableFuture.runAsync(fallback);
        }

        CompletableFuture<Void> result = new CompletableFuture<>();
        ws.sendText(gson.toJson(payload), true).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                try {
                    fallback.run();
                    result.complete(null);
                } catch (RuntimeException fallbackException) {
                    result.completeExceptionally(fallbackException);
                }
                return;
            }
            result.complete(null);
        });
        return result;
    }

    private MessageAttachment toAttachment(AttachmentPayload payload) {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setOriginalName(payload.originalName());
        attachment.setStoredName(payload.storedName());
        attachment.setStoragePath(payload.storagePath());
        attachment.setMimeType(payload.mimeType());
        attachment.setSize(payload.size());
        attachment.setImage(payload.image());
        attachment.setType(payload.type());
        attachment.setDuration(payload.duration());
        return attachment;
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleIncoming(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, java.nio.ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, java.nio.ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            webSocketRef.compareAndSet(webSocket, null);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            webSocketRef.compareAndSet(webSocket, null);
        }
    }

    private void handleIncoming(String json) {
        try {
            JsonObject event = JsonParser.parseString(json).getAsJsonObject();
            String type = event.has("type") ? event.get("type").getAsString() : "";
            long conversationId = event.has("conversationId") ? event.get("conversationId").getAsLong() : -1L;
            String message = event.has("message") && !event.get("message").isJsonNull()
                    ? event.get("message").getAsString()
                    : null;
            String content = event.has("content") && !event.get("content").isJsonNull()
                    ? event.get("content").getAsString()
                    : null;
            String messageType = event.has("messageType") && !event.get("messageType").isJsonNull()
                    ? event.get("messageType").getAsString()
                    : null;
            String filePath = event.has("filePath") && !event.get("filePath").isJsonNull()
                    ? event.get("filePath").getAsString()
                    : null;
            long senderId = event.has("senderId") ? event.get("senderId").getAsLong() : -1L;
            Long messageId = event.has("messageId") ? event.get("messageId").getAsLong() : null;
            Long createdAt = event.has("createdAt") ? event.get("createdAt").getAsLong() : null;
            Long updatedAt = event.has("updatedAt") ? event.get("updatedAt").getAsLong() : null;
            AttachmentPayload attachment = null;
            if (event.has("attachment") && event.get("attachment").isJsonObject()) {
                attachment = gson.fromJson(event.get("attachment"), AttachmentPayload.class);
            }

            RealtimeEvent realtimeEvent = new RealtimeEvent(type, conversationId, senderId, messageId, message, content, messageType, filePath, createdAt, updatedAt, attachment);
            Consumer<RealtimeEvent> consumer = eventConsumer;
            Platform.runLater(() -> consumer.accept(realtimeEvent));
        } catch (Exception ignored) {
            // Ignore malformed events.
        }
    }

    public record MessageRequest(long conversationId, long senderId, String content, String type, String filePath,
                                 AttachmentPayload attachment) {
    }

    public record AttachmentPayload(String originalName, String storedName, String storagePath, String mimeType,
                                    long size, boolean image, String type, Integer duration) {
    }

    public record RealtimeEvent(String type, long conversationId, long senderId, Long messageId, String message,
                                String content, String messageType, String filePath, Long createdAt, Long updatedAt,
                                AttachmentPayload attachment) {
        public boolean isMessageCreated() {
            return "message_created".equalsIgnoreCase(type);
        }

        public boolean isMessageUpdated() {
            return "message_updated".equalsIgnoreCase(type);
        }

        public boolean isMessageDeleted() {
            return "message_deleted".equalsIgnoreCase(type);
        }

        public boolean isError() {
            return "error".equalsIgnoreCase(type);
        }
    }

    private static final class FallbackChatServiceHolder {
        private static final ChatService INSTANCE = new ChatService();
    }
}
