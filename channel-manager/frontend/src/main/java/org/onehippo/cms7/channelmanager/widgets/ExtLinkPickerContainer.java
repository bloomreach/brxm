/**
 * Copyright 2011 Hippo
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
package org.onehippo.cms7.channelmanager.widgets;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;

/**
 * Generic placeholder for link picker dialogs opened from ExtJS. Dialogs are opened via the 'pick' event.
 */
@ExtClass("Hippo.ChannelManager.ExtLinkPickerContainer")
public class ExtLinkPickerContainer extends ExtPanel {

    private static final String DEFAULT_PICKER_CONFIGURATION = "cms-pickers/documents";
    private static final boolean DEFAULT_PICKER_REMEMBERS_LAST_VISITED = true;
    private static final String[] DEFAULT_PICKER_SELECTABLE_NODE_TYPES = new String[]{"hippo:document"};
    private static final String DEFAULT_PICKER_INITIAL_PATH = "";
    private static final String EVENT_PICK = "pick";
    private static final String EVENT_PICK_PARAM_CALLBACK_ID = "callbackId";
    private static final String EVENT_PICK_PARAM_CURRENT = "current";
    private static final String EVENT_PICK_PARAM_PICKER_CONFIG = "pickerConfig";

    private final Logger log = LoggerFactory.getLogger(ExtLinkPickerContainer.class);
    private final ExtLinkPicker picker;

    public ExtLinkPickerContainer(IPluginContext context, String pickerId) {
        add(JavascriptPackageResource.getHeaderContribution(ExtLinkPickerContainer.class, "ExtLinkPickerContainer.js"));

        picker = new ExtLinkPicker(context, pickerId);
        add(picker);

        addEventListener(EVENT_PICK, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                String componentId = JsonUtil.getStringParameter(parameters, EVENT_PICK_PARAM_CALLBACK_ID);
                if (componentId == null) {
                    log.error("Cannot open link picker: no component ID specified for callback");
                    return;
                }

                String current = JsonUtil.getStringParameter(parameters, EVENT_PICK_PARAM_CURRENT);

                IPluginConfig pickerConfig = parsePickerConfig(parameters, EVENT_PICK_PARAM_PICKER_CONFIG, current);
                if (pickerConfig == null) {
                    log.error("Cannot open link picker: no picker configuration specified");
                }

                picker.pickFolder(target, componentId, pickerConfig);
            }
        });
    }

    IPluginConfig parsePickerConfig(final Map<String, JSONArray> parameters, String parameterName, String current) {
        JSONObject json = JsonUtil.getJsonObject(parameters, parameterName);
        if (json != null) {
            String configuration = json.optString("configuration", DEFAULT_PICKER_CONFIGURATION);
            boolean remembersLastVisited = json.optBoolean("remembersLastVisited", DEFAULT_PICKER_REMEMBERS_LAST_VISITED);
            String initialPath = json.optString("initialPath", DEFAULT_PICKER_INITIAL_PATH);
            if (remembersLastVisited && StringUtils.isEmpty(initialPath) && StringUtils.isNotEmpty(current)) {
                initialPath = current;
            }

            String[] selectableNodeTypes = StringUtils.split(json.optString("selectableNodeTypes"), ", ");
            if (selectableNodeTypes == null || selectableNodeTypes.length == 0) {
                selectableNodeTypes = DEFAULT_PICKER_SELECTABLE_NODE_TYPES;
            }

            return JcrPathWidget.createPickerConfig(configuration, remembersLastVisited, selectableNodeTypes, initialPath);
        }

        return JcrPathWidget.createPickerConfig(
                DEFAULT_PICKER_CONFIGURATION,
                DEFAULT_PICKER_REMEMBERS_LAST_VISITED,
                DEFAULT_PICKER_SELECTABLE_NODE_TYPES,
                DEFAULT_PICKER_INITIAL_PATH);
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if (EVENT_PICK.equals(event)) {
            return new ExtEventAjaxBehavior(EVENT_PICK_PARAM_CALLBACK_ID, EVENT_PICK_PARAM_CURRENT, EVENT_PICK_PARAM_PICKER_CONFIG);
        }
        return super.newExtEventBehavior(event);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("id", "channelmanager-picker-container");
        properties.put("eventHandlerId", "channelmanager-picker-container");
    }

}
