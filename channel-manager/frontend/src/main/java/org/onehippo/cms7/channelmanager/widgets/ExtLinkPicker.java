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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private static final String[] DEFAULT_PICKER_SELECTABLE_NODE_TYPES = new String[]{"hippo:document"};
    private static final String DEFAULT_PICKER_INITIAL_PATH = "";
    private static final String EVENT_PICK = "pick";
    private static final String EVENT_PICK_PARAM_CURRENT = "current";
    private static final String EVENT_PICK_PARAM_PICKER_CONFIG = "pickerConfig";

    private final Logger log = LoggerFactory.getLogger(ExtLinkPicker.class);

    public ExtLinkPicker(final IPluginContext context) {
        addEventListener(EVENT_PICK, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                String current = JsonUtil.getStringParameter(parameters, EVENT_PICK_PARAM_CURRENT);

                IPluginConfig pickerConfig = parsePickerConfig(parameters, EVENT_PICK_PARAM_PICKER_CONFIG, current);
                if (pickerConfig == null) {
                    log.error("Cannot open link picker: no picker configuration specified");
                }

                Model<String> pathModel = new Model<String>(current) {
                    @Override
                    public void setObject(final String pickedPath) {
                        super.setObject(pickedPath);
                        AjaxRequestTarget target = AjaxRequestTarget.get();
                        if (target == null) {
                            log.warn("Cannot invoke callback for picked path '{}': no ajax request target available", pickedPath);
                            return;
                        }
                        target.prependJavascript("Hippo.ChannelManager.ExtLinkPickerFactory.Instance.fireEvent('picked', '" + pickedPath + "')");
                    }
                };

                final IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, new UuidFromPathModel(pathModel));
                final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);

                final DialogAction action = new DialogAction(dialogFactory, dialogService);
                action.execute();
            }
        });
    }

    @Override
    public void bind(final Component component) {
        super.bind(component);
        component.add(JavascriptPackageResource.getHeaderContribution(ExtLinkPicker.class, "ExtLinkPicker.js"));
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

}
