package be.geoffrey;

import com.predic8.soamodel.Difference;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.WSDLParser;
import com.predic8.wsdl.diff.WsdlDiffGenerator;

import java.util.List;

public class WsdlDifferences {

    public static void main(String[] args) {

        WSDLParser parser = new WSDLParser();

        Definitions oldVersion = parser.parse("C:\\Users\\geoff\\Desktop\\edelta wsdl compare\\v16\\Aanbieden\\GeefOpdrachtDienst-05.00\\GeefOpdrachtWs.wsdl");
        Definitions newVersion = parser.parse("C:\\Users\\geoff\\Desktop\\edelta wsdl compare\\v18\\Aanbieden\\GeefOpdrachtDienst-05.00\\GeefOpdrachtWs.wsdl");
        WsdlDiffGenerator diffGen = new WsdlDiffGenerator(newVersion, oldVersion);
        List<Difference> lst = diffGen.compare();

        for (Difference difference : lst) {
            String dump = difference.dump();
            System.out.println(dump);
        }

        System.out.println("YIHAR");
    }

}
