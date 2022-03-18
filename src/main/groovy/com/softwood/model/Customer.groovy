package com.softwood.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Customer {
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    final long id

    String name
    List<Site> sites = []

    String toString() {
        "Customer [id: $id, name:$name]"
    }
}
