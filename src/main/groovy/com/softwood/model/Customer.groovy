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
    @Version private long version

    long getVersion () {
        version
    }

    @Index (name="customers_idx") String name

    @OneToMany (cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Collection<Site> sites = []

    String toString() {
        "Customer [id: $id, name:$name]"
    }
}
