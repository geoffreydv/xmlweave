package be.geoffrey.xmlweave.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.UUID;

@Route("")
public class GenerationView extends VerticalLayout {

    public GenerationView() {

        TextField fileField = new TextField("File location:");
        fileField.setValue("C:\\projects\\personal\\experiments\\comparewsdl\\core\\src\\test\\resources\\2_simple_elements.xsd");
        fileField.setSizeFull();

        TextArea generatedCode = new TextArea();
        generatedCode.setSizeFull();
        generatedCode.setEnabled(false);
        generatedCode.setHeight("600px");

        Button button = new Button("Generate XML");
        button.addClickListener(e -> {
            generatedCode.setValue(UUID.randomUUID().toString());
        });

        add(new H1("XMLWeave"), fileField, button, generatedCode);
    }
}
