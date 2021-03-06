package scripts.databaseApproach


import org.codehaus.groovy.runtime.MethodClosure
import groovy.util.Proxy

class GormDomainProxy<T> extends Proxy /*implements GormApi*/ {

    Class<T> clazz
    static GormClass gorm = new GormClass()  //alternate source of methods

    GormDomainProxy (ref) {
        if (ref !instanceof Class) {
            adaptee = ref
            clazz = ref.getClass()
        } else {
            clazz = ref
        }
    }

    GormDomainProxy newInstance (args) {
        if (args == null)
            new GormDomainProxy (clazz::newInstance())
        else
            new GormDomainProxy (clazz::newInstance(args))
    }

    //will intercept all property calls on the proxy and redirect to the adaptee
    def getProperty (String name) {
        if (name != "adaptee") {
            //try the delegate
            def proxy = getAdaptee()
            if (proxy == null)
                return null  //todo : probably should through exception when no adaptee set

            if (proxy.hasProperty(name)) {
                proxy.metaClass.getProperty(getAdaptee(), name)
            } else{
                throw new MissingPropertyException ("couldn't find property $name, on the proxy ${getAdaptee()} ")
            }
        }
        else
            getAdaptee()
    }

    def with (@DelegatesTo (DomainClass) Closure closure) {
        Closure script = (Closure) closure.clone()
        script.delegate = getAdaptee()

        script(gorm)
    }

    def methodMissing (String name, args) {
        if (adaptee.respondsTo(name, args)) {
            adaptee.invokeMethod(name, args)
        } else {
            //try gorm as proxy provider
            if (gorm.respondsTo (name, args)) {
                MethodClosure mc = gorm::"$name"
                if (mc) {
                    mc.delegate = adaptee
                    mc.invokeMethod(name, args)
                }
            } else {
                throw new RuntimeException("cant find matching method on proxy or gorm")
            }
        }
    }
}
