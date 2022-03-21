package com.softwood.application

import com.softwood.com.softwood.db.Database
import com.softwood.com.softwood.db.com.softwood.db.Session
import com.softwood.model.Customer
import com.softwood.model.Site

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

class Application {

    static main (args) {
        Database database = new Database()

        database.withNewSession {Session sess ->
            Customer c1 = sess.getEntityById(Customer, 1)
            Customer rc1 = sess.getEntityReferenceById(Customer, 1)
            assert c1 == rc1
            println "first customer in DB is $c1, in session $sess"
        }

        database.withSession { Session sess ->
            Customer cust = new Customer(name:"NatWest")

            def id = sess.save(cust)
            println "new cust id is $id "

            println "current count of cus is : ${sess.count(Customer)} in session $sess"
        }
        database.shutdown()

        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("objectdb:myDbFile.odb")

        EntityManager entityManager = emf.createEntityManager()
        /*entityManager.with{em ->
            Customer cust = new Customer(name:"HSBC")
            Site hosite =new Site(name:"head office, canary wharf")

            cust.sites << hosite
            hosite.customer = cust
            em.getTransaction().with{tx ->
                try {
                    tx.begin()
                    // Operations that modify the database should come here.
                    em.persist(cust)
                    em.persist(hosite)
                    tx.commit()
                    println "custId: $cust.id, and siteId: $hosite.id"
                }
                finally {
                    if (tx.isActive())
                        tx.rollback()
                }
            }

        }*/

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
