/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

/**
 * Renders a YUI-treeview: http://developer.yahoo.com/yui/treeview/
 */
public abstract class TreeBehavior extends AbstractYuiAjaxBehavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TreeBehavior.class);

    //Provide a more generic approach by making the function call variable as well
    private final PackageTextTemplate INIT_TREE = new PackageTextTemplate(TreeBehavior.class, "init_tree.js");

    protected final TreeSettings settings;
    private HippoTextTemplate template;

    public TreeBehavior(TreeSettings settings) {
        super(settings);
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

        loadTreeData();
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        final RequestCycle requestCycle = RequestCycle.get();

        StringValue action = requestCycle.getRequest().getRequestParameters().getParameterValue("action");
        StringValue uuid = requestCycle.getRequest().getRequestParameters().getParameterValue("UUID");

        if (action.isNull() || uuid.isNull() || uuid.toString().length() == 0) {
            return;
        }

        if (action.toString().equals("click")) {
            onClick(target, uuid.toString());
        } else if (action.toString().equals("dblClick")) {
            onDblClick(target, uuid.toString());
        }
    }

    protected void onDblClick(AjaxRequestTarget target, String uuid) {
    }

    protected void onClick(AjaxRequestTarget target, String uuid) {
    }

    /**
     * Return a JSON object representing the full tree using the TreeItem class
     * 
     * @return JSON object representing tree
     */
    private void loadTreeData() {
        TreeItem root = getRootNode();
        if (root != null) {
            JSONObject results = JSONObject.fromObject(root);
            if (results != null) {
                this.settings.setTreeData(results.toString());
            } else {
                log.warn("TreeItem2JSON conversion gave an empty result, rendering empty tree-widget");
            }
        } else {
            log.warn("No root node found, rendering empty tree-widget");
        }
    }

    protected abstract TreeItem getRootNode();

    protected String getWidgetId() {
        return getComponent().getMarkupId();
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addCssReference(new CssResourceReference(TreeBehavior.class, "mytree.css"));
        context.addModule(TreeNamespace.NS, "treemanager");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.TreeManager.onLoad()");
    }

    /**
     * Determines which javascript class is used
     */
    protected String getClientClassname() {
        return "YAHOO.hippo.HippoTree";
    }

    public static class DefaultTreeItem extends TreeItem {
        private static final long serialVersionUID = 1L;

        private static final String TYPE = "Text";

        public DefaultTreeItem(String label, int numOfChilds) {
            super(label, TYPE, numOfChilds);
        }

        public DefaultTreeItem(String label, String uuid, int numOfChilds) {
            super(label, uuid, TYPE, numOfChilds);
        }

    }

    public static class TreeItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        String label;
        String type;
        String uuid;
        boolean expanded;

        TreeItem[] children;
        int index;
        
        public TreeItem(String label, String type, int numOfChilds) {
            this.label = label;
            this.type = type;
            children = new TreeItem[numOfChilds];
            index = 0;
        }

        public TreeItem(String label, String uuid, String type, int numOfChilds) {
            this.label = label;
            this.type = type;
            this.uuid = uuid;
            children = new TreeItem[numOfChilds];
            index = 0;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public TreeItem[] getChildren() {
            return children;
        }

        public void setChildren(TreeItem[] children) {
            this.children = children;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }
        
        public void addChild(TreeItem item) {
            children[index++] = item;
        }
    }
}
