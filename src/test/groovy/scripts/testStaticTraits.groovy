package scripts

trait MyStaticTraits {
    static void hello() {
        println "hello"
    }
}

class SomeClass /*implements  MyStaticTraits*/ {

}

SomeClass some = new SomeClass()

def classWithTraits = SomeClass.withTraits(MyStaticTraits)



classWithTraits.hello()