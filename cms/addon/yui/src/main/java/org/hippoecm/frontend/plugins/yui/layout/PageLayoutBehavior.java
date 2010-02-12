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
package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.YuiId;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;

public class PageLayoutBehavior extends AbstractYuiAjaxBehavior implements IWireframeService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final PackagedTextTemplate INIT_PAGE = new PackagedTextTemplate(PageLayoutBehavior.class,
            "init_page.js");

    private PageLayoutSettings settings;
    private HippoTextTemplate template;

    public PageLayoutBehavior(final PageLayoutSettings settings) {
        super(settings);
        this.settings = settings;
        template = new HippoTextTemplate(INIT_PAGE, "YAHOO.hippo.GridsRootWireframe") {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return settings.getRootId().getElementId();
            }

            @Override
            public YuiObject getSettings() {
                return settings;
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.LayoutManager.render()");
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
    }

    @Override
    public void detach(Component component) {
        template.detach();
    }

    //Implement IWireframeService
    public YuiId getParentId() {
        return settings.getRootId();
    }

}
