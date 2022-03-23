package com.softwood.com.softwood.db


import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

class Database {

    private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("objectdb:myDbFile.odb")
    private static ThreadLocal<Session> localSession = new ThreadLocal()

    Database () {

    }

    static EntityManagerFactory getEmf () {
        emf
    }

    static isOpen () {
        emf?.isOpen()
    }

    static Session getSession() {
        if (localSession.get() == null) {
            localSession.set (new Session())
        }
        localSession.get()
    }

    static void withSession(Closure code, boolean autoClose=false) {

        Closure codeClone = code.clone()
        codeClone.delegate = this

        //entityManager.with(codeClone)
        codeClone(getSession())

        if (autoClose) {
            getSession().close()
            localSession.remove()
        }

    }

    static void withNewSession(Closure code) {

        Session newSession = new Session()
        Closure codeClone = code.clone()
        codeClone.delegate = this

        //entityManager.with(codeClone)
        codeClone(newSession)

        newSession.close()

    }

    static void shutdown () {
        emf.close()
        emf = null
    }

    static Database start () {
        if (!emf.isOpen() || emf == null) {
            emf = Persistence.createEntityManagerFactory("objectdb:myDbFile.odb")
        }
        this
    }

}
