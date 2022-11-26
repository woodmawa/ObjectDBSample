package com.softwood.utilities

import groovy.transform.InheritConstructors
import org.codehaus.groovy.runtime.MethodClosure

@InheritConstructors
class NamedClosure<V> extends Closure<V> {
    String name

    NamedClosure (String closName, def context, Closure clos) {
        //invoke super with owner as a rehydrated closure on new context object
        //(owner, thisObject)
        super (clos.rehydrate (context, context, null), context)
        super.maximumNumberOfParameters = clos.maximumNumberOfParameters
        super.parameterTypes = clos.parameterTypes

        name = closName
        this
    }

    //have to do 1 extra redirect to call original anonymous function
    def doCall (args) {
        Closure  self = owner
        if (owner instanceof MethodClosure) {
            MethodClosure mc = owner
            mc.
            println "calling method closure "
        } else if (args)
            self (args)
        else
            self()
    }

}
