package com.softwood.db.modelCapability

class GormEnhancer {
    private static GormClass gormTemplate = new GormClass()

    private static GormClass getGormTemplate () {
        gormTemplate
    }


    static def of (object) {
        if (object instanceof Class )
            enhanceDomainClass(object)
        else
            enhanceInstanceMetaClass(object)
    }


    private static def enhanceDomainClass (domainClazz) {
        if (domainClazz.hasProperty ("isGormEnhanced") && domainClazz.isGormEnhanced() ) {
            //already enhanced
            return domainClazz
        }

        List proxyClassMetaMethods = domainClazz.metaClass.methods
        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement
            !(it.name.contains('$getLookup') ||
                    it.name.contains ("MetaClass") ||
                    it.name.contains ("toString") ||
                    it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - proxyClassMetaMethods
        ExpandoMetaClass emc = new ExpandoMetaClass (domainClazz, true, true)

        diff2.each {
            if (it.isStatic()) {
                Closure closRef = GormClass::"$it.name"
                closRef = closRef.rehydrate(domainClazz, getGormTemplate(), null)
                //log.debug "adding gorm static method '$it.name()' to domain class $clazz metaClass"
                emc.registerStaticMethod(it.name, closRef)
            }
        }

        emc.isGormEnhanced = {true} //added property saying we augmented the metaClass
        emc.initialize()
        domainClazz.setMetaClass (emc)
        domainClazz
    }

    private static def enhanceInstanceMetaClass(proxy) {
        if (proxy.hasProperty ("isGormEnhanced") && proxy.isGormEnhanced() ) {
            //already enhanced
            return proxy
        }

        List proxyClassMetaMethods = proxy.getClass().metaClass.methods
        List proxyInstanceMetaMethods = proxy.metaClass.methods
        List<MetaMethod> diff = proxyInstanceMetaMethods - proxyClassMetaMethods

        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement process
            !(it.name.contains('$getLookup') ||
                    it.name.contains ("MetaClass") ||
                    it.name.contains ("toString") ||
                    it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - proxyInstanceMetaMethods

        ExpandoMetaClass emc = new ExpandoMetaClass (proxy.getClass(), true, true)
        //add GormClass methods
        diff2.each {
            Closure closRef = GormClass::"$it.name"
            closRef = closRef.rehydrate(proxy, getGormTemplate(), null)
            if (it.isStatic()) {
                //log.debug "adding gorm static method '$it.name()' to proxy metaClass"
                emc.registerStaticMethod(it.name, closRef)
            } else {
                //log.debug "adding gorm method '$it.name()' to proxy metaClass"
                emc.registerInstanceMethod(it.name, closRef)
            }
        }

        emc.isGormEnhanced = {true} //added property saying we augmented the metaClass
        emc.initialize()
        proxy.setMetaClass (emc)
        proxy
    }

}
