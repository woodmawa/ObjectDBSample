package scripts.softwood.database

import com.softwood.db.Database
import com.softwood.db.Session
import com.softwood.db.modelCapability.DomainEntityProxy
import com.softwood.model.Customer

Database.withSession { Session session ->
    Customer cust = new Customer(name: "Barclays")

    DomainEntityProxy dp = new DomainEntityProxy(session, cust)

    def savedCust = dp.save()
    println savedCust
}