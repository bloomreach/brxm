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
package org.hippoecm.frontend.plugins.standards.tabs;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IFocusListener;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.junit.Before;
import org.junit.Test;

public class TabsTest extends PluginTest {

    static final String PANELS = "panels";

    public static class ContentPanel extends RenderPlugin<Void> {
        private static final long serialVersionUID = 1L;

        ITitleDecorator decorator;
        
        public ContentPanel(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(decorator = new ITitleDecorator() {
                private static final long serialVersionUID = 1L;

                @SuppressWarnings("unchecked")
                public IModel<String> getTitle() {
                    return (IModel<String>) getDefaultModel();
                }

                public ResourceReference getIcon(IconSize type) {
                    // TODO Auto-generated method stub
                    return null;
                }

            }, context.getReference(this).getServiceId());
        }

        void reregister() {
            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(this).getServiceId();
            context.unregisterService(decorator, serviceId);
            context.registerService(decorator, serviceId);
        }
    }

    static class ObservableModel implements IModel, IObservable {
        private static final long serialVersionUID = 1L;

        private Object object = null;
        private IObservationContext obContext;
        
        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
            if (obContext != null) {
                notifyObservers();
            }
        }

        public void detach() {
        }

        public void setObservationContext(IObservationContext context) {
            this.obContext = context;
        }

        public void startObservation() {
        }

        public void stopObservation() {
        }
        
        void notifyObservers() {
            EventCollection<IEvent<IObservable>> events = new EventCollection<IEvent<IObservable>>();
            events.add(new IEvent<IObservable>() {

                public IObservable getSource() {
                    return ObservableModel.this;
                }
                
            });
            obContext.notifyObservers(events);
        }
    }
    
    final static String[] content = {
            "/test", "nt:unstructured",
                "/test/plugin", "frontend:pluginconfig",
                    "plugin.class", TabsPlugin.class.getName(),
                    "wicket.id", "service.root",
                    "tabs", PANELS,
            "/config/panel", "frontend:plugincluster",
                "/config/panel/plugin", "frontend:plugin",
                    "plugin.class", ContentPanel.class.getName(),
                    "wicket.id", PANELS,
    };

    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    @Test
    public void titleObservation() throws Exception {
        start(config);

        // start a panel
        IClusterControl control = context.newCluster(new JcrClusterConfig(new JcrNodeModel("/config/panel")), null);
        control.start();

        ContentPanel panel = context.getService(PANELS, ContentPanel.class);
        ObservableModel model = new ObservableModel();
        model.setObject("first");
        panel.setDefaultModel(model);

        // re-render with panel
        refreshPage();
        tester.assertContains("tabs-container");
        tester.assertLabel("root:tabs:tabs-container:tabs:0:container:link:title", "first");

        // change title
        model.setObject("second");
        refreshPage();
        tester.assertContains("tabs-container");

        // re-register panel
        panel.reregister();
        refreshPage();
        tester.assertContains("tabs-container");
    }

    @Test
    public void focusListenerInvokedOnTabSwitch() throws Exception {
        start(config);

        IClusterControl[] controls = new IClusterControl[2];
        for(int i = 0; i < 2; i++) {
            controls[i] = context.newCluster(new JcrClusterConfig(new JcrNodeModel("/config/panel")), null);
            controls[i].start();
        }
        ObservableModel model = new ObservableModel();
        model.setObject("titel");
        List<ContentPanel> panels = context.getServices(PANELS, ContentPanel.class);
        assertEquals(2, panels.size());

        class Selected {
            int tabbie;
        };
        final Selected selected = new Selected();

        IFocusListener[] listeners = new IFocusListener[2];
        int i = 0;
        for (ContentPanel panel : panels) {
            panel.setDefaultModel(model);
            final int j = i;
            listeners[i++] = new IFocusListener() {
                private static final long serialVersionUID = 1L;

                public void onFocus(IRenderService renderService) {
                    selected.tabbie = j;
                }

            };
            context.registerService(listeners[j], context.getReference(panel).getServiceId());
        }

        // re-render
        refreshPage();

        tester.clickLink("root:tabs:tabs-container:tabs:0:container:link");
        assertEquals(0, selected.tabbie);

        tester.clickLink("root:tabs:tabs-container:tabs:1:container:link");
        assertEquals(1, selected.tabbie);
    }

}
