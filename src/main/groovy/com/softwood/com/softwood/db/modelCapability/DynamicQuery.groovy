package com.softwood.com.softwood.db.modelCapability

import com.softwood.com.softwood.db.Session

import javax.persistence.TypedQuery

class DynamicQuery<T> {

    private QueryBuilder builder
    private Session session
    TypedQuery query

    DynamicQuery (Session session, QueryBuilder builder) {
        this.session = session
        this.builder = builder

        query = session.getEntityManager().createQuery(builder.queryDefinition )
    }

    List<T> list() {
        //attach query from builder to the session

       query.setFlushMode(javax.persistence.FlushModeType.AUTO)
        List<T> results = query.getResultList()
    }

    void setParameter (String param, value) {
        query.setParameter (param, value)

    }

}
