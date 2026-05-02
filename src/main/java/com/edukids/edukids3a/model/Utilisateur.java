package com.edukids.edukids3a.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compte applicatif (table {@code user}, aligné Symfony / auth JDBC).
 * Entité JPA pour les relations (ex. {@link Reservation}).
 */
@Entity
@Table(name = "user")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String roles;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", length = 120)
    private String firstName;

    @Column(name = "last_name", length = 120)
    private String lastName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String avatar;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @OneToMany(mappedBy = "utilisateur")
    private List<Reservation> reservations = new ArrayList<>();

    public Utilisateur() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Utilisateur that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
