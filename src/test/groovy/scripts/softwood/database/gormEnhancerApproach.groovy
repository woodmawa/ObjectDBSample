package scripts.softwood.database

import com.softwood.db.modelCapability.OrmEnhancer
import com.softwood.model.Customer

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


//augmentedMethodsSummary (origClassMC, origCustMC, EnhancedCustomer.metaClass, enhancedCustomer.metaClass)