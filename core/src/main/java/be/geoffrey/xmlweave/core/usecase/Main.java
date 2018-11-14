package be.geoffrey.xmlweave.core.usecase;

import com.geoffrey.xmlweave.xmlschema.Schema;

import javax.xml.bind.JAXB;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        Schema schema = JAXB.unmarshal(new File("C:\\projects\\personal\\experiments\\comparewsdl\\core\\src\\test\\resources\\1_simple_element_with_basic_childs.xsd"), Schema.class);

        System.out.println("HMM..");
    }
}
