/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.model.AbsoluteRelativePathModel;
import org.onehippo.cms7.channelmanager.model.UuidFromPathModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.ExtObservable;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;

/**
 * Factory and event hub for link picker dialogs opened from ExtJS. A link picker widget is available via the ExtJS
 * xtype 'linkpicker'. The widget will generate a 'pick' event when clicked, which opens a link picker with a certain
 * configuration. Available javascript picker configuration options are:
 * <ul>
 * <li>'configuration': The root path of the CMS configuration to use for the picker, relative to
 * '/hippo:configuration/hippo:frontend/cms'. The default configuration is "cms-pickers/documents"</li>
 * <li>'remembersLastVisited': Whether the picker remembers the last visited path. The default is 'true'</li>
 * <li>'initialPath': The initial path to use in the picker if nothing has been selected yet. The default is ""
 * (no initial path)</li>
 * <li>'selectableNodeTypes': string array of node type names to be able to select in the picker. The default is
 * ['hippo:document']</li>
 * </ul>
 * Once a path has been picked a 'picked' event will be generated that updates the ExtJS widget with the selected path.
 */
@ExtClass("Hippo.ChannelManager.ExtLinkPickerFactory")
public class ExtLinkPicker extends ExtObservable {

    private static final String DEFAULT_PICKER_CONFIGURATION = "cms-pickers/documents";
    private static final boolean DEFAULT_PICKER_REMEMBERS_LAST_VISITED = true;
    private static final String[] DEFAULT_PICKER_SELECTABLE_NODE_TYPES = {};
    private static final String DEFAULT_PICKER_INITIAL_PATH = "";
    private static final String DEFAULT_PICKER_ROOT_PATH = "";
    private static final boolean DEFAULT_PICKER_PATH_IS_RELATIVE = false;
    private static final String EVENT_PICK = "pick";
    private static final String EVENT_PICK_PARAM_CURRENT = "current";
    private static final String EVENT_PICK_PARAM_PICKER_CONFIG = "pickerConfig";
    private static final long serialVersionUID = 1L;
    private static final JavaScriptResourceReference LINKPICKER_JS = new JavaScriptResourceReference(ExtLinkPicker.class, "ExtLinkPicker.js");

    private final Logger log = LoggerFactory.getLogger(ExtLinkPicker.class);

    public ExtLinkPicker(final IPluginContext context) {
        addEventListener(EVENT_PICK, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                String current = JsonUtil.getStringParameter(parameters, EVENT_PICK_PARAM_CURRENT);

                JSONObject pickerConfigObject = JsonUtil.getJsonObject(parameters, EVENT_PICK_PARAM_PICKER_CONFIG);

                boolean isRelativePath = DEFAULT_PICKER_PATH_IS_RELATIVE;
                if (pickerConfigObject != null) {
                    isRelativePath = pickerConfigObject.optBoolean("isRelativePath", DEFAULT_PICKER_PATH_IS_RELATIVE);
                }

                String rootPath = DEFAULT_PICKER_ROOT_PATH;
                if (pickerConfigObject != null) {
                    try {
                        rootPath = URLDecoder.decode(pickerConfigObject.optString("rootPath", DEFAULT_PICKER_ROOT_PATH), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        log.warn("Error decoding the root path for the dialog picker.", e);
                    }
                }

                IPluginConfig pickerConfig = parsePickerConfig(pickerConfigObject, current, isRelativePath, rootPath);

                // decorator pattern for path models: UUID-to-path model decorates the absolute-to-relative model,
                // which decorates the picked model.
                PickedPathModel pickedPathModel = new PickedPathModel();
                IModel<String> absRelPathModel = new AbsoluteRelativePathModel(pickedPathModel, current, isRelativePath, rootPath);
                IModel<String> uuidFromPathModel = new UuidFromPathModel(absRelPathModel);

                final IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, uuidFromPathModel);
                final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);

                pickedPathModel.enableEvents();

                final DialogAction action = new DialogAction(dialogFactory, dialogService);
                action.execute();
            }
        });
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        response.render(JavaScriptHeaderItem.forReference(LINKPICKER_JS));
        super.renderHead(component, response);
    }

    IPluginConfig parsePickerConfig(final JSONObject json, String current, boolean isRelativePath, String rootPath) {
        if (json != null) {
            String configuration = json.optString("configuration", DEFAULT_PICKER_CONFIGURATION);
            boolean remembersLastVisited = json.optBoolean("remembersLastVisited", DEFAULT_PICKER_REMEMBERS_LAST_VISITED);

            String initialPath = DEFAULT_PICKER_INITIAL_PATH;
            try {
                initialPath = URLDecoder.decode(json.optString("initialPath", DEFAULT_PICKER_INITIAL_PATH), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.warn("Error decoding the initialPath property of the link picker config.", e);
            }
            if (isRelativePath) {
                initialPath = rootPath + (initialPath.startsWith("/") ? "" : "/") + initialPath;
            }
            if (remembersLastVisited && StringUtils.isEmpty(initialPath) && StringUtils.isNotEmpty(current)) {
                initialPath = current;
            }

            String[] selectableNodeTypes = DEFAULT_PICKER_SELECTABLE_NODE_TYPES;
            JSONArray selectableNodeTypesArray = json.optJSONArray("selectableNodeTypes");
            if (selectableNodeTypesArray != null && selectableNodeTypesArray.length() > 0) {
                List<String> selectableNodeTypesList = new ArrayList<String>();
                for (int i = 0; i < selectableNodeTypesArray.length(); i++) {
                    try {
                        selectableNodeTypesList.add(selectableNodeTypesArray.getString(i));
                    } catch (JSONException e) {
                        log.warn("Cannot parse node type #{} in {}; ignoring it as a selectable node type in the link picker",
                                i, selectableNodeTypesArray);
                    }
                }
                selectableNodeTypes = new String[selectableNodeTypesList.size()];
                selectableNodeTypes = selectableNodeTypesList.toArray(selectableNodeTypes);
            }

            return JcrPathWidget.createPickerConfig(configuration, remembersLastVisited, selectableNodeTypes, initialPath, rootPath);
        }

        return JcrPathWidget.createPickerConfig(
                DEFAULT_PICKER_CONFIGURATION,
                DEFAULT_PICKER_REMEMBERS_LAST_VISITED,
                DEFAULT_PICKER_SELECTABLE_NODE_TYPES,
                DEFAULT_PICKER_INITIAL_PATH,
                DEFAULT_PICKER_ROOT_PATH);
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if (EVENT_PICK.equals(event)) {
            return new ExtEventAjaxBehavior(EVENT_PICK_PARAM_CURRENT, EVENT_PICK_PARAM_PICKER_CONFIG);
        }
        return super.newExtEventBehavior(event);
    }

    private static IDialogFactory createDialogFactory(final IPluginContext context, final IPluginConfig config, final IModel<String> model) {
        return new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public AbstractDialog<String> createDialog() {
                return new LinkPickerDialog(context, config, model);
            }
        };
    }

    /**
     * Model that fires a 'picked' event when events are enabled and a new path is set. By default, events are disabled.
     */
    public class PickedPathModel extends Model<String> {

        private boolean enabledEvents;

        public PickedPathModel() {
            enabledEvents = false;
        }

        public void enableEvents() {
            enabledEvents = true;
        }

        @Override
        public void setObject(String path) {
            super.setObject(path);

            if (enabledEvents) {
                // notify the client that a new path has been picked
                AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target == null) {
                    log.warn("Cannot invoke callback for picked path '{}': no ajax request target available", path);
                    return;
                }
                target.prependJavaScript("Hippo.ChannelManager.ExtLinkPickerFactory.Instance.fireEvent('picked', '" + path + "')");
            }
        }

    }
}
