package scripts.softwood.database

import org.codehaus.groovy.runtime.MethodClosure

boolean shouldFail ( throwable, Closure code) {
    try {
        code.call ()
    } catch (Throwable ex) {
        if (ex.class ==  throwable) {
            true
        } else {
            throw ex
        }
    }
}

class Doner {
    int instanceNumber

    static def donerStaticMethod (String arg) {
        "static method() of Doner ($this) is called with arg [$arg]"
    }

    def instanceMethod (String arg) {
        "instance method() of Doner ($this) called with arg [$arg]"
    }

    String toString () {
        "Doner [doner inst num : $instanceNumber]"
    }
}

class Recipient {
    int recipientNumber

    static def recipientStaticMethod (String arg) {
        "static method() of Doner ($this) is called with arg [$arg]"
    }

    def instanceMethod (String arg) {
        "instance method() of Doner ($this) called with arg [$arg]"
    }

    String toString () {
        "Doner [recip num : $recipientNumber]"
    }

    static def $static_methodMissing (String name, Object args) {
        println "called static method missing with $name "

    }
}
MethodClosure transfer = Doner::donerStaticMethod
println "call doner static method closure " + transfer (" try this on transfer ")

MethodClosure rehydrateTransfer = transfer.rehydrate(Recipient, Recipient, null )


Recipient.metaClass.'static'.change = { }

//trigger method missing- this works
Recipient.hi()
ExpandoMetaClass rmc = Recipient.metaClass
rmc.registerStaticMethod('added', {"hello - added $it"} )
rmc.registerStaticMethod('transfer', rehydrateTransfer )

//call transfered static method
//MethodClosure extra = Recipient::added
println "call dyn added : " + Recipient.added ("try this from transfer ")


println "call doner rehydrated static method closure " + Recipient.transfer (" try this on rehydrated transfer to recipient  ")


println Doner.staticMethod()
assert shouldFail (MissingMethodException) {println Doner.instanceMethod("hello")}

Doner doner1 = new Doner (instanceNumber:1)
Doner doner2 = new Doner (instanceNumber:2)

println doner1.instanceMethod("hello")

MethodClosure mc1 = doner1::instanceMethod
println "instance method closure mc1 has param types  " + mc1.parameterTypes

MethodClosure donerClassMc = Doner::instanceMethod
println "Doner class method closure for instance method has param types  " + donerClassMc.parameterTypes


//Closure curry = donerClassMc.curry(Doner)
//println donerClassMc.invokeMethod('instanceMethod', "invoked via class level methodClosure ref ")
//doesnt work from class level
//donerClassMc.invoke(Doner, "invoked via class level methodClosure ref ")


MethodClosure mc2 = doner2::instanceMethod

println mc1("hello1")
println mc2("hello2")

//try rehydrate from doner2 methodClsoure back to doner1
MethodClosure mc3 = mc2.rehydrate(doner1, doner1, null)
println mc3 ("rehydrated ")

List<MetaMethod> list = Doner.metaClass.methods.findAll {it.name.contains ("Method")}
Map<String, MetaMethod> mmMap = list.collectEntries{new MapEntry (it.name, it)}
//does work at instance level
println mmMap['instanceMethod'].invoke (doner1, "invoke with doner1")
println mmMap['instanceMethod'].invoke (doner2, "invoke with doner2")