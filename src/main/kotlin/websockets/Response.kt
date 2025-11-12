package websockets

public class Response {
    private lateinit var content: String
    private var counter: Int

    constructor() {
        this.content = "" // Scanner vacío por defecto
        this.counter = 0
    }

    constructor(content: String, counter: Int) {
        this.content = content
        this.counter = counter
    }

    fun getContent(): String = content

    fun getCounter(): Int = counter
}
