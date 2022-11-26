package com.softwood.utilities

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MethodClosure

class WillsMethodClosure extends Closure {

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0]
    protected String methodName
    protected Class definingClass
    protected def methodClosureCalleeInstance
    protected String methodClosureCalleeMethodName
    //protected boolean isStatic = methodClosure.

    /*WillsMethodClosure(Object owner, String name) {
        super(owner)

        definingClass = owner.getClass ()
        calleeInstance = owner
        method = name
    }*/

    WillsMethodClosure (String name, def context, MethodClosure methodClosure ) {
        super (context ?: methodClosure.owner)

        def clazz = methodClosure.owner instanceof Class ? methodClosure.owner : methodClosure.owner.getClass()

        methodClosureCalleeMethodName = methodClosure.@method
        definingClass = clazz
        methodClosureCalleeInstance = methodClosure.owner
        methodName = name

    }

    public String getMethod() {
        return methodName
    }

    protected Object doCall(Object arguments) {

        Object[] args
        if (arguments ) {
            args = (Object[]) { arguments }
        } else {
            args = EMPTY_CLASS_ARRAY
        }

        doCall (args)

    }

    protected Object doCall (Object... args) {

        int remain = maximumNumberOfParameters-1
        Object[] reducedArgs = args[0..<remain]

        def ownerClazz = owner instanceof Class ? owner : owner.getClass()

        if (ownerClazz != definingClass) {
            println "doCall ($reducedArgs), on owner $owner using $methodName()"
            if (owner.metaClass.respondsTo(methodName, *reducedArgs)) {
                return InvokerHelper.invokeMethod(owner, methodName, *reducedArgs)
            } else {
                //do something clever here
                println "doCall ($reducedArgs), proxied call to $methodClosureCalleeInstance using $methodClosureCalleeMethodName()"

                InvokerHelper.invokeMethod(methodClosureCalleeInstance, methodClosureCalleeMethodName, *reducedArgs)
            }
            //return InvokerHelper.invokeMethod(owner, methodName, arguments)
        } else {
            println "doCall ($reducedArgs), on $definingClass using $methodName()"
            InvokerHelper.invokeStaticMethod()
            return InvokerHelper.invokeMethod(methodClosureCalleeInstance, methodClosureCalleeMethodName, *reducedArgs)
        }
    }

    public Object getProperty(String property) {
        if ("method".equals(property)) {
            return getMethod()
        } else
            return super.getProperty(property)
    }
}
