// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER

import kotlin.internal.*

fun <T> myRun(@CalledInPlace block: () -> T): T = block()

fun throwInLambda(): Int {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>x<!> =<!> myRun { throw java.lang.IllegalArgumentException() }
    <!UNREACHABLE_CODE!>return x<!>
}