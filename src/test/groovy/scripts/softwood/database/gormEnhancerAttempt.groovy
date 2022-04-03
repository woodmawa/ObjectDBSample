package scripts.softwood.database

import com.softwood.db.modelCapability.GormTrait
import com.softwood.model.Customer

Customer cust = new Customer(name:"mucking about")
List origLcm = cust.metaClass.methods
def enhancedCustomer = cust.withTraits GormTrait

List enhlcm = enhancedCustomer.metaClass.methods
List diff = enhlcm-origLcm

// this works : assert enhancedCustomer.getProxyTarget() == cust
println "enhanced customer self is :  " + enhancedCustomer.self()

println "added methods are " + diff.collect{
    String base = it.isStatic() ? "(static):" : ""
    String name = "${it.name}"
    base+name
}

Customer saved = enhancedCustomer.save()
println saved