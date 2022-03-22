package scripts

import com.softwood.com.softwood.db.modelCapability.DomainEntityProxy
import com.softwood.model.Customer

trait MyStaticTraits {
    abstract static String getEntityName ()
    abstract String getName ()


    static void hello() {
        println "hello [${this.simpleName}] [${getName()}"
    }

    static void hello3() {
        String ent = getEntityName()
        println "hello3 [${this.simpleName}] [${ent}]"
    }


     void hello2() {
        println "hello2 [${this.class.simpleName}], [${getName()}]"
    }

}

class SomeClass {
    //implement abstract trait method
    String getName() {
        this.class.simpleName
    }
    static String getEntityName() {
        def name = this.simpleName
        name
    }
}

SomeClass some = new SomeClass()

DomainEntityProxy proxy  = new DomainEntityProxy (SomeClass)


println " proxy for class ${proxy.getEntityClassName()}"
println "proxy record count ${proxy.count()}"

 Closure clos = {



     def classWithTraits = SomeClass.withTraits(MyStaticTraits)
     def instWithTraits = some.withTraits(MyStaticTraits)

     def customerClassWithTraits = Customer.withTraits ( MyStaticTraits)  //Persistence,

     println "customer is class : ${Customer.getEntityName()}"


     classWithTraits.hello()
     instWithTraits.hello2()

     classWithTraits.hello3()

     customerClassWithTraits.hello3()

 }

clos()