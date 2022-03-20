package com.softwood.com.softwood.db.com.softwood.db

import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

class TransactionDelegate implements EntityTransaction {

    @Delegate EntityTransaction transaction
    EntityManager em
    Session parentSession

    TransactionDelegate(Session session) {
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
