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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.editor.layout.ILayoutControl;
import org.hippoecm.frontend.editor.layout.ILayoutPad;
import org.hippoecm.frontend.editor.layout.ILayoutTransition;
import org.hippoecm.frontend.editor.layout.JavaLayoutPad;
import org.hippoecm.frontend.editor.layout.ListItemLayoutControl;
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
        private List<ListItemLayoutControl> controls = new LinkedList<ListItemLayoutControl>();

        public ItemTracker(String wicketId) {
            super(ILayoutAware.class);
            this.wicketId = wicketId;
        }

        @Override
        protected void onServiceAdded(ILayoutAware service, String name) {
            ItemPad pad = new ItemPad(controls);
            ListItemLayoutControl control = new ListItemLayoutControl(getBuilderContext(), service, pad, wicketId,
                    controls);
            pad.control = control;
            controls.add(control);

            service.setLayoutControl(control);
        }

        @Override
        protected void onRemoveService(ILayoutAware service, String name) {
            service.setLayoutControl(null);

            Iterator<ListItemLayoutControl> controlIter = controls.iterator();
            while (controlIter.hasNext()) {
                ListItemLayoutControl control = controlIter.next();
                if (control.getService() == service) {
                    controlIter.remove();
                    break;
                }
            }
        }

    }

    private class ItemPad extends JavaLayoutPad {
        private static final long serialVersionUID = 1L;

        List<ListItemLayoutControl> controls;
        ILayoutControl control;

        public ItemPad(List<ListItemLayoutControl> controls) {
            super("item");
            this.controls = controls;
        }

        @Override
        public List<String> getTransitions() {
            List<String> transitions = new LinkedList<String>();
            if (controls.indexOf(control) > 0) {
                transitions.add("up");
            }
            if (controls.indexOf(control) < controls.size() - 1) {
                transitions.add("down");
            }
            return transitions;
        }

        @Override
        public ILayoutTransition getTransition(String name) {
            if ("up".equals(name)) {
                return new ILayoutTransition() {
                    private static final long serialVersionUID = 1L;

                    public String getName() {
                        return "up";
                    }

                    public ILayoutPad getTarget() {
                        ListItemLayoutControl previous = controls.get(controls.indexOf(control) - 1);
                        return previous.getLayoutPad();
                    }

                };
            } else if ("down".equals(name)) {
                return new ILayoutTransition() {
                    private static final long serialVersionUID = 1L;

                    public String getName() {
                        return "down";
                    }

                    public ILayoutPad getTarget() {
                        ListItemLayoutControl next = controls.get(controls.indexOf(control) + 1);
                        return next.getLayoutPad();
                    }

                };
            } else {
                throw new RuntimeException("Unknown transition " + name);
            }
        }
    }

}
