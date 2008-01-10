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
package org.hippoecm.frontend.plugins.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FilterPlugin.class);

    private Set<String> handle;

    public FilterPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(descriptor, model, parentPlugin);

        handle = null;
        List<String> ops = descriptor.getParameter("handle");
        if (ops != null) {
            handle = new HashSet<String>(ops.size());
            handle.addAll(ops);
        } else {
            log.warn("No configuration specified for FilterPlugin.  No filtering will take place.");
        }
    }

    @Override
    public void handle(Request request) {
        if (handle != null && handle.contains(request.getOperation())) {
            Channel channel = getDescriptor().getOutgoing();
            if (channel != null) {
                Notification notification = channel.createNotification(request);
                channel.publish(notification);
            }
            return;
        }
        super.handle(request);
    }
}
