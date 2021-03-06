package com.softwood.model

import com.softwood.db.modelCapability.EntityState

import javax.jdo.annotations.Index
import javax.jdo.annotations.Indices
import javax.persistence.CascadeType
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

import javax.persistence.ManyToOne
import javax.persistence.Version

@Entity
@Index (members =  ["address.city", "address.postalCode"])
class Site {
    //generates ids per class hierarchy, one generator per type
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    final long id
    @Version long version

    long getVersion () {
        version
    }

    EntityState status = EntityState.New

    @Index (name="sites_idx") String name

    @ManyToOne  //(cascade = CascadeType.PERSIST)
    Customer customer

    @Embedded GeoAddress address

    boolean isActive () { status == EntityState.Persisted }

    String toString() {
        "Site [id: $id, name:$name] belongs to $customer"
    }
}
