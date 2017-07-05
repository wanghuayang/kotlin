
// FILE: A.java
public class A {
    public void connect(@kotlin.internal.ParameterName(name = "host") String host, @kotlin.internal.ParameterName(name = "port") int port) {
    }
}

// FILE: test.kt
fun main() {
    val test = A()
    test.connect("127.0.0.1", 8080)
    test.connect(host = "127.0.0.1", port = 8080)
    test.connect(port = 8080, host = "127.0.0.1")
}