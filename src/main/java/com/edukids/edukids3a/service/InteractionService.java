package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.UserEvenementInteraction;
import com.edukids.edukids3a.model.Utilisateur;
import com.edukids.edukids3a.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Like / dislike (exclusifs) et favori — aligné sur {@code InteractionService} Symfony.
 */
public class InteractionService {

    private static final Logger LOG = LoggerFactory.getLogger(InteractionService.class);

    public record ToggleResult(String action, String type) {
    }

    public record EtatInteractions(boolean liked, boolean disliked, boolean favorited, int likes, int dislikes, int favorites) {
    }

    private UserEvenementInteraction findInteraction(EntityManager em, int userId, int evenementId, String type) {
        var list = em.createQuery(
                        "select i from UserEvenementInteraction i where i.utilisateur.id = :u and i.evenement.id = :e and i.typeInteraction = :t",
                        UserEvenementInteraction.class)
                .setParameter("u", userId)
                .setParameter("e", evenementId)
                .setParameter("t", type)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public EtatInteractions getEtat(int userId, int evenementId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            Evenement ev = em.find(Evenement.class, evenementId);
            if (ev == null) {
                return new EtatInteractions(false, false, false, 0, 0, 0);
            }
            boolean liked = findInteraction(em, userId, evenementId, UserEvenementInteraction.TYPE_LIKE) != null;
            boolean disliked = findInteraction(em, userId, evenementId, UserEvenementInteraction.TYPE_DISLIKE) != null;
            boolean fav = findInteraction(em, userId, evenementId, UserEvenementInteraction.TYPE_FAVORITE) != null;
            return new EtatInteractions(liked, disliked, fav, ev.getLikesCount(), ev.getDislikesCount(), ev.getFavoritesCount());
        }
    }

    public ToggleResult toggleLike(int userId, int evenementId) {
        return toggle(userId, evenementId, UserEvenementInteraction.TYPE_LIKE);
    }

    public ToggleResult toggleDislike(int userId, int evenementId) {
        return toggle(userId, evenementId, UserEvenementInteraction.TYPE_DISLIKE);
    }

    public ToggleResult toggleFavorite(int userId, int evenementId) {
        return toggle(userId, evenementId, UserEvenementInteraction.TYPE_FAVORITE);
    }

    private ToggleResult toggle(int userId, int evenementId, String type) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            Evenement ev = em.find(Evenement.class, evenementId);
            Utilisateur u = em.find(Utilisateur.class, userId);
            if (ev == null || u == null) {
                em.getTransaction().rollback();
                throw new IllegalArgumentException("Événement ou utilisateur introuvable.");
            }
            UserEvenementInteraction existing = findInteraction(em, userId, evenementId, type);
            if (existing != null) {
                em.remove(existing);
                updateCounter(ev, type, -1);
                em.merge(ev);
                em.getTransaction().commit();
                return new ToggleResult("removed", type);
            }
            if (UserEvenementInteraction.TYPE_LIKE.equals(type)) {
                removeIfExists(em, userId, evenementId, UserEvenementInteraction.TYPE_DISLIKE, ev);
            } else if (UserEvenementInteraction.TYPE_DISLIKE.equals(type)) {
                removeIfExists(em, userId, evenementId, UserEvenementInteraction.TYPE_LIKE, ev);
            }
            UserEvenementInteraction neu = new UserEvenementInteraction();
            neu.setUtilisateur(u);
            neu.setEvenement(ev);
            neu.setTypeInteraction(type);
            em.persist(neu);
            updateCounter(ev, type, 1);
            em.merge(ev);
            em.getTransaction().commit();
            return new ToggleResult("added", type);
        } catch (RuntimeException ex) {
            LOG.error("toggle interaction", ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    private void removeIfExists(EntityManager em, int userId, int evenementId, String type, Evenement ev) {
        UserEvenementInteraction ex = findInteraction(em, userId, evenementId, type);
        if (ex != null) {
            em.remove(ex);
            updateCounter(ev, type, -1);
        }
    }

    private static void updateCounter(Evenement ev, String type, int delta) {
        if (delta > 0) {
            switch (type) {
                case UserEvenementInteraction.TYPE_LIKE -> ev.incrementLikes();
                case UserEvenementInteraction.TYPE_DISLIKE -> ev.incrementDislikes();
                case UserEvenementInteraction.TYPE_FAVORITE -> ev.incrementFavorites();
                default -> {
                }
            }
        } else {
            switch (type) {
                case UserEvenementInteraction.TYPE_LIKE -> ev.decrementLikes();
                case UserEvenementInteraction.TYPE_DISLIKE -> ev.decrementDislikes();
                case UserEvenementInteraction.TYPE_FAVORITE -> ev.decrementFavorites();
                default -> {
                }
            }
        }
    }

    public long compterFavorisUtilisateur(int userId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            Long n = em.createQuery(
                            "select count(i) from UserEvenementInteraction i where i.utilisateur.id = :u and i.typeInteraction = :t",
                            Long.class)
                    .setParameter("u", userId)
                    .setParameter("t", UserEvenementInteraction.TYPE_FAVORITE)
                    .getSingleResult();
            return n != null ? n : 0L;
        }
    }
}
