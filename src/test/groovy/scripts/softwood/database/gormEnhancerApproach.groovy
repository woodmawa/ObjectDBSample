package scripts.softwood.database

import com.softwood.db.modelCapability.GormEnhancer
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

Class EnhancedCustomer = GormEnhancer.of (Customer)
println "Class $EnhancedCustomer is gorm enhanced " + EnhancedCustomer.isGormEnhanced()

assert Customer.metaClass instanceof ExpandoMetaClass

Customer cust2 = new Customer (name:"Wells Fargo")
MetaClass encMetaClass = cust2.metaClass

boolean isEnhanced = cust2.metaClass.respondsTo(cust2, 'isGormEnhanced')
List enhClassMM = encMetaClass.methods.collect {(it.isStatic() ? "(static)" : "") + it.name}
List diff = enhClassMM - origClassMM


boolean canCount = EnhancedCustomer.metaClass.respondsTo (EnhancedCustomer, 'count')
Closure cnt = EnhancedCustomer::count
if (canCount) {
    def numCust = EnhancedCustomer.count()
    //def numCust = cnt.call()
    println "\t->> count of customer records is $numCust"
}

Customer enhancedCustomer = GormEnhancer.of (cust1)

println "instance  $enhancedCustomer is gorm enhanced " + enhancedCustomer.isGormEnhanced()


augmentedMethodsSummary (origClassMC, origCustMC, EnhancedCustomer.metaClass, enhancedCustomer.metaClass)