package net.ticketsbot.patreonproxy.config

import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception

class JSONConfiguration(val obj: JSONObject) {

    fun getElement(path: String): Any {
        try {
            val parts = path.split(".")
            if (parts.size == 1) return get(parts[0])
            else {
                var cfg = obj
                for ((i, part) in parts.withIndex()) {
                    if (i == parts.size-1) return cfg.get(part)
                    else cfg = cfg.getJSONObject(part)
                }
            }
        } catch(_: JSONException) {}
        throw InvalidJSONPathException()
    }

    private fun get(key: String): Any = obj.get(key)

    inline fun <reified T> getGeneric(key: String, default: T): T {
        try {
            val generic = getElement(key)
            if (generic::class.java == T::class.java) return generic as T
        } catch(_: Exception) {}
        return default
    }

    inline fun <reified T> getGenericOrNull(key: String): T? {
        try {
            val generic = getElement(key)
            if (generic::class.java == T::class.java) return generic as T
        } catch(_: Exception) {}
        return null
    }

    fun getRawJSON(indentFactor: Int = 0): String = obj.toString(indentFactor)

    fun set(key: String, data: Any) {
        val parts = key.split(".")
        var cfg = obj
        if(parts.size == 1) {
            obj.put(key, data)
            return
        }
        else for((i, part) in parts.subList(0, parts.size-1).withIndex()) {
            if(!cfg.has(part) && i != part.length-1) cfg.put(part, JSONObject())
            cfg = cfg.getJSONObject(part)
        }
        cfg.put(parts.last(), data)
    }

    fun contains(key: String): Boolean {
        return try {
            getElement(key)
            true
        } catch(_: InvalidJSONPathException) {
            false
        }
    }

    fun remove(key: String) {
        try {
            val parts = key.split(".")
            if (parts.size == 1) obj.remove(parts[0])
            else {
                var cfg = obj
                for ((i, part) in parts.withIndex()) {
                    if (i == parts.size-1) {
                        cfg.remove(part)
                        break
                    }
                    else cfg = cfg.getJSONObject(part)
                }
            }
        } catch(e: JSONException) {
            throw InvalidJSONPathException()
        }
    }
}