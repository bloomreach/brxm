package org.hippoecm.frontend.widgets;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;

public class RadioGroupWidget extends Panel {
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new radio group widget.
     * 
     * @param id 
     *      The widget id
     * @param choices 
     *      List containing {@link Radio} model objects
     * @param model the model
     *      Model that represents selected {@link Radio} item  
     *      
     */
    public RadioGroupWidget(String id, List choices, IModel model) {
        super(id);

        final RadioGroup group = new RadioGroup("widget", model);
        group.setRenderBodyOnly(false);

        group.add(new ListView("choices", choices) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {

                final Serializable radioitem = (Serializable) item.getModelObject();
                final Radio radio = new Radio("radio", new Model(radioitem));

                radio.add(new AjaxEventBehavior("onchange") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        group.processInput();
                        onChange(target, group.getModelObject());
                    }

                    @Override
                    protected CharSequence getEventHandler() {
                        return generateCallbackScript(new AppendingStringBuffer("wicketAjaxPost('").append(
                                getCallbackUrl()).append(
                                "', wicketSerialize(document.getElementById('" + radio.getMarkupId() + "'))"));
                    }
                });
                item.add(radio);

                String label = item.getModelObjectAsString();
                radio.setLabel(new Model(getLocalizer().getString(label, this, label)));
                item.add(new SimpleFormComponentLabel("label", radio));

                RadioGroupWidget.this.populateItem(item);
            }
        });

        add(group);
    }

    /**
     * Override this method to change the ListItem
     * 
     * @param item
     */
    protected void populateItem(ListItem item) {
    }

    /**
     * Override this method to handle the onChange event of the {@link RadioGroup}
     * @param target
     *          The request target
     * @param object
     *          Object held by the selected {@link Radio} component 
     */
    protected void onChange(AjaxRequestTarget target, Object object) {
    }

}
