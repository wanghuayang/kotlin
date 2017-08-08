// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> inPlace(@CalledInPlace block: () -> T): T = block()

fun reassignmentAndNoInitializaiton() {
    val x: Int
    inPlace { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 42 }
   <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}