package com.softwood.com.softwood.db.modelCapability

import javax.persistence.Version

trait Versionable {
    @Version private long version

    long getVersion () {
        version
    }

}