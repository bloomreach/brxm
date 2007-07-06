package org.hippocms.repository.webapp.node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

public class NewPropertyDialogPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private String name;
    private String value;

    public NewPropertyDialogPanel(String id) {
        super(id);

        AjaxEditableLabel name = new AjaxEditableLabel("name", new PropertyModel(this, "name"));
        add(name);

        AjaxEditableLabel value = new AjaxEditableMultiLineLabel("value", new PropertyModel(this, "value"));
        add(value);

        add(new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
            }
        });
        
        add(new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
            }
        });

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}

//
//AjaxLink addLink = new AjaxLink("save") {
//  private static final long serialVersionUID = 1L;
//
//  public void onClick(AjaxRequestTarget target) {
//      NodeEditor form = (NodeEditor) findParent(NodeEditor.class);
//      Node node = form.getNode();
//      try {
//          node.setProperty("aap", "noot");
//          node.save();
//      } catch (RepositoryException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
//      target.addComponent(form);
//  }
//};
//add(addLink);
