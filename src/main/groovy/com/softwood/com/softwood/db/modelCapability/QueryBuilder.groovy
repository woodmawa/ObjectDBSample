package com.softwood.com.softwood.db.modelCapability

import com.softwood.com.softwood.db.Database

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.ParameterExpression
import javax.persistence.criteria.Path
import javax.persistence.criteria.Root
import groovy.transform.*

class QueryBuilder<T> {

    T entityType
    CriteriaBuilder builder  = Database.emf.createEntityManager().getCriteriaBuilder()

    //defined map of params
    Map<String, ParameterExpression> paramExpressions = [:]

    CriteriaQuery<T> queryDefinition
    Root<T> root

    //constructor
    QueryBuilder (Class domainClass) {
        entityType = domainClass
        //initialise criteria Query
        queryDefinition = builder.createQuery(entityType)
        root = queryDefinition.from (entityType)

        queryDefinition.select(root)  //by default assume whole instance to query

    }

    QueryBuilder from (Class<T> domainClass) {
        entityType = domainClass
        root = queryDefinition.from (entityType)

        this
    }

    QueryBuilder where (Closure clause) {

    }

    QueryBuilder where (expr) {

    }

    QueryBuilder distinct () {
        queryDefinition.distinct(true)
        this
    }

    QueryBuilder select() {
        queryDefinition.select(root)
        this
    }

    //todo needs thoughts on this
    QueryBuilder select (Closure closure ) {
        Closure select = closure.clone()
        select.delegate = root
    }

    //todo will return projection object[] not an entity
    QueryBuilder select (String attribute) {
        Path path = root.get(attribute)
        queryDefinition.select(path)
        this
    }

    //todo will return projection object[] not an entity
    QueryBuilder select (String[] attributes) {
        List<Path> multi = attributes.collect {root.get(it)}
        queryDefinition.multiselect(multi)
        this
    }

    QueryBuilder params() {
        //to to previously set params in queryBuilder
        //query.setParameter(exprs[0], value)

    }

    QueryBuilder addParameter (String propName, Class attType) {
        paramExpressions.put (propName,  builder.parameter(attType, propName) )
        this
    }

    QueryBuilder addParameters (Map<String, Class> params) {
        Map<String, ParameterExpression> exprs = params.collectEntries { propName, type ->
            [propName : builder.parameter(type, propName) ]
        }
        paramExpressions.addAll(exprs)
        this
    }


    @TypeChecked
    //DelegatesTo is compiler hint to type check the closure against the QueryBuilder methods
    def findAll (@DelegatesTo (QueryBuilder) Closure closure) {
        Closure constraint = (Closure) closure.clone()
        constraint.delegate = this
        constraint.setResolveStrategy(Closure.DELEGATE_FIRST)

        constraint()

    }

    def hello() {
        println "hello"
        return this
    }

    def and (String val) {
        println val
        this
    }


}
