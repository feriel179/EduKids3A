package com.edukids.edukids3a.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "evenement")
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_evenement", nullable = false)
    private LocalDate dateEvenement;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(name = "type_evenement", length = 50)
    private String typeEvenement;

    /** URL longue (ex. image IA), chemins locaux ou liens web — {@code TEXT} pour éviter la troncature MySQL. */
    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    @Column(length = 500)
    private String localisation;

    @Column(name = "nb_places_disponibles")
    private Integer nbPlacesDisponibles;

    @Column(name = "likes_count", nullable = false)
    private int likesCount = 0;

    @Column(name = "dislikes_count", nullable = false)
    private int dislikesCount = 0;

    @Column(name = "favorites_count", nullable = false)
    private int favoritesCount = 0;

    @OneToOne(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    private Programme programme;

    /** Côté inverse : une réservation référence toujours l’événement ({@code mappedBy = "evenement"}). */
    @OneToMany(mappedBy = "evenement")
    private List<Reservation> reservations = new ArrayList<>();

    public Evenement() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateEvenement() {
        return dateEvenement;
    }

    public void setDateEvenement(LocalDate dateEvenement) {
        this.dateEvenement = dateEvenement;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getTypeEvenement() {
        return typeEvenement;
    }

    public void setTypeEvenement(String typeEvenement) {
        this.typeEvenement = typeEvenement;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public Integer getNbPlacesDisponibles() {
        return nbPlacesDisponibles;
    }

    public void setNbPlacesDisponibles(Integer nbPlacesDisponibles) {
        this.nbPlacesDisponibles = nbPlacesDisponibles;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public void incrementLikes() {
        this.likesCount++;
    }

    public void decrementLikes() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    public int getDislikesCount() {
        return dislikesCount;
    }

    public void setDislikesCount(int dislikesCount) {
        this.dislikesCount = dislikesCount;
    }

    public void incrementDislikes() {
        this.dislikesCount++;
    }

    public void decrementDislikes() {
        if (this.dislikesCount > 0) {
            this.dislikesCount--;
        }
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(int favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public void incrementFavorites() {
        this.favoritesCount++;
    }

    public void decrementFavorites() {
        if (this.favoritesCount > 0) {
            this.favoritesCount--;
        }
    }

    /** Durée de l’événement le jour J (entre heure début et fin). */
    public Duration getDureeEvenement() {
        if (heureDebut == null || heureFin == null) {
            return Duration.ZERO;
        }
        return Duration.between(heureDebut, heureFin);
    }

    public Programme getProgramme() {
        return programme;
    }

    /** Met à jour le côté inverse (sans persister). À utiliser après sauvegarde du {@link Programme}. */
    public void linkProgramme(Programme p) {
        this.programme = p;
    }
    public List<Reservation> getReservations() {
        return reservations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Evenement that)) {
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
        return titre != null ? titre : "Événement#" + id;
    }
}
