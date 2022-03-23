package com.softwood.application

import com.softwood.com.softwood.db.Database
import com.softwood.com.softwood.db.Session
import com.softwood.com.softwood.db.modelCapability.DomainEntityProxy
import com.softwood.model.Customer
import com.softwood.model.Site

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.persistence.metamodel.Metamodel

class Application {

    static main (args) {
        //Database database = new Database()

        long recordId
        Database.withSession { Session sess ->
            def domainClass = new DomainEntityProxy (sess, Customer)

            long deleted = domainClass.hardDeleteAll()
            println "deleted $deleted records from ${domainClass.getClassName()}"

            Customer cust = new Customer(name:"NatWest")
            Site branch = new Site (name:"ipswich branch")
            branch.customer = cust
            cust.sites << branch

            def savedCust = sess.save(cust)
            println "new cust is $savedCust "
            recordId = savedCust.id

            def custRecs = domainClass.count()
            println "count of cust recs $custRecs"

            Customer newCust = domainClass::newInstance()
            newCust.name = "Barclays"
            Customer savedNewCust = domainClass.save (newCust)
            println "new Barclays is $savedNewCust "

           assert sess.isManaged(savedCust)

            println "current count of cus is : ${sess.count(Customer)} in session $sess"
        }

        Database.withNewSession {Session sess ->
            Customer c1 = sess.getEntityById(Customer, recordId)
            Customer rc1 = sess.getEntityReferenceById(Customer, recordId)
            Metamodel mm = sess.getMetamodel()
            Set sEntities = mm.getEntities()
            //EntityType et = mm.entity(Customer) as EntityType
            //SingularAttribute sa = et.getVersion(Customer)
            assert c1 == rc1
            println "first customer in DB is $c1, in session $sess"
        }


        Database.shutdown()

        //standalone query using native features
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("objectdb:myDbFile.odb")

        EntityManager entityManager = emf.createEntityManager()

        entityManager.with{em ->

            Customer c1 = em.find (Customer, 1)
            println "read customer by id 1, $c1"

            javax.persistence.Query query = em.createQuery("SELECT count(c) FROM  Customer c")
            long count = (long) query.getSingleResult()
            println "customer records in db $count"

        }
        emf.close()
        println "closed emf"
    }

}
