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
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class AccordionManagerBehavior extends AbstractYuiBehavior {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    //Provide a more generic approach by making the function call variable as well
    private static final PackagedTextTemplate INIT = new PackagedTextTemplate(AccordionManagerBehavior.class, "init.js");
    
    private DynamicTextTemplate template;
    private String id;

    public AccordionManagerBehavior(IYuiManager manager, AccordionConfiguration accordionSettings) {
        super(manager);
        
        this.template = new DynamicTextTemplate(INIT);
        template.setConfiguration(accordionSettings);
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        template.setId(component.getMarkupId());
        this.id = component.getMarkupId();
    }
    
    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addModule(HippoNamespace.NS, "accordionmanager");
        helper.addTemplate(template);
    }

    public IBehavior newSection() {
        return new AccordionSectionBehavior(id);
    }
    
}
