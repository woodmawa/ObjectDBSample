package com.softwood.utilities

import groovy.transform.InheritConstructors
import org.codehaus.groovy.runtime.MethodClosure

@InheritConstructors
class NamedClosure<V> extends Closure<V> {
    String name
    //def owner

    NamedClosure (String closName, Closure clos) {
        super (clos.clone())
        super.maximumNumberOfParameters = clos.maximumNumberOfParameters
        super.parameterTypes = clos.parameterTypes
        //Closure rehydrated = clos.rehydrate(this, this, null)

        //this.owner = rehydrated
        //println "NamedClosure constructor, owner:${this.getOwner()} "
        name = closName
        this
    }

    //have to do 1 extra redirect to call to function
    def doCall (args) {
        Closure self = owner

        println "doCall(),  call self "
        self.call (args)

        Closure newSelf = self.rehydrate(this, this, null)

        println "doCall(),  call newSelf "

        newSelf.call(args)
    }

}
