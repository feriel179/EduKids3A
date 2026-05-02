package com.edukids.edukids3a.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatModelsTest {

    @Test
    void messageInitializesDefaultsAndTracksDeletion() {
        Message message = new Message();

        assertEquals("text", message.getType());
        assertEquals("sent", message.getStatus());
        assertFalse(message.isRead());
        assertFalse(message.isDeleted());
        assertNotNull(message.getCreatedAt());
        assertNotNull(message.getUpdatedAt());

        message.setDeletedAt(LocalDateTime.now());

        assertTrue(message.isDeleted());
    }

    @Test
    void messageAddsAttachmentAndBackfillsMessageId() {
        Message message = new Message();
        message.setId(42L);
        MessageAttachment attachment = new MessageAttachment();
        attachment.setOriginalName("photo.png");

        message.addAttachment(attachment);

        assertEquals(1, message.getAttachments().size());
        assertSame(message, attachment.getMessage());
        assertEquals(42L, attachment.getMessageId());
        assertEquals("photo.png", attachment.toString());
    }

    @Test
    void conversationHandlesMessagesParticipantsAndDisplayName() {
        Conversation conversation = new Conversation(1L, "", true, "key");
        Message message = new Message();
        ConversationParticipant participant = new ConversationParticipant();

        conversation.addMessage(message);
        conversation.addParticipant(participant);

        assertEquals(1, conversation.getMessages().size());
        assertEquals(1, conversation.getParticipants().size());
        assertEquals("Conversation de groupe", conversation.toString());

        conversation.setTitle("Groupe parents");
        conversation.removeMessage(message);
        conversation.removeParticipant(participant);

        assertEquals("Groupe parents", conversation.toString());
        assertEquals(0, conversation.getMessages().size());
        assertEquals(0, conversation.getParticipants().size());
    }

    @Test
    void participantTracksDeletedAndHiddenState() {
        ConversationParticipant participant = new ConversationParticipant();
        assertNotNull(participant.getJoinedAt());
        assertFalse(participant.isDeleted());
        assertFalse(participant.isHidden());

        participant.setDeletedAt(LocalDateTime.now());
        participant.setHiddenAt(LocalDateTime.now());

        assertTrue(participant.isDeleted());
        assertTrue(participant.isHidden());
    }

    @Test
    void dashboardSnapshotProtectsCollectionsAndBuildsKpis() {
        ChatDashboardSnapshot snapshot = new ChatDashboardSnapshot();
        snapshot.setTotalMessages(12);
        snapshot.setTotalUsers(4);
        snapshot.setActiveUsers7(3);
        snapshot.setActiveUsers30(4);
        snapshot.setTopUserName("Ada");
        snapshot.setTopUserMessages(8);
        snapshot.setMessagesToday(2);
        snapshot.setMessagesWeek(9);
        snapshot.setCharts(null);
        snapshot.setTopUsers(new ArrayList<>(List.of(new ChatTopUser(1L, 1, "Ada", "ada@example.com", 8, 1.0, "Today"))));
        snapshot.setRecentActivities(null);

        ChatDashboardKpis kpis = snapshot.asKpis();

        assertNotNull(snapshot.getCharts());
        assertEquals(1, snapshot.getTopUsers().size());
        assertEquals(0, snapshot.getRecentActivities().size());
        assertEquals(12, kpis.getTotalMessages());
        assertEquals("Ada", kpis.getTopUserName());
        assertEquals(9, kpis.getMessagesWeek());
    }

    @Test
    void chartAndActivityModelsExposeValues() {
        ChatDashboardCharts charts = new ChatDashboardCharts();
        charts.setHourly(List.of(new ChatChartPoint("10h", 4)));
        charts.setDaily(null);
        charts.setActivitySplit(List.of(new ChatChartPoint("Text", 8)));
        ChatRecentActivity activity = new ChatRecentActivity("Nouveau message", "Ada", "10:00", "message");

        assertEquals("10h", charts.getHourly().get(0).getLabel());
        assertEquals(0, charts.getDaily().size());
        assertEquals(8.0, charts.getActivitySplit().get(0).getValue());
        assertEquals("Nouveau message", activity.getTitle());
        assertEquals("message", activity.getType());
    }
}
