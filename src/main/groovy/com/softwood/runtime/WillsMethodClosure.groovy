package com.softwood.runtime


import groovy.lang.Closure;
import groovy.lang.MetaMethod
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MethodClosure;

import java.util.List;


/**
 * Represents a method on an object using a closure which can be invoked
 * at any time
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class WillsMethodClosure extends Closure {

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    private String method;



    public WillsMethodClosure(MethodClosure mc)  {
        //Object owner, String method
        super(mc.owner);
        this.method = mc.method;

        final Class clazz = owner.getClass()==Class.class?(Class) owner:owner.getClass();

        super.maximumNumberOfParameters = 0;
        super.parameterTypes = EMPTY_CLASS_ARRAY;

        List<MetaMethod> methods = InvokerHelper.getMetaClass(clazz).respondsTo(owner, method);

        for(MetaMethod m : methods) {
            if (m.getParameterTypes().length > maximumNumberOfParameters) {
                Class[] pt = m.getNativeParameterTypes();
                super.maximumNumberOfParameters = pt.length;
                super.parameterTypes = pt;
            }
        }
    }

    public String getMethod() {
        return method;
    }

    protected Object doCall(Object arguments) {
        def owner = getOwner()
        return InvokerHelper.invokeMethod(owner, method, arguments)
    }

    public Object getProperty(String property) {
        if ("method".equals(property)) {
            return getMethod();
        } else  return super.getProperty(property);
    }
}
