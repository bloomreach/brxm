/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.addon.workflow;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.StringResourceModel;

abstract class StdWorkflow extends ActionDescription {
    private static final long serialVersionUID = 1L;
    
    private String name;

    StdWorkflow(String id, String name) {
        super(id);
        this.name = name;

        add(new ActionDisplay("text") {
            @Override
            protected void initialize() {
                MenuLink link;
                add(link = new MenuLink("text") {
                    @Override
                    public void onClick() {
                        execute();
                    }
                });
                link.add(new Label("label", getTitle()));
            }
        });

        add(new ActionDisplay("icon") {
            @Override
            protected void initialize() {
                ResourceReference model = getIcon();
                add(new Image("icon", model));
            }
        });

        add(new ActionDisplay("panel") {
            @Override
            protected void initialize() {
            }
        });
    }
    
    protected final String getName() {
        return name;
    }

    protected StringResourceModel getTitle() {
        return new StringResourceModel(getName(), this, null, getName());
    }

    protected ResourceReference getIcon() {
        return new ResourceReference(getClass(), "workflow-16.png");
    }

    protected abstract void execute();
}
