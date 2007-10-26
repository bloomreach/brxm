/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.perspectives;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;

/**
 * Panel representing the content panel for the first tab.
 */
public class EditPerspective extends Plugin
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Constructor
     * 
     * @param id
     *            component id
     */
    public EditPerspective(String id, JcrNodeModel model)
    {
        super(id, model);
        
        //add(new EditorPlugin("editor", model));
    }

    @Override
    public void update(AjaxRequestTarget target, JcrEvent model) {
        // TODO Auto-generated method stub
        
    }
}


