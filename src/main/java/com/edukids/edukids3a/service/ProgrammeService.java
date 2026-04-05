package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.Programme;
import com.edukids.edukids3a.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProgrammeService {

    private static final Logger LOG = LoggerFactory.getLogger(ProgrammeService.class);

    public static final int MAX_LIGNES_LISTE = 2000;

    public List<Programme> listerTous() {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            TypedQuery<Programme> q = em.createQuery(
                            "select p from Programme p join fetch p.evenement order by p.evenement.dateEvenement desc",
                            Programme.class)
                    .setMaxResults(MAX_LIGNES_LISTE);
            List<Programme> list = q.getResultList();
            if (list.size() >= MAX_LIGNES_LISTE) {
                LOG.warn("Liste programmes tronquée à {} lignes (limite MAX_LIGNES_LISTE).", MAX_LIGNES_LISTE);
            }
            return list;
        }
    }

    public Programme trouverParId(int id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            Programme p = em.find(Programme.class, id);
            if (p != null) {
                p.getEvenement().getTitre();
            }
            return p;
        }
    }

    public void enregistrer(Programme programme) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            Evenement ev = programme.getEvenement();
            if (ev == null) {
                throw new IllegalArgumentException("L'événement est obligatoire.");
            }
            Evenement managedEvent = em.merge(ev);

            if (programme.getId() == null) {
                programme.setEvenement(managedEvent);
                em.persist(programme);
                managedEvent.linkProgramme(programme);
            } else {
                programme.setEvenement(managedEvent);
                Programme merged = em.merge(programme);
                managedEvent.linkProgramme(merged);
            }
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOG.error("Échec enregistrement programme id={}", programme.getId(), ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void supprimer(Programme programme) {
        EntityManager em = JpaUtil.createEntityManager();
        em.getTransaction().begin();
        try {
            Programme managed = em.merge(programme);
            Evenement ev = managed.getEvenement();
            if (ev != null) {
                ev.linkProgramme(null);
            }
            em.remove(managed);
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOG.error("Échec suppression programme id={}", programme.getId(), ex);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
