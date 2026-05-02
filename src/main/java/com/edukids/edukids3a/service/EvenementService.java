package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EvenementService {

    public record StatsInteractions(long totalEvents, long totalLikes, long totalDislikes, long totalFavorites) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(EvenementService.class);

    /** Limite de lignes pour les listes (évite de charger des millions de lignes par mégarde). */
    public static final int MAX_LIGNES_LISTE = 2000;

    public List<Evenement> listerTous() {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            TypedQuery<Evenement> q = em.createQuery(
                            "select distinct e from Evenement e left join fetch e.programme order by e.dateEvenement desc, e.heureDebut",
                            Evenement.class)
                    .setMaxResults(MAX_LIGNES_LISTE);
            List<Evenement> list = q.getResultList();
            if (list.size() >= MAX_LIGNES_LISTE) {
                LOG.warn("Liste événements tronquée à {} lignes (limite MAX_LIGNES_LISTE).", MAX_LIGNES_LISTE);
            }
            return list;
        }
    }

    public Evenement trouverParId(int id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            Evenement e = em.find(Evenement.class, id);
            if (e != null) {
                e.getProgramme();
            }
            return e;
        }
    }

    public void enregistrer(Evenement evenement) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            if (evenement.getId() == null) {
                em.persist(evenement);
            } else {
                em.merge(evenement);
            }
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOG.error("Échec enregistrement événement id={}", evenement.getId(), ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public StatsInteractions getStatsInteractions() {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            long totalEv = em.createQuery("select count(e) from Evenement e", Long.class).getSingleResult();
            Long likes = em.createQuery("select coalesce(sum(e.likesCount), 0) from Evenement e", Long.class).getSingleResult();
            Long dislikes = em.createQuery("select coalesce(sum(e.dislikesCount), 0) from Evenement e", Long.class).getSingleResult();
            Long favs = em.createQuery("select coalesce(sum(e.favoritesCount), 0) from Evenement e", Long.class).getSingleResult();
            return new StatsInteractions(totalEv, likes != null ? likes : 0L, dislikes != null ? dislikes : 0L, favs != null ? favs : 0L);
        }
    }

    public List<Evenement> topParLikes(int limit) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery("select e from Evenement e order by e.likesCount desc, e.titre", Evenement.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public List<Evenement> topParDislikes(int limit) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery("select e from Evenement e order by e.dislikesCount desc, e.titre", Evenement.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public List<Evenement> topParFavoris(int limit) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery("select e from Evenement e order by e.favoritesCount desc, e.titre", Evenement.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    /**
     * Somme des places réservées (adultes + enfants) pour un événement.
     */
    public int sommePlacesReserveesPourEvenement(int evenementId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            Long sum = em.createQuery(
                            "select coalesce(sum(r.nbAdultes + r.nbEnfants), 0) from Reservation r where r.evenement.id = :id",
                            Long.class)
                    .setParameter("id", evenementId)
                    .getSingleResult();
            return sum != null ? sum.intValue() : 0;
        }
    }

    public List<Evenement> listerEvenementsFavorisPourUtilisateur(int userId, int max) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery(
                            "select e from UserEvenementInteraction i join i.evenement e left join fetch e.programme "
                                    + "where i.utilisateur.id = :u and i.typeInteraction = :t order by i.createdAt desc",
                            Evenement.class)
                    .setParameter("u", userId)
                    .setParameter("t", com.edukids.edukids3a.model.UserEvenementInteraction.TYPE_FAVORITE)
                    .setMaxResults(max)
                    .getResultList();
        }
    }

    public void supprimer(Evenement evenement) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            Integer id = evenement.getId();
            if (id != null) {
                em.createQuery("delete from UserEvenementInteraction i where i.evenement.id = :id")
                        .setParameter("id", id)
                        .executeUpdate();
                em.createQuery("delete from Reservation r where r.evenement.id = :id")
                        .setParameter("id", id)
                        .executeUpdate();
            }
            Evenement managed = em.merge(evenement);
            em.remove(managed);
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOG.error("Échec suppression événement id={}", evenement.getId(), ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
