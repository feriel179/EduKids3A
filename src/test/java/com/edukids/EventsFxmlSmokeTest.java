package com.edukids;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventsFxmlSmokeTest {

    private static volatile boolean fxStarted;

    @BeforeAll
    static void startJavaFx() throws Exception {
        if (fxStarted) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            fxStarted = true;
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void eventScreensLoad() throws Exception {
        List<String> screens = List.of(
                "/fxml/BackPageListeEvenements.fxml",
                "/fxml/BackPageFicheEvenement.fxml",
                "/fxml/BackPageListeProgrammes.fxml",
                "/fxml/BackPageFicheProgramme.fxml",
                "/fxml/BackPageListeReservations.fxml",
                "/fxml/BackPageStatistiques.fxml",
                "/fxml/FrontPageEvenements.fxml"
        );

        for (String screen : screens) {
            URL url = EventsFxmlSmokeTest.class.getResource(screen);
            assertNotNull(url, "Missing FXML " + screen);
            loadOnFxThread(url);
        }
    }

    private static void loadOnFxThread(URL url) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                new FXMLLoader(url).load();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out loading " + url);
        if (failure.get() != null) {
            throw new AssertionError("Could not load " + url, failure.get());
        }
    }
}
