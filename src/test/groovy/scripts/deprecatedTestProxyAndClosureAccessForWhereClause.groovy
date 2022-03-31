package scripts

import groovy.transform.TypeChecked

class DeprecatedDomain {
    String name = "William"

    def someWork () {
        println "\t->Domains, someWork(): $name"
    }
}

trait DeprecatedGormTrait {
    def where (@DelegatesTo(DeprecatedGormTrait) Closure closure) {
        println ">>from gorm trait running closure "
        Closure clos = closure.clone()
        clos.delegate = this
        def result = clos()
        result
    }

    def and (@DelegatesTo (DeprecatedGormTrait) expr) {
        println "eval ${(expr)}"
        this
    }
}


class DeprecatedGormClass {
    def where (Closure closure) {  //@DelegatesTo(GormClass)
        println ">>from gorm trait running closure "
        Closure clos = closure.clone()
        clos.delegate = this
        def result = clos()
        result
    }

    def and ( expr) {  //@DelegatesTo (GormClass)
        println "eval ${expr}"
        this
    }
}


//get before with traits enhancement
DeprecatedDomain instance = new DeprecatedDomain()

instance.metaClass.thingy = {-> println "thingy called  "}
instance.metaClass.val = "any added value"

DeprecatedDomain instance2 = new DeprecatedDomain()

List l = instance.metaClass.methods.collect {it.name}

class DeprecatedDomainProxy extends groovy.util.Proxy {

    DeprecatedDomainProxy(proxy) {  //@DelegatesTo (Domain)
        List origMethods = proxy.metaClass.methods.collect{it.name}
        List diff = proxy.metaClass.methods - proxy.getClass().metaClass.methods

        List gormClassMM = DeprecatedGormClass.metaClass.methods.findAll {!(it.name.contains ("MetaClass") ) && !(it.name.contains('$get'))}
        List domMM = proxy.metaClass.methods
        List diff2 = gormClassMM - domMM


        diff

        this.adaptee = proxy  //.withTraits Gorm  //proxy and enhanced adaptee

        //withTraits doesnt transfer any meta methods from proxy.metaClass - so do this now
        ExpandoMetaClass emc = new ExpandoMetaClass(adaptee.getClass(),true, true)
        diff.each {
            emc.registerInstanceMethod(it)}
        //diff2.each {it.setMetaClass(emc)
            //emc.registerInstanceMethod(it) }  //add in the GormClass metaMethods
       emc.initialize()
        adaptee.setMetaClass(emc)

        adaptee.metaClass.where = { DeprecatedGormClass::where }
        adaptee.metaClass.and = {DeprecatedGormClass::and }

        List metaMethods = adaptee.metaClass.methods.collect{it.name}
        metaMethods

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
        adaptee.where (closure.clone())
        this
    }


}

/*class Dummy {
    def where (@DelegatesTo (Domain) Closure clos) {
        clos()
    }
}
Dummy dummy = new Dummy()

instance2.metaClass.where = (Dummy::where).rehydrate(instance2 , dummy, dummy)   //augment inst2 with borrowed trait method
*/
//instance2.where { println "instance 2 with borrowed trait method "}


//enhance Class with some traits
def EnhancedDomainClass = DeprecatedDomain.withTraits DeprecatedGormTrait
DeprecatedDomain instance2FromEnhDomainClass = EnhancedDomainClass.newInstance()

Proxy dp = new DeprecatedDomainProxy(instance)
//instance = dp.newInstance()
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
//instance2FromEnhDomainClass.where {println "\t "} will fail

List ldpproxy =  dp.adaptee.getMetaClass().getMethods().collect {it.name}

dp.adaptee.thingy() // {println "hello in ldpproxy "}
println "val from dp " + dp.adaptee.val

dp.where {
    boolean result = name == 'William'
    someWork()
    thingy()
    println val
    and (10)
    println "\tgorm adapted proxy in where closure where name is $name, own:$owner delegate:$delegate"
}




