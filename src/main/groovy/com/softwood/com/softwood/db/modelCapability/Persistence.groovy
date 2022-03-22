package com.softwood.com.softwood.db.modelCapability

import com.softwood.com.softwood.db.Session

import javax.persistence.EntityManager

trait Persistence {
    Session session

    long count () {
        EntityManager em = session.getEntityManager()
        em.flush()
        javax.persistence.Query query = em.createQuery("SELECT count(r) FROM  ${this.getSimpleName()} r")
        long count = (long) query.getSingleResult()
    }

    static void hello() {
        println "hello"
    }

}
