package net.ticketsbot.patreonproxy.config

import org.json.JSONObject
import java.io.*
import java.util.stream.Collectors

abstract class JSON(private val folder: File? = null) {

    private lateinit var cfile: File
    lateinit var config: JSONConfiguration

    @Throws(IOException::class)
    fun load() {
        if(folder != null && !folder.exists()) folder.mkdir()

        cfile =
            if(folder == null) File(javaClass.getAnnotation(FileName::class.java).fileName)
            else File(folder, javaClass.getAnnotation(FileName::class.java).fileName)

        if(!cfile.exists())
            createFileFromResource(cfile, javaClass.getAnnotation(FileName::class.java).fileName)

        val json = cfile.bufferedReader().readLines().joinToString(" ")
        config = JSONConfiguration(JSONObject(json))
    }

    @Throws(IOException::class)
    private fun createFileFromResource(f: File, resource: String) {
        f.createNewFile()
        val `is` = this::class.java.getResourceAsStream("/$resource")
        val writer = PrintWriter(f, "UTF-8")
        writer.println(BufferedReader(InputStreamReader(`is`, Charsets.UTF_8)).lines().collect(Collectors.joining("\n")))
        writer.close()
        `is`.close()
    }

    @Throws(IOException::class)
    fun save() {
        cfile.bufferedWriter().use { writer ->
            writer.write(config.getRawJSON(2))
        }
    }
}