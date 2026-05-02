package com.edukids.edukids3a.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Like / dislike / favori sur un événement (table {@code user_evenement_interaction}, aligné Symfony).
 */
@Entity
@Table(
        name = "user_evenement_interaction",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_evenement_type",
                columnNames = {"user_id", "evenement_id", "type_interaction"}))
public class UserEvenementInteraction {

    public static final String TYPE_LIKE = "like";
    public static final String TYPE_DISLIKE = "dislike";
    public static final String TYPE_FAVORITE = "favorite";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", referencedColumnName = "id", nullable = false)
    private Evenement evenement;

    @Column(name = "type_interaction", nullable = false, length = 20)
    private String typeInteraction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserEvenementInteraction() {
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    public String getTypeInteraction() {
        return typeInteraction;
    }

    public void setTypeInteraction(String typeInteraction) {
        this.typeInteraction = typeInteraction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserEvenementInteraction that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
