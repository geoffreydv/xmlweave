package be.geoffrey.xmlweave.core.usecase;

import java.io.File;

public class TestFileReader {

    public static File readTestFile(String name) {
        ClassLoader classLoader = TestFileReader.class.getClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }

}
