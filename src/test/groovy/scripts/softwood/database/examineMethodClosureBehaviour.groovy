package scripts.softwood.database

import com.softwood.runtime.WillsMethodClosure
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod

ExpandoMetaClass.enableGlobally()  //setup for expandMc at the start

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

//you can define a new method as being declared on some class and implemented as a closure, then invoke it
MetaMethod extraMM = new ClosureStaticMetaMethod('extra', Doner, {arg ->println "extra called with [$arg]"}, (Class[])[String] )
extraMM.invoke(Doner, "william")

class Doner {
    int instanceNumber

    static def donerStaticMethod (String arg) {
        "static method() of Doner ($this) is called with arg [$arg]"
    }

    def donerInstanceMethod (String arg) {
        "instance method() of Doner ($this) called with arg [$arg]"
    }

    //wrapped by entity class metaClass extension
    static def doner_save (delegate, args) {
        println "static doner save() received delegate $delegate, and args list [$args]"
        "save() returned : OK"
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

    def recipientInstanceMethod (String arg) {
        "instance method() of Doner ($this) called with arg [$arg]"
    }

    String toString () {
        "Recipient [recipient number : $recipientNumber]"
    }

    static def $static_methodMissing (String name, Object args) {
        println "called static method missing with $name "

    }
}

MethodClosure transferStaticMethod = Doner::donerStaticMethod
println "call doner static method closure " + transferStaticMethod (" try this on transfer closure ")
MethodClosure transferInstanceMethod = Doner::donerInstanceMethod

Doner tempDoner = new Doner (instanceNumber:100)
Recipient tempRecipient  = new Recipient (recipientNumber:200)
transferInstanceMethod (tempDoner, "invoke instance method")
//Closure clonedTransfer = transfer.clone()
//WillsMethodClosure rehydrateTransfer = clonedTransfer.rehydrate(Recipient, Recipient, null )
//String res = rehydrateTransfer (" try this on rehyrated transfer closure  ")
//println "call rehydrated  static method closure " + res

//trigger method missing- this works
Recipient.hi()
ExpandoMetaClass rmc = Recipient.metaClass
rmc.registerStaticMethod('added', transferStaticMethod, (Class[])[String] )
rmc.registerStaticMethod('transferStaticMethod', {String s -> transferStaticMethod(s.toUpperCase()) }, (Class[])[String] )

Closure rmclos = {String s ->
    def thisDelegate = delegate
    transferInstanceMethod (tempDoner, "inside recipient closure - invoke instance method against tempDoner")
    Closure instanceMM = transferInstanceMethod//.rehydrate(delegate, tempRecipient, null)
    println "in rmclos : " + instanceMM (tempDoner /*delegate*/, s.toUpperCase())
}
//rmclos = rmclos.rehydrate(tempRecipient, tempRecipient, null)

Closure rmSaveClos = {args ->
    def thisDelegate = delegate
    MethodClosure saveInstanceMethod = Doner::doner_save
    if (args.size() > 1) {
        //got some sort of array of args here
    } else {
        //got one param
    }

    def result = saveInstanceMethod (thisDelegate, args)
    result
}

//now transfer doner instance method to metaClass (before we create any recipients
rmc.registerInstanceMethod('transferInstanceMethod', rmclos)
rmc.registerInstanceMethod('save', rmSaveClos)

//call transfered static method
//MethodClosure extra = Recipient::added
println "call dyn added : " + Recipient.added ("try this from transfer ")
println "call dyn added : " + Recipient.transferStaticMethod ("transformed arg to uppercase ")

println "call doner rehydrated static method closure " + Recipient.transferStaticMethod (" try this on rehydrated transfer to recipient  ")


println Doner.donerStaticMethod("direct invoke of doner static method")
//can't invoke instance method on Class, will throw an exception
assert shouldFail (MissingMethodException) {println Doner.donerInstanceMethod("hello")}

Doner doner1 = new Doner (instanceNumber:1)
Doner doner2 = new Doner (instanceNumber:2)

println doner1.donerInstanceMethod("hello")  //direct invoke

MethodClosure mc1 = doner1::donerInstanceMethod
println "instance method closure mc1 has param types  " + mc1.parameterTypes

MethodClosure donerClassMc = Doner::donerInstanceMethod
println "Doner class method closure for instance method has param types  " + donerClassMc.parameterTypes


//Closure curry = donerClassMc.curry(Doner)
//println donerClassMc.invokeMethod('instanceMethod', "invoked via class level methodClosure ref ")
//doesnt work from class level
//donerClassMc.invoke(Doner, "invoked via class level methodClosure ref ")


MethodClosure mc2 = doner2::donerInstanceMethod

println mc1("hello1")
println mc2("hello2")


Recipient recip1 = new Recipient(recipientNumber: 1)
Recipient recip2 = new Recipient(recipientNumber: 2)

MetaMethod transferredInstanceMM = recip1.metaClass.getMetaMethod('transferInstanceMethod', (Class[]) [String])
println "transferInstanceMethod mm invoked : " + transferredInstanceMM.invoke(recip1, "invoked a transferred Doner instance method in recipient")

//try rehydrate from doner2 methodClsoure back to doner1
//MethodClosure mc3 = mc2.rehydrate(doner1, doner1, null)
//println mc3 ("rehydrated ")

List<MetaMethod> list = Doner.metaClass.methods.findAll {it.name.contains ("Method")}
Map<String, MetaMethod> mmMap = list.collectEntries{new MapEntry (it.name, it)}
//does work at instance level
println mmMap['donerInstanceMethod'].invoke (doner1, "invoke with doner1")
println mmMap['donerInstanceMethod'].invoke (doner2, "invoke with doner2")

List<MetaMethod> recipList = Recipient.metaClass.methods.findAll {it.name.contains ("Method")}
Map<String, MetaMethod> rmmMap = recipList.collectEntries{new MapEntry (it.name, it)}
//does work at instance level
println rmmMap['transferInstanceMethod'].invoke (recip1, "invoke with recip1")
println rmmMap['transferInstanceMethod'].invoke (recip2, "invoke with recip2")

println " -- try dynamic save() on recipient instance -- "
println recip1.save("instance save() method on recipient called, proxied to doner static save() ")