package be.geoffrey.xmlweave.ui;

import be.geoffrey.xmlweave.core.usecase.Element;
import be.geoffrey.xmlweave.core.usecase.XmlWeaveService;
import be.geoffrey.xmlweave.service.XmlRenderer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.util.Optional;

@Route("")
public class GenerationView extends VerticalLayout {

    private final XmlWeaveService xmlWeaveService;
    private final XmlRenderer xmlRenderer;

    public GenerationView(XmlWeaveService xmlWeaveService,
                          XmlRenderer xmlRenderer) {

        this.xmlWeaveService = xmlWeaveService;
        this.xmlRenderer = xmlRenderer;

        buildUI();
    }

    private void buildUI() {

        TextField fileField = new TextField("File location:");
        fileField.setValue("C:\\projects\\roots\\mowesb-fuse\\edelta-connector\\src\\main\\resources\\META-INF\\wsdl\\v18\\Aanbieden\\GeefOpdrachtDienst-05.00\\GeefOpdrachtWsResponse.xsd");
        fileField.setSizeFull();

        TextArea generatedCode = new TextArea();
        generatedCode.setSizeFull();
        generatedCode.setEnabled(false);
        generatedCode.setHeight("600px");

        TextField rootElementBox = new TextField("Root Element");
        rootElementBox.setValue("GeefOpdrachtWsResponse");

        Button button = new Button("Generate XML");
        button.addClickListener(e -> {
            Optional<Element> representation = xmlWeaveService.getRepresentation(new File(fileField.getValue()), rootElementBox.getValue());

            if (representation.isPresent()) {
                generatedCode.setValue(xmlRenderer.renderAsXml(representation.get()));
            } else {
                generatedCode.setValue("Nothing useful can be rendered.");
            }
        });

        add(new H1("XMLWeave"), fileField, rootElementBox, button, generatedCode);
    }
}
