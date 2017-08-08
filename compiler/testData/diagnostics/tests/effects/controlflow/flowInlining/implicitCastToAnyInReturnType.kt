// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> myRun(@CalledInPlace block: () -> T): T = block()

fun functionWithSideEffects(x: Int): Int = x + 1 // ...and some other useful side-effects

fun implicitCastWithIf(s: String) {
    myRun { if (s == "") functionWithSideEffects(42) else println("Not empty!") }
}