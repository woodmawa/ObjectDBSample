package com.softwood.com.softwood.db.modelCapability

import com.softwood.com.softwood.db.Session
import com.softwood.com.softwood.db.TransactionDelegate
import groovy.util.logging.Slf4j

import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import javax.persistence.Persistence
import javax.persistence.PersistenceUtil

enum DomainProxyType {
    Class, Instance
}

@Slf4j
class DomainEntityProxy extends groovy.util.Proxy {

    String className
    Session session
    private String proxyTypeName
    private Class proxyType
    PersistenceUtil putil = Persistence.getPersistenceUtil()


    String getEntityClassType() {
        className
    }

    DomainEntityProxy(session, adaptee) {
        this.session = session
        if (adaptee instanceof Class) {
            className = adaptee.simpleName
            proxyTypeName = DomainProxyType.Class
            proxyType = adaptee
        }
        else {
            className = adaptee.getClass().getSimpleName()  //its an instance
            proxyTypeName = DomainProxyType.Instance
            proxyType = adaptee.getClass()

        }

        wrap (adaptee)

    }

    boolean isClass () {
        proxyTypeName == DomainProxyType.Class
    }

    boolean isInstance () {
        proxyTypeName == DomainProxyType.Instance
    }

    long count () {
        EntityManager em = session.getEntityManager()
        em.flush()
        String clazzString = getEntityClassType()
        javax.persistence.TypedQuery query = em.createQuery("SELECT count(r) FROM  ${clazzString} r")
        long count = (long) query.getSingleResult()
    }

    def deleteAll () {
        String clazzString = getEntityClassType()

        session.withTransaction {
            session.getEntityManager().createQuery("DELETE FROM ${clazzString}").executeUpdate() as long
        }
    }

    def newInstance (args) {
        if (args)
            getAdaptee()::new (args)
        else
            getAdaptee()::new ()
    }

    def methodMissing (String name, args) {
        if (name == 'new') {
            //return proxy for new instance of class being proxied
            return getAdaptee()::new (args)
        } else if (name == 'newProxy') {
            return new DomainEntityProxy (getAdaptee()::new (args))
        }
        if (adaptee.respondsTo (name, args)) {
            adaptee.invokeMethod(name, args)
        } else
            new MissingMethodException(name, this, args) //resolve on metaclass
    }

    boolean isLoaded () {
        if (proxyTypeName == DomainProxyType.Instance)
            putil.isLoaded(adaptee)
        else
            false
    }

    boolean isFieldLoaded (String fieldName) {
        if (proxyTypeName == DomainProxyType.Instance)
            putil.isLoaded(adaptee, fieldName)
        else
            false
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
                def result = work.call (em)
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
    def save (records, FlushModeType flushMode = FlushModeType.COMMIT) {
        def result = withTransaction(flushMode = FlushModeType.COMMIT) {EntityManager em ->
            def domainObjectList = records.collect {record ->
                def domainRecord = record
                if(isDetached (record)) {
                    log.debug ("save():  record $record is not managed, so merge it with cache ")
                    domainRecord = em.merge (record)
                }
                em.persist(domainRecord)
                domainRecord
            }
            domainObjectList
        }
        if (result instanceof Collection && result.size() == 1)
            result[0]
        else
            result

    }

    void delete (records) {
        withTransaction (FlushModeType.COMMIT) {EntityManager em
            records.each {record ->
                def domainRecord = record
                if(isDetached(record)) {
                    log.debug ("delete():  record $record is not managed, so merge it with cache ")
                    domainRecord = em.merge (record)
                }
                em.remove(domainRecord)
            }
        }
    }
}
