package com.edukids;

import com.edukids.entities.Session;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionModelTest {

    @Test
    void sessionDetectsActiveStatusAndClosedDuration() {
        Timestamp login = Timestamp.valueOf("2026-05-02 10:00:00");
        Timestamp logout = Timestamp.valueOf("2026-05-02 11:35:00");
        Session session = new Session(1, 2, "Ada", "ROLE_ELEVE", login, logout,
                "active", "token", "127.0.0.1", login);

        assertTrue(session.isActive());
        assertEquals("1h 35m", session.getDuration());
    }

    @Test
    void sessionHandlesMissingLogin() {
        Session session = new Session();
        session.setSessionStatus("closed");

        assertFalse(session.isActive());
        assertEquals("N/A", session.getDuration());
    }
}
