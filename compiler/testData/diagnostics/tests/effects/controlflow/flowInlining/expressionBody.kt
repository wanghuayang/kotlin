// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> myRun(@CalledInPlace block: () -> T): T = block()

fun functionWithExpressionBody(x: Int) = myRun {
    if (x == 0) return true
    if (x == 1) return false
    return functionWithExpressionBody(x - 2)
}