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

    def hardDeleteAll () {
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


    def save (records, FlushModeType flushMode = FlushModeType.COMMIT) {
        log.debug "proxy save():  use session.save() "
        session.save(records)
     }

    long delete (records) {
        log.debug "proxy delete():  use session.delete() "
        session.delete(records)
    }
}
