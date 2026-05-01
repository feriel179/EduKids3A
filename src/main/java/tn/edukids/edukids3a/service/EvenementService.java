package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EvenementService {

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

    public void supprimer(Evenement evenement) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
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
