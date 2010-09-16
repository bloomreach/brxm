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
package org.hippoecm.frontend.plugins.cms.browse;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;

import javax.jcr.Node;
import java.util.Iterator;

public class BrowserPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final JcrNodeModel NULL_MODEL = new JcrNodeModel((Node)null);

    private TabsPlugin tabs;
    private IExpandableCollapsable listing;

    private IModelReference modelReference;
    private IModel previousSelection;

    private final WireframeBehavior wireframe;
    private boolean clientOverride = false;

    public BrowserPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        addExtensionPoint("top");
        addExtensionPoint("center");
        addExtensionPoint("left");

        context.registerTracker(new ServiceTracker<RenderService>(RenderService.class) {

            @Override
            protected void onServiceAdded(RenderService service, String name) {
                super.onServiceAdded(service, name);
                if(service instanceof IExpandableCollapsable) {
                    listing = (IExpandableCollapsable)service;
                }

                if(listing == null || !listing.isSupported()) {
                    wireframe.collapseAll();
                } else {
                    checkExpandDefault();
                }
            }

            @Override
            protected void onRemoveService(RenderService service, String name) {
                super.onRemoveService(service, name);
                
                listing = null;
                previousSelection = null;
            }
        }, "service.browse.list");

        context.registerTracker(new ServiceTracker<RenderService>(RenderService.class) {
            @Override
            protected void onServiceAdded(RenderService service, String name) {
                super.onServiceAdded(service, name);
                if(service instanceof TabsPlugin) {
                    tabs = (TabsPlugin) service;
                }
            }

            @Override
            protected void onRemoveService(RenderService service, String name) {
                super.onRemoveService(service, name);
                tabs = null;
            }
        }, "service.browse.editor");

        context.registerTracker(new ServiceTracker<IModelReference>(IModelReference.class) {

            IObserver observer;

            @Override
            protected void onServiceAdded(final IModelReference service, String name) {
                modelReference = service;

                if (observer == null) {
                    context.registerService(observer = new IObserver() {

                        public IObservable getObservable() {
                            return service;
                        }

                        public void onEvent(Iterator events) {
                            //Prevent calling toggle twice in a single request: we will end up here after
                            //the onToggle override below sets the documentModel to null to remove the selected
                            //state from the doclisting
                            if (service != null) {
                                if(!service.getModel().equals(NULL_MODEL)) {
                                    wireframe.collapseAll();
                                } else {
                                    checkExpandDefault();
                                }
                            }

                        }
                    }, IObserver.class.getName());
                }
                super.onServiceAdded(service, name);
            }

            @Override
            protected void onRemoveService(IModelReference service, String name) {
                super.onRemoveService(service, name);
                if (observer != null) {
                    context.unregisterService(observer, IObserver.class.getName());
                    observer = null;
                }
                modelReference = null;
            }
        }, config.getString("model.document"));

        // register as the IRenderService for the browser service
        String browserId = config.getString("browser.id");
        context.registerService(this, browserId);

        add(wireframe = new WireframeBehavior(new WireframeSettings(config.getPluginConfig("layout.wireframe"))) {
            @Override
            protected void onToggle(boolean expand, String position) {
                if(listing != null && listing.isSupported()) {
                    if (expand) {
                        listing.expand();
                    } else {
                        listing.collapse();
                    }
                }

                if (tabs != null) {
                    if (expand) {
                        tabs.hide();
                    } else {
                        tabs.show();
                    }
                }

                if(modelReference != null) {
                    if (expand) {
                        previousSelection = modelReference.getModel();
                        modelReference.setModel(NULL_MODEL);
                    } else if (previousSelection != null && modelReference.getModel().equals(
                            NULL_MODEL)) {
                        modelReference.setModel(previousSelection);
                    }
                }
            }

            @Override
            protected void onExpandDefault() {
                if (listing != null && listing.isSupported()) {
                    listing.expand();
                }
            }

            @Override
            protected void onToggleFromClient(String position, boolean expand) {
                clientOverride = !expand;
            }
        });
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        return new ResourceReference(BrowserPerspective.class, "browser-perspective-" + type.getSize() + ".png");
    }
    
    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        checkExpandDefault();
    }

    private void checkExpandDefault() {
        if (!clientOverride && tabs != null && !tabs.hasOpenTabs() && listing != null && listing.isSupported() && wireframe != null) {
            wireframe.expandDefault();
        }
    }

    @Override
    public void detachModels() {
        super.detachModels();

        if(previousSelection != null) {
            previousSelection.detach();
        }
    }
}
