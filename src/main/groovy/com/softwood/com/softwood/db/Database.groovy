package com.softwood.com.softwood.db

import com.softwood.com.softwood.db.com.softwood.db.Session

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.Persistence

class Database {

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("objectdb:myDbFile.odb")
    ThreadLocal<Session> localSession = new ThreadLocal()

    Database () {

    }

    void withSession(Closure code, boolean autoClose=false) {

        if (!localSession.get()) {
            localSession.set (new Session(this, emf))
        }
        Closure codeClone = code.clone()
        codeClone.delegate = this

        //entityManager.with(codeClone)
        codeClone(localSession.get())

        if (autoClose)
            localSession.get().close()

    }



    void shutdown () {
        emf.close()
    }




}
