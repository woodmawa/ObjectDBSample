package com.softwood.model

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
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    final long id
    @Version long version

    long getVersion () {
        version
    }

    @Index (name="customers_idx") String name

    @OneToMany (cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Collection<Site> sites = []

    boolean softDeleted = false
    boolean isActive () { softDeleted == false }

    //implement abstract trait method
    static String getEntityClassName () {
        "Customer"
    }

    String toString() {
        "Customer [id: $id, name:$name]"
    }
}
