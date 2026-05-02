package com.edukids.edukids3a.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "programme")
public class Programme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "evenement_id", referencedColumnName = "id", nullable = false, unique = true)
    private Evenement evenement;

    @Column(name = "pause_debut", nullable = false)
    private LocalTime pauseDebut;

    @Column(name = "pause_fin", nullable = false)
    private LocalTime pauseFin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String activites;

    @Column(name = "documents_requis", nullable = false, columnDefinition = "TEXT")
    private String documentsRequis;

    @Column(name = "materiels_requis", nullable = false, columnDefinition = "TEXT")
    private String materielsRequis;

    public Programme() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    public LocalTime getPauseDebut() {
        return pauseDebut;
    }

    public void setPauseDebut(LocalTime pauseDebut) {
        this.pauseDebut = pauseDebut;
    }

    public LocalTime getPauseFin() {
        return pauseFin;
    }

    public void setPauseFin(LocalTime pauseFin) {
        this.pauseFin = pauseFin;
    }

    public String getActivites() {
        return activites;
    }

    public void setActivites(String activites) {
        this.activites = activites;
    }

    public String getDocumentsRequis() {
        return documentsRequis;
    }

    public void setDocumentsRequis(String documentsRequis) {
        this.documentsRequis = documentsRequis;
    }

    public String getMaterielsRequis() {
        return materielsRequis;
    }

    public void setMaterielsRequis(String materielsRequis) {
        this.materielsRequis = materielsRequis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Programme that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        if (evenement != null && evenement.getTitre() != null) {
            return "Programme — " + evenement.getTitre();
        }
        return "Programme#" + id;
    }
}
