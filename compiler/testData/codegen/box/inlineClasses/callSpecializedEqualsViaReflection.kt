// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.InvocationTargetException

inline class Simple(val x: String) {
    fun somethingWeird() {}
}

fun box(): String {
    var s = ""
    val name = "equals--impl"
    val specializedEquals =
        Class.forName("Simple\$Erased").getDeclaredMethod("equals--impl", String::class.java, String::class.java)
            ?: return "$name not found"

    try {
        specializedEquals.invoke(null, "a", "b")
    } catch (e: InvocationTargetException) {
        return if (e.targetException is NullPointerException) "OK" else "${e.targetException}"
    }

    return "Reserved method invoked successfully"
}
