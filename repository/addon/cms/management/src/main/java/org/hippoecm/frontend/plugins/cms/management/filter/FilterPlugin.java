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
package org.hippoecm.frontend.plugins.cms.management.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.legacy.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.model.JcrNodeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FilterPlugin.class);

    private Set<String> handle;

    public FilterPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        super(descriptor, new JcrNodeModel(model), parentPlugin);

        handle = null;
        ParameterValue param = descriptor.getParameter("handle");
        if (param != null && param.getStrings().size() > 0) {
            handle = new HashSet<String>(param.getStrings());
        } else {
            log.warn("No configuration specified for FilterPlugin.  No filtering will take place.");
        }
    }

    @Override
    public void handle(Request request) {
        if (handle != null && handle.contains(request.getOperation())) {
            Channel channel = getBottomChannel();
            if (channel != null) {
                Notification notification = channel.createNotification(request);
                channel.publish(notification);
            }
            return;
        }
        super.handle(request);
    }
}
