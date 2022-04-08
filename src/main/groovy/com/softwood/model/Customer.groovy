package com.softwood.model

import com.softwood.db.modelCapability.EntityState

import javax.jdo.annotations.Index
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Version

import static javax.persistence.CascadeType.*

@Entity
class Customer {
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY) final long id

    @Version long version
    long getVersion () {
        version
    }

    EntityState status = EntityState.New
    EntityState getStatus() {status}
    EntityState setStatus(EntityState s) {status = s}

    @Index (name="customers_idx") String name

    @OneToMany (cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Collection<Site> sites = []

    boolean isActive () { status == EntityState.Persisted }

    //implement abstract trait method
    static String getEntityClassName () {
        "Customer"
    }

    String toString() {
        "Customer [id: $id, name:$name]"
    }

    /*static def $static_methodMissing (String name, args) {
        println "missing method $name ($args)"
    }*/
}
