package com.softwood.com.softwood.db.com.softwood.db

import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

class DelegatingTransaction implements EntityTransaction {

    @Delegate EntityTransaction transaction
    EntityManager em
    Session parentSession

    DelegatingTransaction(Session session) {
        parentSession = session
        this.em = session.localEntityManager.get()

        transaction = em.getTransaction()
    }

    //you can override default EntityTransaction methods here..
    /*@Override
    void begin () {
        ....etc
    }*/

}
