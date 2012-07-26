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
package org.hippoecm.frontend.plugins.yui.accordion;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;

public class AccordionSectionBehavior extends AbstractBehavior {
    private static final long serialVersionUID = 1L;


    Component c;
    String parentId;

    public AccordionSectionBehavior(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public void bind(Component component) {
        c = component;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        if (c.isVisible()) {
            response.renderOnDomReadyJavascript("YAHOO.hippo.AccordionManager.render('" + parentId + "', '"
                    + c.getMarkupId() + "')");
        }
    };

}
