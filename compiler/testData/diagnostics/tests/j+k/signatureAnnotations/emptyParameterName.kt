// FILE: A.java

class A {
    public void emptyName(@kotlin.internal.ParameterName(name = "") String first, @kotlin.internal.ParamterName("ok") int second) {
    }

    public void missingName(@kotlin.internal.ParameterName() String first) {
    }

    public void numberName(@kotlin.internal.ParameterName(name = 42) String first) {
    }
}

// FILE: test.kt
fun main() {
    val test = A()
    test.emptyName("first", 42)
    test.emptyName("first", <!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>ok<!> = 42<!NO_VALUE_FOR_PARAMETER!>)<!>

    test.missingName(<!NAMED_ARGUMENTS_NOT_ALLOWED!>`first`<!> = "arg")
    test.missingName("arg")

    test.numberName("first")
}