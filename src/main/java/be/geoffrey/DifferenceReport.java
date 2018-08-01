package be.geoffrey;

import com.predic8.soamodel.Difference;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.WSDLParser;
import com.predic8.wsdl.diff.WsdlDiffGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class DifferenceReport {

    private final String rootOne;
    private final String rootTwo;

    private StringCollectionDifferences fileDifferences;

    public DifferenceReport(String rootOne,
                            String rootTwo) {
        this.rootOne = rootOne;
        this.rootTwo = rootTwo;
    }

    public void init() throws IOException {
        List<String> filesInOne = Files.walk(Paths.get(rootOne))
                .filter(Files::isRegularFile)
                .map(Path::toAbsolutePath)
                .filter(f -> f.toString().endsWith(".wsdl"))
                .map(p -> p.toString().substring(rootOne.length()))
                .collect(Collectors.toList());

        List<String> filesInTwo = Files.walk(Paths.get(rootTwo))
                .filter(Files::isRegularFile)
                .map(Path::toAbsolutePath)
                .filter(f -> f.toString().endsWith(".wsdl"))
                .map(p -> p.toString().substring(rootTwo.length()))
                .collect(Collectors.toList());


        this.fileDifferences = new StringCollectionDifferences(filesInOne, filesInTwo);
    }

    public void diffCommonServices(String nameFilter) {

        WSDLParser parser = new WSDLParser();

        for (String fileName : fileDifferences.getCommon()) {
            if (nameFilter == null || fileName.contains(nameFilter)) {
                List<Difference> diffs = listDiffOfWsdl(parser, rootOne + fileName, rootTwo + fileName);
                diffs.forEach(diff -> {
                    System.out.println(diff.dump());
                });
            }
        }
    }

    private List<Difference> listDiffOfWsdl(WSDLParser parser, String one, String two) {

        // parse both wsdl documents
        Definitions wsdl1 = parser.parse(one);
        Definitions wsdl2 = parser.parse(two);

        // compare the wsdl documents
        WsdlDiffGenerator diffGen = new WsdlDiffGenerator(wsdl1, wsdl2);
        return diffGen.compare();
    }

    @Override
    public String toString() {
        return "" +
                "Change Report\n" +
                "========================================\n" +
                "\n" +
                "New Services (" + fileDifferences.getNewItems().size() + ")" + "\n" +
                "----------------------------------------\n" +
                String.join("\n", fileDifferences.getNewItems()) + "\n" +
                "\n" +
                "Removed Services (" + fileDifferences.getRemovedItems().size() + ")\n" +
                "----------------------------------------\n" +
                String.join("\n", fileDifferences.getRemovedItems()) + "\n" +
                "\n";
    }
}
