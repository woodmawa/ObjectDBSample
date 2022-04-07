package com.softwood.db

import com.softwood.db.modelCapability.EntityState
import groovy.util.logging.Slf4j

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import javax.persistence.Persistence
import javax.persistence.PersistenceUtil
import javax.persistence.TypedQuery
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.ParameterExpression
import javax.persistence.criteria.Root

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

                if (record.hasProperty('status') && record.status == EntityState.New) {
                    record.setProperty ('status', EntityState.Persisted )
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

    long delete (records, boolean hardDelete = false) {
        def result = withTransaction (FlushModeType.COMMIT) {Session session
            records.collect {record ->
                def domainRecord = record
                if(isDetached(record)) {
                    log.debug ("delete():  record $record is not managed, so merge it with cache ")
                    domainRecord = getEntityManager().merge (record)
                }
                if (record.hasProperty('status') && !hardDelete) {
                    log.debug "delete(): soft deleted $domainRecord"
                    record.setProperty ('status', EntityState.SoftDeleted)
                    return 1

                } else {
                    log.debug "delete(): record has no softDeleted property so hard delete $domainRecord"
                    getEntityManager().remove(domainRecord)
                    return 1
                }
            }
        }
        if (result instanceof Collection && result.size() == 1)
            result[0]
        else
            result.size()

    }

    long count (Class entityClazz) {
        EntityManager em = getEntityManager()
        em.flush()
        javax.persistence.TypedQuery query = em.createQuery("SELECT count(r) FROM  ${entityClazz.getSimpleName()} r", Long)
        query.getSingleResult()

    }

    def criteriaQuery (Class<T> domainClass, Map params, value) {

        EntityManager em = getEntityManager()
        CriteriaBuilder cb = em.getCriteriaBuilder()
        CriteriaQuery criteriaQueryBuilder = cb.createQuery(domainClass)
        Root<T> entity = criteriaQueryBuilder.from (domainClass)
        criteriaQueryBuilder.select(entity)

        List<ParameterExpression> exprs = params.collect { propName, type ->
            cb.parameter(type, propName)
        }

        if (exprs?[0])
            criteriaQueryBuilder.where (cb.equal (entity.get ('name'), exprs[0]))


        TypedQuery query = em.createQuery (criteriaQueryBuilder)

        query.setParameter(exprs[0], value)
        query.setFlushMode(FlushModeType.AUTO)
        List<T> results = query.getResultList()

    }

    String toString () {
        "Session (name:$name)"
    }
}