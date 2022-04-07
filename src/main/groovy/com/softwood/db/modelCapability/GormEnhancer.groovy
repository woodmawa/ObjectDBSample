package com.softwood.db.modelCapability

import com.softwood.db.Database
import groovy.util.logging.Slf4j

import javax.persistence.EntityManager
import javax.persistence.metamodel.EntityType
import javax.persistence.metamodel.Metamodel

@Slf4j
class GormEnhancer {
    private static GormClass gormTemplate = new GormClass()

    private static GormClass getGormTemplate () {
        gormTemplate
    }


    static def enhance(object) {

        return enhanceMetaClass (object)

    }

    private static def enhanceMetaClass(object) {
        if (object.respondsTo ("isGormEnhanced") && object.isGormEnhanced() ) {
            //already enhanced
            return object

        }

        def type = object.getClass()
        def clazz = (type == Class) ? object : object.getClass()

        //if class is not known, this will register to the Metamodel
        checkClassRegistration(clazz)

        List clazzMetaMethods = clazz.metaClass.methods
        List objectInstanceMetaMethods = object.metaClass.methods
        List<MetaMethod> diff = objectInstanceMetaMethods - clazzMetaMethods

        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement process
            !(it.name.contains('$getLookup') ||
                    it.name.contains ("MetaClass") ||
                    it.name.contains ("enhance") ||
                    it.name.contains ("isGormEnhanced") ||
                    it.name.contains ("toString") ||
                    it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - clazzMetaMethods

        ExpandoMetaClass emc = new ExpandoMetaClass (clazz, true, true)
        //add GormClass methods
        diff2.each {
            Closure closRef = GormClass::"$it.name"
            //closRef = closRef.rehydrate(getGormTemplate(),object , null)
            if (it.isStatic()) {
                //log.debug "adding gorm (static) method '$it.name()' to instance metaClass"
                emc.registerStaticMethod(it.name, {closRef.call(object)} )
            } else {
                //log.debug "adding gorm method '$it.name()' to instance metaClass"
                emc.registerInstanceMethod(it.name, {closRef.call(object)} )
            }
        }

        emc.registerStaticMethod("isGormEnhanced", { true })  //added property saying we augmented the metaClass
        emc.initialize()
        object.setMetaClass (emc)
        object
    }

    private static def enhanceDomainClass (Class domainClazz) {
        if (domainClazz.hasProperty ("isGormEnhanced") && domainClazz.isGormEnhanced() ) {
            //already enhanced
            return domainClazz
        }

        List proxyClassMetaMethods = domainClazz.metaClass.methods
        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement
            !(it.name.contains('$getLookup') ||
              it.name.contains ("MetaClass") ||
              it.name.contains ("of") ||
              it.name.contains ("isGormEnhanced") ||
              it.name.contains ("toString") ||
              it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - proxyClassMetaMethods
        ExpandoMetaClass emc = new ExpandoMetaClass (domainClazz, true, true)

        diff2.each {
            if (it.isStatic()) {
                Closure closRef = GormClass::"$it.name"
                log.debug "adding gorm static method '$it.name()' to domain class $domainClazz metaClass"
                emc.registerStaticMethod(it.name, {closRef.call(domainClazz)})
            }
        }

        emc.registerStaticMethod("isGormEnhanced", {true})  //added property saying we augmented the metaClass
        emc.initialize()
        domainClazz.setMetaClass (emc)
        domainClazz
    }

    private static def enhanceInstanceMetaClass(instance) {
        if (instance.hasProperty ("isGormEnhanced") && instance.isGormEnhanced() ) {
            //already enhanced
            return instance

        }

        List proxyClassMetaMethods = instance.getClass().metaClass.methods
        List proxyInstanceMetaMethods = instance.metaClass.methods
        List<MetaMethod> diff = proxyInstanceMetaMethods - proxyClassMetaMethods

        List gormTemplateMetaMethods = getGormTemplate().metaClass.methods.findAll{
            //exclude certain methods from enhancement process
            !(it.name.contains('$getLookup') ||
              it.name.contains ("MetaClass") ||
              it.name.contains ("of") ||
              it.name.contains ("isGormEnhanced") ||
              it.name.contains ("toString") ||
              it.name.contains ("GormMethods")
            )
        }

        List diff2 = gormTemplateMetaMethods - proxyInstanceMetaMethods

        ExpandoMetaClass emc = new ExpandoMetaClass (instance.getClass(), true, true)
        //add GormClass methods
        diff2.each {
            Closure closRef = GormClass::"$it.name"
            closRef = closRef.rehydrate(getGormTemplate(),instance , null)
            if (it.isStatic()) {
                log.debug "adding gorm (static) method '$it.name()' to instance metaClass"
                emc.registerStaticMethod(it.name, {closRef.call(instance.getClass())} )
            } else {
                log.debug "adding gorm method '$it.name()' to instance metaClass"
                emc.registerInstanceMethod(it.name, {closRef.call(instance)} )
            }
        }

        emc.registerInstanceMethod("isGormEnhanced", { true })  //added property saying we augmented the metaClass
        emc.initialize()
        instance.setMetaClass (emc)
        instance
    }

    /*
     * checks class against the metamodel and adds it if its missing
     */
    static EntityType checkClassRegistration (Class delegateClazz) {

        String clsName = delegateClazz.simpleName
        Metamodel metaModel = Database.getEmf().getMetamodel()

        Set<EntityType> entTypes = metaModel.getEntities()
        EntityType isRegisteredClass = entTypes.find {it.getName() == clsName}
        EntityType et
        if (!isRegisteredClass) {
            log.debug "registering class $delegateClazz to database known types"
            et = metaModel.entity(delegateClazz)
        }
        isRegisteredClass ?: et
    }
}
