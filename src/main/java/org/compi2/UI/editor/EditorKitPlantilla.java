package org.compi2.UI.editor;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class EditorKitPlantilla extends DefaultEditorKit implements ViewFactory {

    @Override
    public ViewFactory getViewFactory() {
        return this;
    }

    @Override
    public View create(Element elem) {
        return new VistaSintaxis(elem);
    }

    @Override
    public String getContentType() {
        return "text/plantilla-codigo";
    }
}
