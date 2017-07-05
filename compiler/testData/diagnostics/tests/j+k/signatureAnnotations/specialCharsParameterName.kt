// FILE: A.java
import kotlin.internal.*;

public class A {
    public void dollarName(@ParameterName(name = "$") String host) {
    }

    public void numberName(@ParameterName(name = "42") String field) {
    }
}

// FILE: test.kt
fun main() {
    val test = A()
    test.dollarName(`$` = "hello")
    test.dollarName("hello")
    test.dollarName(<!NAMED_PARAMETER_NOT_FOUND!>host<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>

    test.numberName(`42` = "world")
    test.numberName("world")
    test.numberName(<!NAMED_PARAMETER_NOT_FOUND!>field<!> = "world"<!NO_VALUE_FOR_PARAMETER!>)<!>
}