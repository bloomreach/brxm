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
package org.hippoecm.frontend.console;

import java.io.Serializable;

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.service.RenderService;

public class RootPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private String[] extensions;

    public RootPlugin() {
        extensions = new String[] { "browser", "content" };
    }

    @Override
    public void start(PluginContext context) {
        super.start(context);

        for (String extension : extensions) {
            registerListener(extension);
        }
    }

    @Override
    public void stop() {
        for (String extension : extensions) {
            unregisterListener(extension);
        }

        super.stop();
    }

    @Override
    protected void onServiceAdded(String name, Serializable service) {
        super.onServiceAdded(name, service);
        if (service instanceof RenderService) {
            add((RenderService) service);
        }
    }

    @Override
    protected void onServiceRemoved(String name, Serializable service) {
        if (service instanceof RenderService) {
            remove((RenderService) service);
        }
        super.onServiceRemoved(name, service);
    }
}
