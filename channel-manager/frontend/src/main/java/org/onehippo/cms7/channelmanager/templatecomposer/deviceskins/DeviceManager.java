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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.resource.TextTemplateResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelNotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.channels.ChannelStoreFactory;
import org.onehippo.cms7.channelmanager.templatecomposer.ToolbarPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.data.ExtStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import static org.onehippo.cms7.channelmanager.ChannelManagerConsts.CONFIG_REST_PROXY_SERVICE_ID;

/**
 * @version "$Id$"
 */
@ExtClass("Hippo.ChannelManager.DeviceManager")
public class DeviceManager extends ToolbarPlugin implements IHeaderContributor {

    private static Logger log = LoggerFactory.getLogger(DeviceManager.class);

    protected static final String DEVICE_MANAGER_JS = "DeviceManager.js";
    protected static final String SERVICE_ID = "device.service.id";

    private final ExtStore store;
    private final ChannelStore channelStore;
    final DeviceService service;

    private static final Map<String, Object> CSS = new ConcurrentHashMap<String, Object>();

    @ExtProperty
    protected String emptyText;


    public DeviceManager(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.containsKey(SERVICE_ID)) {
            service = context.getService(config.getString(SERVICE_ID), DeviceService.class);
        } else {
            //loading default service
            final DefaultDeviceService defaultDeviceService = new DefaultDeviceService(context, config);
            service = defaultDeviceService;
            this.emptyText = defaultDeviceService.translateKey("emptyText");
        }

        IRestProxyService restProxyService = context.getService(config.getString(CONFIG_REST_PROXY_SERVICE_ID, IRestProxyService.class.getName()), IRestProxyService.class);

        this.channelStore = ChannelStoreFactory.createStore(context, config, restProxyService);

        addEventListener("setchanneldefaults", new ExtEventListener() {
            private Object getValue(final Map<String, JSONArray> parameters, final String key) throws JSONException {
                JSONArray values = parameters.get(key);
                if (values == null || values.length() == 0) {
                    return null;
                }
                return values.get(0);
            }

            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                try {
                    String channelId = (String) getValue(parameters, "channelId");
                    Channel channel = channelStore.getChannel(channelId);
                    List<String> devices = channel.getDevices();
                    String defaultDevice = channel.getDefaultDevice();
                    StringBuilder buf = new StringBuilder();
                    buf.append("Ext.getCmp('deviceManager').setChannelDefaults('");
                    buf.append(channelId);
                    buf.append("',[");
                    if (devices != null) {
                        for (String device : devices) {
                            buf.append("'");
                            buf.append(device);
                            buf.append("',");
                        }
                        buf.setLength(buf.length()-1);
                    }
                    buf.append("],'");
                    buf.append(defaultDevice);
                    buf.append("')");
                    target.prependJavascript(buf.toString());
                } catch (JSONException e) {
                    throw new WicketRuntimeException("Invalid JSON parameters", e);
                } catch (ChannelNotFoundException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

        });
        addHeadContribution();
        this.store = service.getStore();
    }

    /**
     * Adding js file and generating dynamic css.
     */
    public void addHeadContribution() {
        add(JavascriptPackageResource.getHeaderContribution(DeviceManager.class, DEVICE_MANAGER_JS));

        String style = getCssStyle();
        CSS.put("css", style);
        ResourceReference resourceReference = new TextTemplateResourceReference(DeviceManager.class, "dynamic.css", "text/css", new LoadableDetachableModel<Map<String, Object>>() {
            @Override
            protected Map<String, Object> load() {
                Map<String, Object> cssMap = new HashMap<String, Object>();
                cssMap.putAll(CSS);
                return cssMap;
            }
        });
        add(CSSPackageResource.getHeaderContribution(resourceReference));
    }

    /**
     * @return Retrieving the appropriate styles for each device.
     */
    public String getCssStyle() {
        final CSSUtil util = new CSSUtil();
        for (StyleableDevice styleable : service.getStylables()) {
            CSSRule wraprule = new CSSRule(String.format(".%s > .x-panel-bwrap > .x-panel-body", styleable.getId()));
            wraprule.setDeclarationsString(styleable.getWrapStyle());
            CSSRule rule = new CSSRule(String.format(".%s > .x-panel-bwrap > .x-panel-body iframe", styleable.getId()));
            rule.setDeclarationsString(styleable.getStyle());
            util.add(wraprule);
            util.add(rule);
        }
        return util.toString();
    }



    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if ("setchanneldefaults".equals(event)) {
            return new ExtEventAjaxBehavior("channelId");
        }
        return super.newExtEventBehavior(event);
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

    public String getEmptyText() {
        return emptyText;
    }

    public void setEmptyText(final String emptyText) {
        this.emptyText = emptyText;
    }

    private class CSSUtil {
        List<CSSRule> cssRules = new ArrayList<CSSRule>();
        private static final String loopTemplate = "%s\n";

        public boolean add(final CSSRule cssRule) {
            return cssRules.add(cssRule);
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            for (CSSRule rule : cssRules) {
                buffer.append(String.format(loopTemplate, rule.toString()));
            }
            return buffer.toString();
        }
    }

    private class CSSRule {

        private String selector;
        private String declarationsString;
        private static final String template = "" +
                "%s{\n" +
                "%s" +
                "\n}";

        public CSSRule(final String selector) {
            this.selector = selector;
        }

        public void setDeclarationsString(final String declarationString) {
            this.declarationsString = declarationString;
        }

        public String toString() {
            return String.format(template, selector, declarationsString);
        }
    }
}
