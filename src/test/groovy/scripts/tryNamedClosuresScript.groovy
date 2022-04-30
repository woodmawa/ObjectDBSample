package scripts

import com.softwood.utilities.NamedClosure

//NamedClosure nc = new NamedClosure ("will", {it -> println "do this closure [$it]"})
String name = "my Script"

Closure someClos = {it ->
    def thisOwner = owner
    String nm
    if (thisOwner.hasProperty ('name')) {
        //true for Alt class, but not for a script
        nm = thisOwner.name
    } else {
        Binding binding = getBinding()

        nm = "property 'name' not defined for closure context ${owner.getClass()}"
    }
    println "'name' was $nm"
    println "\tclos called with arg [$it] (name:${-> nm}) (owner:$owner)"
}
print "direct call of closure from script with 0 >> "
someClos(0)

class Alt {
    String name
}
Alt alt1 = new Alt(name:'named alt1')
Alt alt2 = new Alt(name:'named alt2')

Closure alt1Clos = someClos.rehydrate(alt1,alt1, null)
Closure alt2Clos = someClos.rehydrate(alt2,alt2,null)

alt1Clos (1)
alt2Clos (2)

/*
NamedClosure nc = ['will', someClos]
print "then invoke named closure with 2 >> "
nc(2)
*/
