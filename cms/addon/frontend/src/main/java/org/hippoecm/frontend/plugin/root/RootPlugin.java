/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin.root;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.util.ServiceTracker;

public class RootPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private Map<String, ServiceTracker> trackers;

    public RootPlugin() {
        trackers = new HashMap<String, ServiceTracker>();
        for (String extension : new String[] { "browser", "content" }) {
            ServiceTracker tracker = new ServiceTracker(IRenderService.class);
            tracker.addListener(new ServiceTracker.IListener() {
                private static final long serialVersionUID = 1L;

                public void onServiceAdded(String name, Serializable service) {
                    replace((Component) service);
                }

                public void onServiceChanged(String name, Serializable service) {
                }

                public void onServiceRemoved(String name, Serializable service) {
                    replace(new EmptyPanel(((Component) service).getId()));
                }
            });
            trackers.put(extension, tracker);
            add(new EmptyPanel(extension));
        }
    }

    @Override
    public void start(PluginContext context) {
        super.start(context);

        for (Map.Entry<String, ServiceTracker> entry : trackers.entrySet()) {
            entry.getValue().open(context, context.getProperty(entry.getKey()));
        }
    }

    @Override
    public void stop() {
        for (Map.Entry<String, ServiceTracker> entry : trackers.entrySet()) {
            entry.getValue().close();
            replace(new EmptyPanel(entry.getKey()));
        }

        super.stop();
    }
}
