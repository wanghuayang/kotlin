// FULL_JDK
// SKIP_IN_RUNTIME_TEST
package test

import java.util.AbstractList

public open class ModalityOfFakeOverrides : AbstractList<String>() {
    override fun get(index: Int): String {
        return ""
    }

    override val size: Int get() = 0
}
