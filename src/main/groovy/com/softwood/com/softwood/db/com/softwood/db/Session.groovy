package com.softwood.com.softwood.db.com.softwood.db

import com.softwood.com.softwood.db.Database

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType

class Session {

    ThreadLocal<EntityManager> localEntityManager = new ThreadLocal()
    List<Throwable> errors = []
    Database db

    EntityManagerFactory emf
    boolean openState

    Session(Database db, EntityManagerFactory factory) {
        this.db = db
        this.emf = factory
        localEntityManager.set (emf.createEntityManager())
        openState = true

    }

    boolean isOpen () {openState}

    EntityManager getEntityManager() {
        localEntityManager.get()
    }

    //cant find an implementation try delegating to the threadLocal entityManager
    def methodMissing (String methodName, def args) {

        if (localEntityManager.get().respondsTo(methodName, args))
            localEntityManager.get().invokeMethod(methodName, args)
        else
            throw new MissingMethodException(name, delegate, args)
        /*
        def dynamicMethods =[]

        def method = dynamicMethods.find { it.match(methodName) }
        if (method){
            Database.metaClass."$methodName" = {Object[] varArgs ->
                method.invokeMethod(delegate, methodName, varArgs)
            }
            return method.invokeMethod (methodName, args)
        } else if (localEntityManager.get().respondsTo(methodName, args))
            localEntityManager.get().invokeMethod(methodName, args)
        else {
            //generate and cache
            Database.metaClass."$methodName" = {varArgs -> println "generated $methodName was called "}
            method.invokeMethod (methodName, args)
        }
        //else throw new MissingMethodException(name, delegate, args)
        */

    }


    void close() {
        EntityManager em
        if (em = localEntityManager.get()) {
            em.close()
            localEntityManager.remove()
            openState = false
        }
    }

    def withTransaction (FlushModeType flushMode = FlushModeType.COMMIT, Closure work ) {
        errors.clear()
        EntityTransaction transaction = new TransactionDelegate (this)
        transaction.with { EntityTransaction tx ->
            try {
                tx.begin()
                //call work closure with open transaction
                work.call (em)
                tx.commit()
            } catch (Throwable ex) {
            errors << ex
            return -1
            } finally {
                if (tx.isActive())
                    tx.rollback()
            }
        }
    }

    EntityTransaction getTransaction () {
        new TransactionDelegate  (this)
    }

    def save (records, FlushModeType flushMode = FlushModeType.COMMIT) {
        def result = withTransaction(flushMode = FlushModeType.COMMIT) {EntityManager em ->
            records.each {rec ->
                em.persist(rec)
            }
        }
        return result

    }

    void delete (records) {
        withTransaction (FlushModeType.COMMIT) {EntityManager em
            records.each {record ->
                em.remove(record)
            }
        }
    }

    long count (Class entityClazz) {
        EntityManager em = localEntityManager.get()
        javax.persistence.Query query = em.createQuery("SELECT count(r) FROM  ${entityClazz.getSimpleName()} r")
        long count = (long) query.getSingleResult()

    }
}
