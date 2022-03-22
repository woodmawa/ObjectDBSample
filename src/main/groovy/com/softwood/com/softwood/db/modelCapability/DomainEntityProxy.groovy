package com.softwood.com.softwood.db.modelCapability

import com.softwood.com.softwood.db.Session

import javax.persistence.EntityManager

class DomainEntityProxy extends groovy.util.Proxy {

    String className
    Session session

    String getEntityClassName () {
        className
    }

    DomainEntityProxy(session, adaptee) {
        this.session = session
        if (adaptee instanceof Class)
            className = adaptee.simpleName
        else
            className = adaptee.getClass().getSimpleName()  //its an instance
        wrap (adaptee)

    }

    long count () {
        EntityManager em = session.getEntityManager()
        em.flush()
        String clazzString = getEntityClassName()//this.getSimpleName()
        javax.persistence.Query query = em.createQuery("SELECT count(r) FROM  ${clazzString} r")
        long count = (long) query.getSingleResult()
    }

    def methodMissing (String name, args) {
        if (name == 'new') {
            //return proxy for new instance of class being proxied
            return new DomainEntityProxy (getAdaptee()::new (args))
        }
        if (adaptee.respondsTo (name, args)) {
            adaptee.invokeMethod(name, args)
        } else
            new MissingMethodException(name, this, args) //resolve on metaclass
    }


}
