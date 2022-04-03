package scripts

import groovy.transform.TypeChecked
import org.codehaus.groovy.reflection.CachedClass
import org.codehaus.groovy.runtime.MethodClosure

class DeprecatedDomain {
    String name = "default name: William"

    def someWork () {
        println "\t->Domains, someWork(): $name"
    }

    String toString() {
        "DeprecatedDomain ([$name])"
    }
}

trait DeprecatedGormTrait {
    def where (@DelegatesTo(DeprecatedGormTrait) Closure closure) {
        println ">>from gormTrait.where() running closure "
        Closure clos = closure.clone()
        clos.delegate = this
        def result = clos()
        result
    }

    def and ( expr) {
        println "gromTrait and(): eval ${(expr)}"
        this
    }
}


class DeprecatedGormClass {
    def where (@DelegatesTo (DeprecatedGormClass) Closure closure) {  //@DelegatesTo(GormClass)
        println ">>from GormClass.where() running closure "
        Closure clos = closure.clone()
        //clos.delegate = this
        def result = clos()
        result
    }

    def and ( expr) {  //@DelegatesTo (GormClass)
        println "GormClass.and(): eval ${expr}"
        this
    }

    String toString() {
        "GormClass [@${(Long.toHexString( this.hashCode() ))}]"
    }
}

class DeprecatedDomainProxy extends groovy.util.Proxy {

    //create instance for rehydration purposes
    static DeprecatedGormClass gorm = new DeprecatedGormClass()
    static DeprecatedGormClass getGorm () {gorm }

    DeprecatedDomainProxy(proxy) {  //@DelegatesTo (Domain)
        List origMethods = proxy.metaClass.methods.collect{it.name}

        //see if any additions were made to per instance metaClass
        List diff = proxy.metaClass.methods - proxy.getClass().metaClass.methods

        List gormClassMM = DeprecatedGormClass.metaClass.methods
        List gormClassMMExcludedNames = gormClassMM.findAll {!(it.name.contains ("MetaClass") || it.name.contains('$get') || it.name == 'toString' )}
        List domMM = proxy.metaClass.methods
        List diff2 = gormClassMMExcludedNames - domMM


        diff

        this.adaptee = proxy  //.withTraits Gorm  //proxy and enhanced adaptee

        //withTraits doesnt transfer any meta methods from proxy.metaClass - so do this now
        ExpandoMetaClass emc = new ExpandoMetaClass(adaptee.getClass(),true, true)
        diff.each {
            println "\t domain proxy constructor() is adding meta method $it.name to emc"
            emc.registerInstanceMethod(it)}
        diff2.each {
            println "\t\t domain proxy constructor() is adding gormClass meta method $it.name to emc"
            Closure closRef = DeprecatedGormClass::"$it.name"
            closRef = closRef.rehydrate(proxy, getGorm(),null)
            emc.registerInstanceMethod(it.name, closRef)
        }  //add in the GormClass metaMethods
        emc.initialize()
        adaptee.setMetaClass(emc)

        List finalMetaMethods = adaptee.metaClass.methods.collect{it.name}
        finalMetaMethods

    }

    def newInstance() {
        //return an new adaptee,  enhanced proxy instance
        adaptee::new().withTraits DeprecatedGormTrait
    }

    @TypeChecked
    def getProperty ( String name) {  //@DelegatesTo (Domain)
        //force rely of properties to adaptee
        def local = getAdaptee()
         if (local.hasProperty(name)) {
            return local.metaClass.getProperty(local, name)
         } else if (name == "adaptee") {
             return local
         }
         else {
            throw new MissingPropertyException ("property $name missing on proxy")
        }
    }

    def where (@DelegatesTo (DeprecatedDomain) Closure closure) {  //
        //relay where call to proxy, with Gorm enhancement trait
        Closure work = closure.clone()
        //rehydrate (delegate, owner, thisOwner)
        work = work.rehydrate(adaptee, gorm, null)
        work.setResolveStrategy(Closure.DELEGATE_FIRST) //try adaptee first, then gorm

        println "domainProxy.where(), delegates to $gorm where to run closure "
        MethodClosure gormWhere = gorm::where as MethodClosure
        //not required - adapteeWhere.delegate = adaptee
        gormWhere (work)  //using gorms.where(work)
    }


}

//update the master class metaClass
DeprecatedDomain.metaClass.classLevelThingy = {-> println "class level thingy() called from $delegate "}
DeprecatedDomain.metaClass.val = "any added value"

DeprecatedDomain instance = new DeprecatedDomain(name:"instance")
instance.metaClass.perInstanceThingy = {-> println "per instance thingy called from $delegate "}

List l = instance.metaClass.methods.collect {it.name}
instance.classLevelThingy()
instance.perInstanceThingy()

DeprecatedDomain instance1 = new DeprecatedDomain(name:"instance1")
instance1.metaClass.perInstanceThingy = {-> println "per instance thingy() called from $delegate "}
instance1.classLevelThingy()
instance1.perInstanceThingy()

//enhance Class with some traits, should still have class level thingy()
println "\n -- Enhanced domain class using GormTrait and create inst2 from enh domain class --"
def EnhancedDomainClass = DeprecatedDomain.withTraits DeprecatedGormTrait
DeprecatedDomain instance2FromEnhDomainClass = EnhancedDomainClass.newInstance()
instance2FromEnhDomainClass.name = "inst2 from enhancedDomainClass"
instance2FromEnhDomainClass.metaClass.perInstanceThingy = {-> println "per instance thingy() called from $delegate "}

instance2FromEnhDomainClass.classLevelThingy()
instance2FromEnhDomainClass.perInstanceThingy()

println "\n  -- now create proxy from instance3  --"
DeprecatedDomain instance3 = new DeprecatedDomain(name:"instance3")
groovy.util.Proxy dp = new DeprecatedDomainProxy(instance3)
def res =instance.metaClass.respondsTo (DeprecatedDomain, "someWork")

List lclassenh =  EnhancedDomainClass.metaClass.methods.collect {it.name}
List lenhinst =  instance2FromEnhDomainClass.metaClass.methods.collect {it.name}
List ldp =  dp.getMetaClass().getMethods().collect {it.name}

//println "enhanced domain object responds to 'where' " + instance.respondsTo ('someWork', [])


dp.someWork()  //proxy method call works, proxy property access doesnt work, e.g. dp.name will throw exception
def name = dp.name  //proxied property access
def val = dp.val

def domInstance = instance2FromEnhDomainClass.withTraits DeprecatedGormTrait
List ldominst =  domInstance.metaClass.methods.collect {it.name}

domInstance.where {println "\tin where closure "}  //works
EnhancedDomainClass.where {println "\tenh domain class, in where closure"}  //available on enhanced class, but no on instances from it
//instance2FromEnhDomainClass.where {println "\tinstance of enh domain class, in where closure"}

List ldpproxy =  dp.adaptee.getMetaClass().getMethods().collect {it.name}

dp.adaptee.classLevelThingy() // {println "hello in ldpproxy "}
println "val from dp " + dp.adaptee.val

def whereResult = dp.where {
    boolean result = name == 'instance3'
    someWork()
    classLevelThingy()
    //perInstanceThingy() - not declared on instance3
    println val
    and (10)  //uses gorm class owners and
    println "\tgorm adapted proxy in dp.where() with closure context name is $name, own:$owner delegate:$delegate"
    "all done"
}

println "-> proxies where(Closure) call  returned $whereResult"

println "\n -- now setup proxy dp2 from 'enh domain class instance2' and try that  \n"

DeprecatedDomainProxy dp2 = new DeprecatedDomainProxy(instance2FromEnhDomainClass)
whereResult = dp2.where {
    someWork()
    classLevelThingy()    //from adjusted metaClass change on DomainClass
    perInstanceThingy()
    println val
    and (10)  //uses gorm class owners and
    println "\tgorm adapted proxy in dp2.where() with closure context name is $name, own:$owner delegate:$delegate"
    "all done"
}






