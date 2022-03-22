package com.softwood.com.softwood.db


import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

class Database {

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("objectdb:myDbFile.odb")
    ThreadLocal<Session> localSession = new ThreadLocal()

    Database () {

    }

    Session getSession() {
        if (localSession.get() == null) {
            localSession.set (new Session(this, emf))
        }
        localSession.get()
    }

    void withSession(Closure code, boolean autoClose=false) {

        Closure codeClone = code.clone()
        codeClone.delegate = this

        //entityManager.with(codeClone)
        codeClone(getSession())

        if (autoClose) {
            getSession().close()
            localSession.remove()
        }

    }

    void withNewSession(Closure code) {

        Session newSession = new Session(this, emf)
        Closure codeClone = code.clone()
        codeClone.delegate = this

        //entityManager.with(codeClone)
        codeClone(newSession)

        newSession.close()

    }

    void shutdown () {
        emf.close()
    }




}
