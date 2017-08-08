// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> myRun(@CalledInPlace block: () -> T): T = block()

fun foo(x: Int): Int = x + 1

fun typeMismatchInLambda(y: String) {
    val x = myRun { foo(y) }
}