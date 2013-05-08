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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.resource.TextTemplateResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.configuration.channel.Channel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.channels.ChannelStoreFactory;
import org.onehippo.cms7.channelmanager.templatecomposer.ToolbarPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtArrayStore;
import org.wicketstuff.js.ext.data.ExtDataField;
import org.wicketstuff.js.ext.data.ExtStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import static org.onehippo.cms7.channelmanager.ChannelManagerConsts.CONFIG_REST_PROXY_SERVICE_ID;

/**
 * @version "$Id$"
 */
@ExtClass("Hippo.ChannelManager.DeviceManager")
public class DeviceManager extends ToolbarPlugin implements IHeaderContributor {

    private static Logger log = LoggerFactory.getLogger(DeviceManager.class);

    protected static final String DEVICE_MANAGER_JS = "DeviceManager.js";
    protected static final String SERVICE_ID = "deviceskins.service.id";

    private final ExtStore store;
    private final ChannelStore channelStore;
    private final DeviceService service;

    public DeviceManager(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.containsKey(SERVICE_ID)) {
            service = context.getService(config.getString(SERVICE_ID), DeviceService.class);
        } else {
            //loading default service
            final DefaultDeviceService defaultDeviceService = new DefaultDeviceService(context, config);
            service = defaultDeviceService;
        }

        IRestProxyService restProxyService = context.getService(config.getString(CONFIG_REST_PROXY_SERVICE_ID, IRestProxyService.class.getName()), IRestProxyService.class);

        this.channelStore = ChannelStoreFactory.createStore(context, config, restProxyService);

        addHeadContribution();
        this.store = new ExtArrayStore<StyleableDevice>(Arrays.asList(new ExtDataField("name"), new ExtDataField("id"),
                new ExtDataField("relativeImageUrl")),service.getStylables());
    }

    /**
     * Adding js file and generating dynamic css.
     */
    public void addHeadContribution() {
        add(JavascriptPackageResource.getHeaderContribution(DeviceManager.class, DEVICE_MANAGER_JS));

        StringBuilder buf = new StringBuilder();
        for (StyleableDevice styleable : service.getStylables()) {
            styleable.appendCss(buf);
        }
        final Map<String,Object> cssMap = new HashMap<String,Object>();
        cssMap.put("css", buf.toString());
        ResourceReference resourceReference = new TextTemplateResourceReference(DeviceManager.class, "dynamic.css", "text/css", new LoadableDetachableModel<Map<String, Object>>() {
            @Override
            protected Map<String, Object> load() {
                return Collections.unmodifiableMap(cssMap);
            }
        });
        add(CSSPackageResource.getHeaderContribution(resourceReference));
    }


    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        RequestCycle rc = RequestCycle.get();
        properties.put("baseImageUrl", rc.urlFor(new ResourceReference(this.getClass(), "")));
        properties.put("deviceStore", new JSONIdentifier(this.store.getJsObjectId()));

        JSONObject defaultDeviceIds = new JSONObject();
        JSONObject devices = new JSONObject();
        for (Channel channel: this.channelStore.getChannels()) {
            defaultDeviceIds.put(channel.getId(), channel.getDefaultDevice());
            JSONArray channelDevices = new JSONArray();
            for (String device : channel.getDevices()) {
                channelDevices.put(device);
            }
            devices.put(channel.getId(), channelDevices);
        }
        properties.put("defaultDeviceIds", defaultDeviceIds);
        properties.put("devices", devices);
    }

}
