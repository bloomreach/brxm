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

package org.hippoecm.frontend.plugins.yui.tree;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public abstract class TreeBehavior extends AbstractYuiAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    //Provide a more generic approach by making the function call variable as well
    private static final PackagedTextTemplate INIT_TREE = new PackagedTextTemplate(TreeBehavior.class, "init_tree.js");

    protected final TreeSettings settings;
    private HippoTextTemplate template;

    public TreeBehavior(IYuiManager service, TreeSettings settings) {
        super(service, settings);
        this.settings = settings;
        this.template = new HippoTextTemplate(INIT_TREE, getClientClassname()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getWidgetId();
            }

            @Override
            public YuiObject getSettings() {
                return TreeBehavior.this.settings;
            }
        };
    }

    protected String getWidgetId() {
        return getComponent().getMarkupId();
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addCssReference(new ResourceReference(TreeBehavior.class, "mytree.css"));
        context.addModule(TreeNamespace.NS, "treemanager");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.TreeManager.onLoad()");
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
        return "YAHOO.hippo.HippoTree";
    }

}
