/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class WorkflowActionComponentDropDownChoice extends DropDownChoice {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private WorkflowActionComponent selected;
    
    public WorkflowActionComponentDropDownChoice(String id, List<? extends WorkflowActionComponent> items) {
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
    
    private IModel createModel(final List<? extends WorkflowActionComponent> items) {
        IModel model = new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                List<WorkflowActionComponent> list = new ArrayList<WorkflowActionComponent>(items.size());
                for(WorkflowActionComponent entry : items) {
                    if(entry.getAction().isEnabled()) {
                        list.add(entry);
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
                return c.getLabel().getObject();
            }

            public String getIdValue(Object object, int index) {
                return String.valueOf(index);
            }

        };
        return renderer;
    }
   
}
    
    
    
