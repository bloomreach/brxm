/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugin;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.application.Home;
import org.hippoecm.frontend.service.IRenderReference;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderReference extends LoadableDetachableModel implements IRenderReference, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderReference.class);

    private IRenderService root;
    private String pluginPath;

    public RenderReference(IRenderService renderer) {
        super(renderer);

        StringBuilder sb = new StringBuilder();
        IRenderService parent = renderer;
        IRenderService ancestor = parent.getParentService();
        while (ancestor != null) {
            List<IRenderService> siblings = ancestor.getChildServices(parent.getId());
            int index = siblings.indexOf(parent);

            sb.insert(0, ':');
            sb.insert(0, Integer.toString(index));
            sb.insert(0, ':');
            sb.insert(0, parent.getId());

            parent = ancestor;
            ancestor = ancestor.getParentService();
        }
        root = parent;
        if (!(root instanceof Home)) {
            log.warn("root render service is not a web page; deserialization may fail.");
        }
        pluginPath = sb.toString();
    }

    public IRenderService resolve() {
        return (IRenderService) getObject();
    }

    @Override
    public Object load() {
        String path = pluginPath;
        IRenderService renderer = root;
        while (path.length() > 0) {
            int sep = path.indexOf(':');
            String name = path.substring(0, sep);
            path = path.substring(sep + 1);

            List<IRenderService> list = renderer.getChildServices(name);
            if (list == null) {
                return null;
            }

            sep = path.indexOf(':');
            int idx;
            if (sep < 0) {
                idx = Integer.valueOf(path);
            } else {
                idx = Integer.valueOf(path.substring(0, sep));
                path = path.substring(sep + 1);
            }

            IRenderService service = (IRenderService) list.get(idx);
            if (sep < 0) {
                return service;
            } else {
                if (service == null) {
                    return null;
                }
            }
        }
        return null;
    }
}
