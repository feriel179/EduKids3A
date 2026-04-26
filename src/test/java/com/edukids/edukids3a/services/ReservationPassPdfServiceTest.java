package com.edukids.edukids3a.services;

import com.edukids.edukids3a.models.Evenement;
import com.edukids.edukids3a.models.Reservation;
import com.edukids.edukids3a.models.Utilisateur;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationPassPdfServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void qrPayload_matchesWebContract() {
        Reservation r = fakeReservation();

        String json = ReservationPassPdfService.buildQrPayloadJson(r);
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();

        assertEquals(42, o.get("id").getAsInt());
        assertEquals("Atelier Robotique", o.get("event").getAsString());
        assertEquals("Sara Ben Ali", o.get("user").getAsString());
        assertEquals("2026-05-02", o.get("date").getAsString());
        assertEquals(3, o.get("places").getAsInt());
        assertEquals("42", o.get("reservation").getAsString());
    }

    @Test
    void exportPdf_writesNonEmptyFile() throws Exception {
        ReservationPassPdfService svc = new ReservationPassPdfService();
        Reservation r = fakeReservation();
        Path out = tempDir.resolve("pass-test.pdf");

        svc.exportPdf(r, out);

        assertTrue(Files.exists(out), "Le PDF n'a pas ete cree.");
        assertTrue(Files.size(out) > 1024, "Le PDF semble vide ou trop petit.");
    }

    private static Reservation fakeReservation() {
        Evenement ev = new Evenement();
        ev.setId(9);
        ev.setTitre("Atelier Robotique");
        ev.setDateEvenement(LocalDate.of(2026, 5, 2));
        ev.setHeureDebut(LocalTime.of(10, 0));
        ev.setHeureFin(LocalTime.of(12, 0));
        ev.setLocalisation("Salle A");
        ev.setNbPlacesDisponibles(40);

        Utilisateur u = new Utilisateur();
        u.setId(7);
        u.setEmail("sara@example.com");

        Reservation r = new Reservation();
        r.setId(42);
        r.setEvenement(ev);
        r.setUtilisateur(u);
        r.setPrenom("Sara");
        r.setNom("Ben Ali");
        r.setEmail("sara@example.com");
        r.setNbAdultes(2);
        r.setNbEnfants(1);
        r.setDateReservation(LocalDateTime.of(2026, 4, 26, 1, 10));
        return r;
    }
}
