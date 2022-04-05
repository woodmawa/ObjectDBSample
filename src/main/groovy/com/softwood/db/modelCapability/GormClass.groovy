package com.softwood.db.modelCapability

import com.softwood.db.Database
import com.softwood.db.Session

import javax.persistence.EntityManager
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicLong

class GormClass {
    AtomicLong sequence = new AtomicLong (0)
    long id
    String status = "New"

    //see if these can be added dynamically to a domain class via ExpandoMetaClass
    static Map gormMethods = [
            'save':this::save, 'delete':this::delete, 'where':this::where, 'hello': this::hello
    ]

    static def hello () {
        println "\tfrom hello, hi William"
    }

    static def of (def instance) {
        println "of() called to enhance instance "
        ExpandoMetaClass emc = new ExpandoMetaClass (instance.getClass(), true, true)
        ExpandoMetaClass.enableGlobally()

        MetaClass mci = instance.metaClass
        instance.metaClass {
            hello = this::hello
            save = this::save
            delete = this::delete
            where = this::where
        }

        /*gormMethods.each {methodName, closure ->
            println ("adding $methodName, with closure instance")
            //emc.registerInstanceMethod(methodName, closure.rehydrate(instance, instance, instance))
            instance.metaClass."$methodName" = closure
        }*/

        MetaClass afterMci = instance.metaClass
        //emc.initialize()
        //instance.setMetaClass (emc)
        instance
    }

    static Class of (Class <?> clazz) {

        List<Method> reflectedMethods = this.getDeclaredMethods()
        Map<String, Method> gormReflectedMethods = [:]
        reflectedMethods.each {
            switch (it.name) {
                case 'save' -> gormReflectedMethods.put (it.name, it)
                case 'delete' -> gormReflectedMethods.put (it.name, it)
                case 'where' -> gormReflectedMethods.put (it.name, it)
                case 'hello' -> gormReflectedMethods.put ('greeting', it)
            }
        }
        println "of() called to enhance class "

        ExpandoMetaClass.enableGlobally()

        ExpandoMetaClass emc = new ExpandoMetaClass (clazz, true, true)

        MetaClass mc = clazz.metaClass

        gormMethods.each {methodName, closure ->
            //create list of metaMethod from each closure and add to metaClass
            //List<MetaMethod> lmm = ClosureMetaMethod.createMethodList(methodName, clazz, closure)
            //lmm.each {emc.registerInstanceMethod(it)}
            //emc."$methodName" = closure.rehydrate(clazz,clazz,null)//new MethodClosure (clazz, methodName)
            mc."$methodName" = closure
        }

        //emc.initialize()

        List expMm = (mc as ExpandoMetaClass).getExpandoMethods()
        //clazz.setMetaClass (emc)
        clazz
    }

    def save () {
        id = sequence.incrementAndGet()

        Database.withSession { Session session ->
            status = "attached"

            session.save(this)
        }

    }

    void delete () {
        Database.withSession { Session session ->
            def obj = session.delete(id)
            status = "soft deleted"
        }
    }

    def where (/*@DelegatesTo (DomainClass)*/ Closure closure) {
        Closure whereConstraint = closure.clone()
        def result = whereConstraint.call()
        result
    }

    static long count (delegateClazz) {
        Database.withSession { Session session ->
            EntityManager em = session.getEntityManager()
            em.flush()
            String clsName = delegateClazz.simpleName
            javax.persistence.TypedQuery query = em.createQuery("SELECT count(r) FROM  ${clsName} r", Long)
            query.getSingleResult()
        }

    }
}