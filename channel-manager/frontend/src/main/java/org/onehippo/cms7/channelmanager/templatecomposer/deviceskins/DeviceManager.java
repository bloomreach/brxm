/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.templatecomposer.ToolbarPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtArrayStore;
import org.wicketstuff.js.ext.data.ExtDataField;
import org.wicketstuff.js.ext.data.ExtStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

/**
 * @version "$Id$"
 */
@ExtClass("Hippo.ChannelManager.DeviceManager")
public class DeviceManager extends ToolbarPlugin {

    private static Logger log = LoggerFactory.getLogger(DeviceManager.class);

    protected static final String DEVICE_MANAGER_JS = "DeviceManager.js";
    protected static final String SERVICE_ID = "deviceskins.service.id";

    private final ExtStore store;
    private final DeviceService service;

    public DeviceManager(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.containsKey(SERVICE_ID)) {
            service = context.getService(config.getString(SERVICE_ID), DeviceService.class);
        } else {
            // loading default service
            final ClassPathDeviceService defaultDeviceService = new ClassPathDeviceService(context, config);
            service = defaultDeviceService;
        }

        final List<DeviceSkin> deviceSkins = service.getDeviceSkins();
        List<DeviceSkinDetails> detailsList = new ArrayList<DeviceSkinDetails>();
        for (DeviceSkin skin : deviceSkins) {
            detailsList.add(new DeviceSkinDetails(skin));
        }
        this.store = new ExtArrayStore<DeviceSkinDetails>(Arrays.asList(new ExtDataField("name"), new ExtDataField("id"),
                new ExtDataField("imageUrl")), detailsList);
    }

    /**
     * Adding js file and generating dynamic css.
     */
    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(DeviceManager.class, DEVICE_MANAGER_JS)));

        ResourceReference resourceReference = new ResourceReference(DeviceManager.class, "dynamic.css") {

            @Override
            public IResource getResource() {
                return new ResourceStreamResource() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IResourceStream getResourceStream() {
                        StringBuilder buf = new StringBuilder();
                        for (DeviceSkin skin : service.getDeviceSkins()) {
                            buf.append(skin.getCss());
                        }
                        return new StringResourceStream(buf.toString(), "text/css");
                    }
                };
            }
        };
        response.render(CssHeaderItem.forReference(resourceReference));
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("deviceStore", new JSONIdentifier(this.store.getJsObjectId()));

    }

    static class DeviceSkinDetails implements Serializable {
        private final String id;
        private final String name;
        private final String imageUrl;

        public DeviceSkinDetails(final DeviceSkin skin) {
            this.id = skin.getId();
            this.name = skin.getName();

            RequestCycle rc = RequestCycle.get();
            this.imageUrl = rc.urlFor(new ResourceReferenceRequestHandler(
                    skin.getImage())).toString();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }

}
