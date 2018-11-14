import be.geoffrey.xmlweave.core.usecase.ElementRepresentation;
import be.geoffrey.xmlweave.service.XmlRenderer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlRenderingTest {

    @Test
    public void simpleRendering() {

        XmlRenderer renderer = new XmlRenderer();
        assertThat(renderer.renderAsXml(new ElementRepresentation("Abc"))).isEqualTo("<Abc />");
        assertThat(renderer.renderAsXml(new ElementRepresentation("Element"))).isEqualTo("<Element />");
    }
}
