package org.onehippo.cms7.essentials.components.gui.panel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class TemplatePanel extends Panel {


    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TemplatePanel.class);
    private final TextArea<String> textArea;
    private final CheckBox checkBox;
    private boolean checkboxModel;
    private String textModel;
    private final WebMarkupContainer container;

    public TemplatePanel(final String id, final String title, final Form<?> form) {
        super(id);

        container = new WebMarkupContainer("myContainer");
        textArea = new TextArea<>("textArea", new PropertyModel<String>(this, "textModel"));
        checkBox = new CheckBox("checkBox", new PropertyModel<Boolean>(this, "checkboxModel"));
        final Label label = new Label("title", title);
        //############################################
        // SETUP
        //############################################
        textArea.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);
        container.setOutputMarkupId(true);
        container.add(textArea);
        container.add(checkBox);
        container.add(label);
        add(container);
        form.add(this);

    }



    public void setTextModel(final AjaxRequestTarget target, final String model){
        textModel = model;
        textArea.modelChanged();
        target.add(textArea);
    }

    public void show(final AjaxRequestTarget target) {
        container.setVisible(true);
        if (target != null) {
            target.add(container);
        }
    }

    public void hide(final AjaxRequestTarget target) {
        container.setVisible(false);
        if (target != null) {
            target.add(container);
        }
    }

    public boolean isCheckboxModel() {
        return checkboxModel;
    }

    public void setCheckboxModel(final boolean checkboxModel) {
        this.checkboxModel = checkboxModel;
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public String getTextModel() {
        return textModel;
    }

    public void setTextModel(final String textModel) {
        this.textModel = textModel;
    }

    public TextArea<String> getTextArea() {
        return textArea;
    }
}
