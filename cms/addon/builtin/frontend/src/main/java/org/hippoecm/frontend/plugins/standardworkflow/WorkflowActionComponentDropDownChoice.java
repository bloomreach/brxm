package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class WorkflowActionComponentDropDownChoice extends DropDownChoice {
    private static final long serialVersionUID = 1L;
    
    private WorkflowActionComponent selected;
    
    public WorkflowActionComponentDropDownChoice(String id, Map<String, ? extends WorkflowActionComponent> items) {
        super(id);
        setModel(new PropertyModel(this, "selected"));
        setChoices(createModel(items));
        setChoiceRenderer(createRenderer());

        add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (selected != null) {
                    selected.getAction().execute();
                }
            }
        });
    }

    @Override
    protected boolean wantOnSelectionChangedNotifications() {
        return true;
    }
    
    private IModel createModel(final Map<String, ? extends WorkflowActionComponent> items) {
        IModel model = new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                List<WorkflowActionComponent> list = new ArrayList<WorkflowActionComponent>(items.size());
                for(Entry<String, ? extends WorkflowActionComponent> entry : items.entrySet()) {
                    if(entry.getValue().getAction().isEnabled()) {
                        list.add(entry.getValue());
                    }
                }
                return list;
            }

            public void setObject(Object object) {
            }

            public void detach() {
            }

        };
        return model;
    }
    
    private IChoiceRenderer createRenderer() {
        IChoiceRenderer renderer = new IChoiceRenderer() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(Object object) {
                WorkflowActionComponent c = (WorkflowActionComponent) object;
                return c.getLabel();
            }

            public String getIdValue(Object object, int index) {
                return String.valueOf(index);
            }

        };
        return renderer;
    }
   
}
    
    
    
