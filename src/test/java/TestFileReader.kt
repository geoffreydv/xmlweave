import java.io.File

object TestFileReader {

    fun readTestFile(name: String): File {
        val classLoader = TestFileReader::class.java.classLoader
        return File(classLoader.getResource(name)!!.file)
    }

}
