package com.softwood.com.softwood.db


import groovy.util.logging.Slf4j

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import javax.persistence.Persistence
import javax.persistence.PersistenceUtil

/**
 * database session in openSessionInView mode
 *
 * Session will hold an active entity manager per thread called on
 *
 * sessions can be named if required
 *
 * session will delegate methods to entityManager where not explicitly defined in Session
 * otherwise it pushes it to the metaClass to try and resolve the method call
 *
 * @Author will woodman
 * @Date 21-03-2022
 */

@Slf4j
class Session<T> {

    private ThreadLocal<EntityManager> localEntityManager = new ThreadLocal()
    //todo: should errors be thread local also ?
    private List<Throwable> errors = []
    String name = "${getClass().simpleName}@${Integer.toHexString(System.identityHashCode(this)) }"
    PersistenceUtil putil = Persistence.getPersistenceUtil()

    EntityManagerFactory emf
    boolean openState

    Session(boolean autoStart = false) {
        this.emf = Database.getEmf()
        if (autoStart) {
            Database.start()
            localEntityManager.set(emf.createEntityManager())
            openState = true
        } else if (Database.isOpen()){
            EntityManager em = emf.createEntityManager()
            boolean open = em.isOpen()
            localEntityManager.set(em)
            openState = true
        }
        else {
            openState = false
        }

    }

    boolean isOpen () {openState == true}
    boolean isClosed () {openState == false}

    List<Throwable> getErrors() {
        errors.asImmutable()
    }

    boolean hasErrors() {
        errors.size() > 0
    }

    EntityManager getEntityManager() {
        localEntityManager.get()
    }

    T getEntityById (Class<T> entityClass, primaryKey, Map properties = [:]) {
        getEntityManager().find (entityClass, primaryKey, properties)
    }

    T getEntityReferenceById (Class<T> entityClass, primaryKey) {
        getEntityManager().getReference(entityClass, primaryKey)
    }

    boolean isLoaded (record) {
        putil.isLoaded(record)
    }

    boolean isFieldLoaded (record, String fieldName) {
        putil.isLoaded(record, fieldName)
    }

    //kills any uncommitted changes and resets to known state from db
    T refresh (record) {
        getEntityManager().refresh (record)
    }

    T detach (record) {
        getEntityManager().detach (record)
    }

    //cant find an implementation try delegating to the threadLocal entityManager
    def methodMissing (String methodName, def args) {

        if (getEntityManager().respondsTo(methodName, args))
            getEntityManager().invokeMethod(methodName, args)
        else
            //else defer to MetaClass to resolve
            throw new MissingMethodException(name, this, args)
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
        EntityManager em = getEntityManager()
        if (em) {
            em.close()
            localEntityManager.remove()
            openState = false
        }
    }

    def withTransaction (FlushModeType flushMode = FlushModeType.COMMIT, Closure work ) {
        EntityManager em = getEntityManager()
        errors.clear()
        EntityTransaction transaction = new TransactionDelegate (this)
        transaction.with { EntityTransaction tx ->
            try {
                tx.begin()
                assert tx.isActive(), "transaction not started"
                //call work closure with open transaction
                def result = work.call (this)
                //em.flush()
                tx.commit()
                return result
            } catch (Throwable ex) {
                log.debug ("withTransaction():  in transaction threw error $ex, database rollback triggered ")
                errors << ex
                return -1
            } finally {
                if (tx.isActive()) {
                    tx.rollback()
                    log.debug ("withTransaction():  errors $errors, caused database rollback  ")
                }
            }
        }
    }

    EntityTransaction getTransaction () {
        new TransactionDelegate  (this)
    }

    boolean isManaged (record) {
        EntityManager em = getEntityManager()
        em.contains (record)
    }

    boolean isDetached (record) {
        EntityManager em = getEntityManager()
        !em.contains (record)
    }

    def save (records, FlushModeType flushMode = FlushModeType.COMMIT) {
        def result = withTransaction(flushMode = FlushModeType.COMMIT) {Session session ->
            def domainObjectList = records.collect {record ->
                def domainRecord = record
                if(isDetached (record) ) {
                    log.debug ("save():  record $record is not managed, so merge it with cache ")
                    domainRecord = getEntityManager().merge (record)
                }
                EntityManager em = session.getEntityManager()
                em.persist(domainRecord)  //void return ()
                domainRecord
             }
            domainObjectList
        }
        if (result instanceof Collection && result.size() == 1)
            result[0]
        else
            result

    }

    void delete (records, boolean hardDelete = false) {
        withTransaction (FlushModeType.COMMIT) {Session session
            records.each {record ->
                def domainRecord = record
                if(isDetached(record)) {
                    log.debug ("delete():  record $record is not managed, so merge it with cache ")
                    domainRecord = getEntityManager().merge (record)
                }
                if (record.hasProperty('softDeleted') && !hardDelete) {
                    log.debug "soft deleted $domainRecord"
                    record.setProperty ('softDeleted', true)
                } else
                    getEntityManager().remove(domainRecord)
            }
        }
    }

    long count (Class entityClazz) {
        EntityManager em = getEntityManager()
        em.flush()
        javax.persistence.TypedQuery query = em.createQuery("SELECT count(r) FROM  ${entityClazz.getSimpleName()} r", Long)
        query.getSingleResult()

    }

    String toString () {
        "Session (name:$name)"
    }
}