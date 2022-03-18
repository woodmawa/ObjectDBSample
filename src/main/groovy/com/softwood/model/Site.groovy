package com.softwood.model

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class Site {
    //generates ids per class hierarchy, one generator per type
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    final long id

    String name

    @ManyToOne(cascade= CascadeType.PERSIST)
    Customer customer

    String toString() {
        "Site [id: $id, name:$name] belongs to $customer"
    }
}
