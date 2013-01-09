/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.autocomplete;

import org.apache.wicket.Component;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;

public abstract class AutoCompleteBehavior extends AbstractYuiAjaxBehavior {

    private static final long serialVersionUID = 1L;

    //Provide a more generic approach by making the function call variable as well
    private final PackagedTextTemplate INIT_AUTOCOMPLETE = new PackagedTextTemplate(AutoCompleteBehavior.class,
            "init_autocomplete.js");

    protected final AutoCompleteSettings settings;
    private HippoTextTemplate template;

    public AutoCompleteBehavior(AutoCompleteSettings settings) {
        super(settings);
        this.settings = settings;
        this.template = new HippoTextTemplate(INIT_AUTOCOMPLETE, getClientClassname()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public YuiObject getSettings() {
                return AutoCompleteBehavior.this.settings;
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "autocompletemanager");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.AutoCompleteManager.onLoad()");
    }

    @Override
    public void detach(Component component) {
        super.detach(component);
        template.detach();
    }

    /**
     * Determines which javascript class is used
     * @return
     */
    protected String getClientClassname() {
        return "YAHOO.hippo.HippoAutoComplete";
    }

}
