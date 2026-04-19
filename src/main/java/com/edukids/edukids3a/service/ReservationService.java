package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.Reservation;
import com.edukids.edukids3a.model.Utilisateur;
import com.edukids.edukids3a.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Réservations (participation aux événements), aligné sur le flux Symfony {@code ReservationController}.
 */
public class ReservationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationService.class);
    public static final int MAX_LIGNES = 2000;

    public List<Reservation> listerToutes() {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            TypedQuery<Reservation> q = em.createQuery(
                    "select distinct r from Reservation r join fetch r.evenement join fetch r.utilisateur "
                            + "order by r.dateReservation desc",
                    Reservation.class);
            q.setMaxResults(MAX_LIGNES);
            return q.getResultList();
        }
    }

    public List<Reservation> listerPourUtilisateur(int userId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery(
                            "select distinct r from Reservation r join fetch r.evenement e left join fetch e.programme "
                                    + "where r.utilisateur.id = :u order by r.dateReservation desc",
                            Reservation.class)
                    .setParameter("u", userId)
                    .setMaxResults(MAX_LIGNES)
                    .getResultList();
        }
    }

    public Reservation trouverParId(int id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            List<Reservation> list = em.createQuery(
                            "select r from Reservation r join fetch r.evenement join fetch r.utilisateur where r.id = :id",
                            Reservation.class)
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * Crée une réservation après contrôle des places (comme le web).
     */
    public void creerReservation(int userId, int evenementId, String nom, String prenom, String email,
                               String telephone, int nbAdultes, int nbEnfants) {
        String n = nom == null ? "" : nom.trim();
        String p = prenom == null ? "" : prenom.trim();
        String emailTrim = email == null ? "" : email.trim();
        if (n.isEmpty() || p.isEmpty() || emailTrim.isEmpty()) {
            throw new IllegalArgumentException("Le nom, le prénom et l'e-mail sont obligatoires.");
        }
        int demande = nbAdultes + nbEnfants;
        if (demande <= 0) {
            throw new IllegalArgumentException("Vous devez réserver au moins une place.");
        }
        if (nbAdultes < 0 || nbEnfants < 0) {
            throw new IllegalArgumentException("Les nombres de places doivent être positifs ou nuls.");
        }

        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            Evenement ev = em.find(Evenement.class, evenementId);
            Utilisateur u = em.find(Utilisateur.class, userId);
            if (ev == null || u == null) {
                throw new IllegalArgumentException("Événement ou utilisateur introuvable.");
            }

            Long sumObj = em.createQuery(
                            "select coalesce(sum(r.nbAdultes + r.nbEnfants), 0) from Reservation r where r.evenement.id = :id",
                            Long.class)
                    .setParameter("id", evenementId)
                    .getSingleResult();
            int reserves = sumObj != null ? sumObj.intValue() : 0;

            Integer cap = ev.getNbPlacesDisponibles();
            if (cap != null) {
                int restantes = cap - reserves;
                if (restantes <= 0) {
                    throw new IllegalArgumentException("Il n'y a plus de places disponibles pour cet événement.");
                }
                if (demande > restantes) {
                    throw new IllegalArgumentException("Il ne reste que " + restantes
                            + " place(s) disponible(s). Réduisez le nombre de places demandées.");
                }
            }

            Reservation r = new Reservation();
            r.setUtilisateur(u);
            r.setEvenement(ev);
            r.setNom(n);
            r.setPrenom(p);
            r.setEmail(emailTrim);
            String tel = telephone == null ? "" : telephone.trim();
            r.setTelephone(tel.isEmpty() ? null : tel);
            r.setNbAdultes(nbAdultes);
            r.setNbEnfants(nbEnfants);
            em.persist(r);
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOG.error("création réservation", ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void supprimer(int reservationId) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            Reservation r = em.find(Reservation.class, reservationId);
            if (r != null) {
                em.remove(r);
            }
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOG.error("suppression réservation id={}", reservationId, ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
