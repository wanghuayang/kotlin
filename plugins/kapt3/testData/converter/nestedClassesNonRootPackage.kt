// FILE: test/JavaClass.java
package test;

class JavaClass {
    class Foo {
        class Bar {}
    }
}

// FILE: a.kt
package test

interface IFoo {
    interface IBar {
        annotation class Anno(vararg val value: kotlin.reflect.KClass<*>)

        @Anno(IZoo::class)
        interface IZoo
    }
}

class Experiment {
    annotation class Type

    @Type
    data class Group(s: String)
}

class Foo {
    open class Bar {
        object Zoo
    }
}

@IFoo.IBar.Anno(IFoo.IBar.IZoo::class, Foo.Bar::class)
class Test1(val zoo: Foo.Bar.Zoo) : Foo.Bar(), IFoo.IBar, IFoo.IBar.IZoo {
    fun a(): Thread.State = Thread.State.NEW
    fun b(foo: JavaClass.Foo, bar: JavaClass.Foo.Bar) {}
}