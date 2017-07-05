// FILE: A.java
import kotlin.internal.*;


public class A {
    public void foo(@ParameterName(name = "hello") String world) {}
}

// FILE: B.kt

class B : A() {
    override fun foo(hello: String) {}
}

// FILE: C.kt

class C : A() {
}

