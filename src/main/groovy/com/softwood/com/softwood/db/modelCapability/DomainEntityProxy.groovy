package com.softwood.com.softwood.db.modelCapability

import com.softwood.com.softwood.db.Session

import javax.persistence.EntityManager

class DomainEntityProxy extends groovy.util.Proxy {

    String className
    Session session
    private String proxyTypeName
    private Class proxyType

    String getEntityClassType() {
        className
    }

    DomainEntityProxy(session, adaptee) {
        this.session = session
        if (adaptee instanceof Class) {
            className = adaptee.simpleName
            proxyTypeName = "class"
            proxyType = adaptee
        }
        else {
            className = adaptee.getClass().getSimpleName()  //its an instance
            proxyTypeName = "instance"
            proxyType = adaptee.getClass()

        }

        wrap (adaptee)

    }

    boolean isClass () {
        proxyTypeName == "class"
    }

    boolean isInstance () {
        proxyTypeName == "instance"
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


}
