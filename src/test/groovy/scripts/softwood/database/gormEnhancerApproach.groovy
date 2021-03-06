package scripts.softwood.database

import com.softwood.db.modelCapability.OrmEnhancer
import com.softwood.model.Customer
import org.codehaus.groovy.runtime.MethodClosure

import java.lang.reflect.Method

//summariser method
def augmentedMethodsSummary (MetaClass origMetaClass, MetaClass instanceMetaClass, MetaClass enhMetaClass, MetaClass inhInstanceMetaClass) {
    List origClassMM = origMetaClass.methods.collect{(it.isStatic() ? "(static)" : "") + it.name}
    List origInstanceMM = instanceMetaClass.methods.collect{(it.isStatic() ? "(static)" : "") +  it.name}

    List enhClassMM = enhMetaClass.methods.collect{(it.isStatic() ? "(static)" : "") + it.name}
    List enhInstanceMM = inhInstanceMetaClass.methods.collect{(it.isStatic() ? "(static)" : "") + it.name}

    println "enhanced Class methods : " + (enhClassMM - origClassMM).sort()
    println "enhanced inst methods : " + (enhInstanceMM - origInstanceMM).sort()
    "done"
}


String meth = '$static_methodMissing'

class Temp {
    def method () {
        println "declared method is invoked "
    }
}
Temp.metaClass.dyn = 10

Closure missing = {String name, Object args ->
    println "method $name is not declared on the class with args $args"
}
ExpandoMetaClass emc = Temp.metaClass
emc.registerStaticMethod (meth, missing, String, Object)
/*Temp.metaClass.'static'."$meth" = {String name, Object arg ->
    println "method $name is not declared on the class "
}*/

Temp.hi()
MetaMethod  tempMethMissing = Temp.metaClass.getMetaMethod(meth, String, Object)
println "temp Missing method signature " + tempMethMissing.getSignature()

MetaClass origClassMC = Customer.metaClass //Class metaClass

Customer cust1 = new Customer (name:"Goldman Sachs")
MetaClass origCustMC = cust1.metaClass  //instance metaClass
//assert origCustMC instanceof MetaClassImpl //wrapped by HandleMetaClass proxy
List origClassMM = origClassMC.methods.collect {(it.isStatic() ? "(static)" : "") + it.name}

Class EnhancedCustomer = OrmEnhancer.enhance (Customer)
println "Class $EnhancedCustomer is orm enhanced " + EnhancedCustomer.isOrmEnhanced()

assert Customer.metaClass instanceof ExpandoMetaClass

Customer cust2 = new Customer (name:"Wells Fargo")
MetaClass encMetaClass = cust2.metaClass

boolean isEnhanced = cust2.metaClass.respondsTo(cust2, 'isOrmEnhanced')
List enhClassMM = encMetaClass.methods.collect {(it.isStatic() ? "(static)" : "") + it.name}
List diff = enhClassMM - origClassMM


boolean canCount = EnhancedCustomer.metaClass.respondsTo (EnhancedCustomer, 'count')
if (canCount) {
    def numCust = EnhancedCustomer.count()
    println "\t->> count of customer records is $numCust"
}

Customer enhancedCustomer = OrmEnhancer.enhance (cust1)
Customer enhancedCustomer2 = OrmEnhancer.enhance (cust2)

println "instance  $enhancedCustomer is orm enhanced " + enhancedCustomer.isOrmEnhanced()
println "instance  $enhancedCustomer2 is orm enhanced " + enhancedCustomer2.isOrmEnhanced()

MethodClosure mc = Customer::getById

MetaMethod byIdMM = Customer.metaClass.methods.find {it.name == 'getById'}
println "getById sig is " + byIdMM.getSignature()

def result = byIdMM.invoke (Customer, 1 )


def res = mc.call(2)
println "got $result on mm.invoke(1)"
println "got $res on mc.call(2)"

//Method m = Customer.getMethod('getById', Object)
//m.invoke(Customer, 3 )
MetaMethod mm = Customer.metaClass.getMetaMethod('getById', Object)
mm.invoke(Customer, 3 )


def  mmiss = Customer.metaClass.respondsTo (Customer, meth)
println mmiss
println mmiss.size()
assert mmiss
assert mmiss[0].isStatic()

MetaMethod  customerMethMissing = Temp.metaClass.getMetaMethod(meth, String, Object)
println "Customer Missing method signature " + tempMethMissing.getSignature()

Customer.metaClass.invokeMethod(Customer, meth, "hi", 1)

def c = Customer.getById ( 4 as Object )
println "got $c using Customer.getById (3) " //bullshit!!
//augmentedMethodsSummary (origClassMC, origCustMC, EnhancedCustomer.metaClass, enhancedCustomer.metaClass)