package com.softwood.com.softwood.db.com.softwood.db

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType

class Session {

    ThreadLocal<EntityManager> localEntityManager = new ThreadLocal()
    List<Throwable> errors = []

    EntityManagerFactory emf
    boolean openState

    Session(EntityManagerFactory factory) {
        this.emf = factory
        localEntityManager.set (emf.createEntityManager())
        openState = true

    }

    boolean isOpen () {openState}

    EntityManager getEntityManager() {
        localEntityManager.get()
    }

    void close() {
        EntityManager em
        if (em = localEntityManager.get()) {
            em.close()
            localEntityManager.remove()
            openState = false
        }
    }

    def save (records, FlushModeType flushMode = FlushModeType.COMMIT) {
        errors.clear()
        EntityManager em = localEntityManager.get()
        em.getTransaction().with { EntityTransaction tx ->
            try {
                tx.begin()
                //use groovy to do an each whether its a list or single record
                records.each {rec ->
                    em.persist(rec)
                }
                tx.commit()
                return records.collect{it.id}
            } catch (Throwable ex) {
                errors << ex
                return -1
            } finally {
                if (tx.isActive())
                    tx.rollback()
            }
        }
    }

    void delete (records) {
        errors.clear()
        EntityManager em = localEntityManager.get()
        em.getTransaction().with { EntityTransaction tx ->
            try {
                tx.begin()
                //use groovy to do an each whether its a list or single record
                records.each {
                    em.remove(record)
                }
                tx.commit()
            } catch (Throwable ex) {
                errors << ex
            } finally {
                if (tx.isActive())
                    tx.rollback()
            }
        }
    }

    long count (Class entityClazz) {
        EntityManager em = localEntityManager.get()
        javax.persistence.Query query = em.createQuery("SELECT count(r) FROM  ${entityClazz.getSimpleName()} r")
        long count = (long) query.getSingleResult()

    }
}
