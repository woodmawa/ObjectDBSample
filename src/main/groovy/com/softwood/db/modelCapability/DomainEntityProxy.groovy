package com.softwood.db.modelCapability

import com.softwood.db.Session
import groovy.util.logging.Slf4j

import javax.persistence.EntityManager
import javax.persistence.FlushModeType
import javax.persistence.Persistence
import javax.persistence.PersistenceUtil

enum DomainProxyType {
    Class, Instance
}

@Slf4j
class DomainEntityProxy extends groovy.util.Proxy {

    String className
    Session session
    private String proxyTypeName
    private Class proxyType
    PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil()

    static GormClass gormTemplate = new GormClass()

    static GormClass getGormTemplate () {
        gormTemplate
    }

    String getEntityClassType() {
        className
    }

    DomainEntityProxy(session, adaptee) {
        this.session = session
        if (adaptee instanceof Class) {
            className = adaptee.simpleName
            proxyTypeName = DomainProxyType.Class
            proxyType = adaptee as Class
            enhanceDomainClass (proxyType)
        }
        else {
            className = adaptee.getClass().getSimpleName()  //its an instance
            proxyTypeName = DomainProxyType.Instance
            proxyType = adaptee.getClass()
            enhanceInstanceMetaClass (adaptee)

        }

        wrap (adaptee)

    }

    private def enhanceDomainClass (clazz) {
        //todo - needs to be cleverer
        if (clazz.hasProperty ("isGormEnhanced") && clazz.isGormEnhanced() ) {
            //already enhanced
            return null
        }

        List proxyClassMetaMethods = proxyType.metaClass.methods
        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement
            !(it.name.contains('$getLookup') ||
                    it.name.contains ("MetaClass") ||
                    it.name.contains ("toString") ||
                    it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - proxyClassMetaMethods
        ExpandoMetaClass emc = new ExpandoMetaClass (proxyType, true, true)

        diff2.each {
            if (it.isStatic()) {
                Closure closRef = GormClass::"$it.name"
                closRef = closRef.rehydrate(proxyType, getGormTemplate(), null)
                //log.debug "adding gorm static method '$it.name()' to domain class $clazz metaClass"
                emc.registerStaticMethod(it.name, closRef)
            }
        }

        emc.isGormEnhanced = true //added property saying we augmented the metaClass
        emc.initialize()
        clazz.setMetaClass (emc)
    }

    private def enhanceInstanceMetaClass(proxy) {
        List proxyClassMetaMethods = proxy.getClass().metaClass.methods
        List proxyInstanceMetaMethods = proxy.metaClass.methods
        List<MetaMethod> diff = proxyInstanceMetaMethods - proxyClassMetaMethods

        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement
            !(it.name.contains('$getLookup') ||
                    it.name.contains ("MetaClass") ||
                    it.name.contains ("toString") ||
                    it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - proxyInstanceMetaMethods

        ExpandoMetaClass emc = new ExpandoMetaClass (proxyType, true, true)
        //add GormClass methods
        diff2.each {
            Closure closRef = GormClass::"$it.name"
            closRef = closRef.rehydrate(proxy, getGormTemplate(), null)
            if (it.isStatic()) {
                //log.debug "adding gorm static method '$it.name()' to proxy metaClass"
                emc.registerStaticMethod(closRef)
            } else {
                //log.debug "adding gorm method '$it.name()' to proxy metaClass"
                emc.registerInstanceMethod(closRef)
            }
        }
        emc.isGormEnhanced = true //added property saying we augmented the metaClass
        emc.initialize()
        proxy.setMetaClass (emc)
    }

    boolean isClass () {
        proxyTypeName == DomainProxyType.Class
    }

    boolean isInstance () {
        proxyTypeName == DomainProxyType.Instance
    }


    long count () {
        EntityManager em = session.getEntityManager()
        em.flush()
        String clazzString = getEntityClassType()
        javax.persistence.TypedQuery query = em.createQuery("SELECT count(r) FROM  ${clazzString} r")
        long count = (long) query.getSingleResult()

    }

    def hardDeleteAll () {
        String clazzString = getEntityClassType()

        session.withTransaction {
            session.getEntityManager().createQuery("DELETE FROM ${clazzString}").executeUpdate() as long
        }
    }



    def newInstance (args) {
        if (args)
            getAdaptee()::new (args)
        else
            getAdaptee()::new ()
    }

    def methodMissing (String name, args) {
        if (name == 'new') {
            //return proxy for new instance of class being proxied
            return getAdaptee()::new (args)
        } else if (name == 'newProxy') {
            return new DomainEntityProxy (getAdaptee()::new (args))
        }
        if (adaptee.respondsTo (name, args)) {
            adaptee.invokeMethod(name, args)
        } else
            new MissingMethodException(name, this, args) //resolve on metaclass
    }

    boolean isLoaded () {
        if (proxyTypeName == DomainProxyType.Instance)
            persistenceUtil.isLoaded(adaptee)
        else
            false
    }

    boolean isFieldLoaded (String fieldName) {
        if (proxyTypeName == DomainProxyType.Instance)
            persistenceUtil.isLoaded(adaptee, fieldName)
        else
            false
    }


    def save (FlushModeType flushMode = FlushModeType.COMMIT) {
        log.debug "proxy save():  use session.save() "
        session.save(adaptee)
     }

    long delete (FlushModeType flushMode = FlushModeType.COMMIT) {
        log.debug "proxy delete():  use session.delete() "
        session.delete(adaptee)
    }

}
