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
package org.hippoecm.frontend.editor.builder;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.editor.layout.ILayoutContext;
import org.hippoecm.frontend.editor.layout.ILayoutPad;
import org.hippoecm.frontend.editor.layout.ILayoutTransition;
import org.hippoecm.frontend.editor.layout.ListItemLayoutContext;
import org.hippoecm.frontend.editor.layout.ListItemPad;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;

public class ListViewPluginEditorPlugin extends RenderPluginEditorPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ItemTracker itemTracker;

    public ListViewPluginEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // disable remove link
        get("head:remove").setVisible(false);
    }

    @Override
    protected Iterator<ILayoutTransition> getTransitionIterator() {
        List<ILayoutTransition> list = Collections.emptyList();
        return list.iterator();
    }

    @Override
    public String getTemplateBuilderExtensionPoint() {
        return getBuilderContext().getEditablePluginConfig().getString("item");
    }

    @Override
    public List<ILayoutAware> getChildren() {
        List<ILayoutAware> children = new LinkedList<ILayoutAware>(super.getChildren());
        if (itemTracker != null) {
            children.addAll(itemTracker.getItems());
        }
        return children;
    }

    @Override
    protected void registerChildTrackers() {
        super.registerChildTrackers();

        IPluginConfig config = getBuilderContext().getEditablePluginConfig();
        itemTracker = new ItemTracker(config.getString("item"));
        getPluginContext().registerTracker(itemTracker, getEffectivePluginConfig().getString("item"));
    }

    @Override
    protected void unregisterChildTrackers() {
        if (itemTracker != null) {
            getPluginContext().unregisterTracker(itemTracker, getEffectivePluginConfig().getString("item"));
            itemTracker = null;
        }
        super.unregisterChildTrackers();
    }

    protected class ItemTracker extends ServiceTracker<ILayoutAware> {
        private static final long serialVersionUID = 1L;

        private String wicketId;
        private List<ListItemLayoutContext> controls = new LinkedList<ListItemLayoutContext>();

        public ItemTracker(String wicketId) {
            super(ILayoutAware.class);
            this.wicketId = wicketId;
        }

        @Override
        protected void onServiceAdded(ILayoutAware service, String name) {
            ListItemPad pad;
            ILayoutContext layoutControl = getLayoutContext();
            if (layoutControl != null) {
                pad = new ListItemPad(controls, layoutControl.getLayoutPad());
            } else {
                String variant = getVariation();
                if (variant == null || "".equals(variant)) {
                    pad = new ListItemPad(controls, ILayoutPad.Orientation.VERTICAL);
                } else {
                    pad = new ListItemPad(controls, ILayoutPad.Orientation.HORIZONTAL);
                }
            }
            ListItemLayoutContext control = new ListItemLayoutContext(getBuilderContext(), service, pad, wicketId,
                    controls);
            pad.setLayoutControl(control);
            controls.add(control);

            service.setLayoutContext(control);
        }

        @Override
        protected void onRemoveService(ILayoutAware service, String name) {
            service.setLayoutContext(null);

            Iterator<ListItemLayoutContext> controlIter = controls.iterator();
            while (controlIter.hasNext()) {
                ListItemLayoutContext control = controlIter.next();
                if (control.getService() == service) {
                    controlIter.remove();
                    break;
                }
            }
        }

        private List<ILayoutAware> getItems() {
            List<ILayoutAware> services = new LinkedList<ILayoutAware>();
            for (ListItemLayoutContext control : controls) {
                services.add(control.getService());
            }
            return services;
        }
    }

}
