package scripts

import com.softwood.utilities.NamedClosure
import com.softwood.utilities.WillsMethodClosure

//store value in script binding
name = "my Script"

Closure someClos = {it ->
    def thisOwner = owner

    // will try and resolve on either script binding, or Alt instance owner
    String nm = "${owner.name}"
    println "\tclos called with arg [$it] (name:$nm) (owner:$owner)"
}
print "direct call of closure from script with 0 >> "
someClos(0)

class Doner {
    static def statMethod () {return "statMethod returns OK"}
}
Doner doner1 = new Doner()

class Alt {
    String name
}
Alt alt1 = new Alt(name:'named alt1')
Alt alt2 = new Alt(name:'named alt2')
Alt alt3 = new Alt(name:'named alt3')

//rehydrate someClosure with new owner and delegates
Closure alt1Clos = someClos.rehydrate(alt1,alt1, null)
Closure alt2Clos = someClos.rehydrate(alt2,alt2,null)

print "call of closure with alt1 as context, with 1 >> "
alt1Clos (1)

print "call of closure with alt2 as context, with 2 >> "
alt2Clos (2)


NamedClosure nc = new NamedClosure ("myClosure", alt3, someClos)
nc(3)

WillsMethodClosure wmc = new WillsMethodClosure ("myClosure", alt3, doner1::statMethod)
println wmc(4,10)
