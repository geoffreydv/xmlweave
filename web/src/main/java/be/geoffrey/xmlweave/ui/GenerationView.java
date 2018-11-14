package be.geoffrey.xmlweave.ui;

import be.geoffrey.xmlweave.core.usecase.XmlWeaveService;
import be.geoffrey.xmlweave.core.usecase.ElementRepresentation;
import be.geoffrey.xmlweave.core.usecase.choice.ElementChoice;
import be.geoffrey.xmlweave.service.XmlRenderer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

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
        fileField.setValue("C:\\projects\\personal\\experiments\\comparewsdl\\core\\src\\test\\resources\\2_simple_elements.xsd");
        fileField.setSizeFull();

        TextArea generatedCode = new TextArea();
        generatedCode.setSizeFull();
        generatedCode.setEnabled(false);
        generatedCode.setHeight("600px");

        TextField rootElementBox = new TextField("Root Element");

        Button button = new Button("Generate XML");
        button.addClickListener(e -> {
            Optional<ElementRepresentation> representation = xmlWeaveService.getRepresentation(new File(fileField.getValue()), rootElementBox.getValue());

            if (representation.isPresent()) {
                generatedCode.setValue(xmlRenderer.renderAsXml(representation.get()));
            } else {
                generatedCode.setValue("Nothing useful can be rendered.");
            }
        });

        add(new H1("XMLWeave"), fileField, rootElementBox, button, generatedCode);
    }
}
