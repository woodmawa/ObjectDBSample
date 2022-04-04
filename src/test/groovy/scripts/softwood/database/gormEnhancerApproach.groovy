package scripts.softwood.database

import com.softwood.db.modelCapability.GormEnhancer
import com.softwood.model.Customer

Customer cust = new Customer (name:"Goldman Sachs")


Customer enhancedCustomer = GormEnhancer.of (cust)
println "instance  $enhancedCustomer is gorm enhanced " + enhancedCustomer.isGormEnhanced()

Class EnhancedCustomer = GormEnhancer.of (Customer)
println "Class $EnhancedCustomer is gorm enhanced " + EnhancedCustomer.isGormEnhanced()