/*
 * Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Objects;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.model.DocumentLinkInfo;
import org.onehippo.cms7.channelmanager.model.DocumentLinkModel;
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
    private static final Logger log = LoggerFactory.getLogger(ExtLinkPicker.class);

    private static final ResourceReference LINK_PICKER_JS = new JavaScriptResourceReference(ExtLinkPicker.class,
            "ExtLinkPicker.js");

    private static final String DEFAULT_PICKER_CONFIGURATION = "cms-pickers/documents";
    private static final boolean DEFAULT_PICKER_REMEMBERS_LAST_VISITED = true;
    private static final String[] DEFAULT_PICKER_SELECTABLE_NODE_TYPES = {};
    private static final String DEFAULT_PICKER_INITIAL_PATH = "";
    private static final String DEFAULT_PICKER_ROOT_PATH = "";
    private static final boolean DEFAULT_PICKER_PATH_IS_RELATIVE = false;

    private static final String EVENT_PICK = "pick";
    private static final String EVENT_PICK_PARAM_CURRENT = "current";
    private static final String EVENT_PICK_PARAM_PICKER_CONFIG = "pickerConfig";

    public ExtLinkPicker(final IPluginContext context) {
        addEventListener(EVENT_PICK, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {

                final JSONObject pickerConfigObject = JsonUtil.getJsonObject(parameters,
                        EVENT_PICK_PARAM_PICKER_CONFIG);
                final boolean isRelativePath = pickerConfigObject != null
                        ? pickerConfigObject.optBoolean("isRelativePath", DEFAULT_PICKER_PATH_IS_RELATIVE)
                        : DEFAULT_PICKER_PATH_IS_RELATIVE;

                String rootPath = DEFAULT_PICKER_ROOT_PATH;
                if (pickerConfigObject != null) {
                    try {
                        rootPath = URLDecoder.decode(pickerConfigObject.optString("rootPath", DEFAULT_PICKER_ROOT_PATH),
                                "UTF-8");
                    } catch (final UnsupportedEncodingException e) {
                        log.warn("Error decoding the root path for the dialog picker.", e);
                    }
                }

                final String current = JsonUtil.getStringParameter(parameters, EVENT_PICK_PARAM_CURRENT);
                final IPluginConfig pickerConfig = parsePickerConfig(pickerConfigObject, current, isRelativePath,
                        rootPath);

                // decorator pattern for path models: UUID-to-path model decorates the absolute-to-relative model,
                // which decorates the picked model.
                final PickedNodeModel pickedNodeModel = new PickedNodeModel();
                final IModel<String> absRelPathModel = new DocumentLinkModel(pickedNodeModel, current, isRelativePath,
                        rootPath);
                final IModel<String> uuidFromPathModel = new UuidFromPathModel(absRelPathModel);

                final IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, uuidFromPathModel);
                final IDialogService dialogService = context.getService(IDialogService.class.getName(),
                        IDialogService.class);

                pickedNodeModel.enableEvents();

                final DialogAction action = new DialogAction(dialogFactory, dialogService);
                action.execute();
            }
        });
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        response.render(JavaScriptHeaderItem.forReference(LINK_PICKER_JS));
        super.renderHead(component, response);
    }

    IPluginConfig parsePickerConfig(final JSONObject json, final String current, final boolean isRelativePath, final String rootPath) {
        if (json != null) {
            final String configuration = json.optString("configuration", DEFAULT_PICKER_CONFIGURATION);
            final boolean remembersLastVisited = json.optBoolean("remembersLastVisited",
                    DEFAULT_PICKER_REMEMBERS_LAST_VISITED);

            String initialPath = DEFAULT_PICKER_INITIAL_PATH;
            try {
                initialPath = URLDecoder.decode(json.optString("initialPath", DEFAULT_PICKER_INITIAL_PATH), "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                log.warn("Error decoding the initialPath property of the link picker config.", e);
            }
            if (isRelativePath) {
                initialPath = rootPath + (initialPath.startsWith("/") ? "" : "/") + initialPath;
            }
            if (remembersLastVisited && StringUtils.isEmpty(initialPath) && StringUtils.isNotEmpty(current)) {
                initialPath = current;
            }

            final String[] selectableNodeTypes = parseSelectableNodeTypes(json.optJSONArray("selectableNodeTypes"));
            return createPickerConfig(configuration, remembersLastVisited, selectableNodeTypes, initialPath, rootPath);
        }

        return createPickerConfig(
                DEFAULT_PICKER_CONFIGURATION,
                DEFAULT_PICKER_REMEMBERS_LAST_VISITED,
                DEFAULT_PICKER_SELECTABLE_NODE_TYPES,
                DEFAULT_PICKER_INITIAL_PATH,
                DEFAULT_PICKER_ROOT_PATH);
    }

    private String[] parseSelectableNodeTypes(final JSONArray selectableNodeTypesArray) {
        if (selectableNodeTypesArray == null || selectableNodeTypesArray.length() == 0) {
            return DEFAULT_PICKER_SELECTABLE_NODE_TYPES;
        }

        final List<String> selectableNodeTypesList = new ArrayList<>();
        for (int i = 0; i < selectableNodeTypesArray.length(); i++) {
            try {
                selectableNodeTypesList.add(selectableNodeTypesArray.getString(i));
            } catch (final JSONException e) {
                log.warn("Cannot parse node type #{} in {}; ignoring it as a selectable node type in the link picker",
                        i, selectableNodeTypesArray);
            }
        }
        return selectableNodeTypesList.toArray(new String[0]);
    }

    private static JavaPluginConfig createPickerConfig(final String pickerConfigPath,
                                                               final boolean remembersLastVisited,
                                                               final String[] selectableNodeTypes,
                                                               final String initialPath,
                                                               final String rootPath) {
        final JavaPluginConfig pickerConfig = new JavaPluginConfig();
        pickerConfig.put("cluster.name", pickerConfigPath);
        pickerConfig.put(NodePickerControllerSettings.LAST_VISITED_ENABLED, Boolean.toString(remembersLastVisited));
        pickerConfig.put(NodePickerControllerSettings.SELECTABLE_NODETYPES, selectableNodeTypes);

        if (StringUtils.isNotEmpty(initialPath)) {
            final javax.jcr.Session session = UserSession.get().getJcrSession();
            try {
                final Node node = session.getNode(initialPath);
                pickerConfig.put(NodePickerControllerSettings.BASE_UUID, node.getIdentifier());
            } catch (final PathNotFoundException e) {
                log.warn("Initial picker path not found: '{}'. Using the default initial path of '{}' instead.",
                        initialPath, pickerConfigPath);
            } catch (final RepositoryException e) {
                log.error("Could not retrieve the UUID of initial picker path node '" + initialPath
                        + "'. Using the default initial path of '" + pickerConfigPath + "' instead.", e);
            }
        }
        if (StringUtils.isNotEmpty(rootPath)) {
            // set cluster option 'root.path', which will be used as the root of the navigator in the document picker
            final JavaPluginConfig clusterOptions = new JavaPluginConfig();
            clusterOptions.put("root.path", rootPath);
            pickerConfig.put("cluster.options", clusterOptions);
        }
        return pickerConfig;
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
            @Override
            public AbstractDialog<String> createDialog() {
                return new LinkPickerDialog(context, config, model) {

                    @Override
                    public void onCancelFromCloseButton() {
                        super.onCancelFromCloseButton();
                        fireLinkPickerFactoryEvent("cancel");
                    }

                    @Override
                    protected void onCancel() {
                        super.onCancel();
                        fireLinkPickerFactoryEvent("cancel");
                    }
                };
            }
        };
    }

    private static void fireLinkPickerFactoryEvent(final String eventName, final String... params) {
        final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (!target.isPresent()) {
            log.info("Cannot invoke callback for event '{}': no Ajax request target available", eventName);
            return;
        }

        final StringBuilder args = new StringBuilder();
        for (final String param : params) {
            args.append(String.format(", '%s'", StringEscapeUtils.escapeJavaScript(param)));
        }

        final String script = String.format("Hippo.ChannelManager.ExtLinkPickerFactory.Instance.fireEvent('%s'%s);",
                StringEscapeUtils.escapeJavaScript(eventName), args);

        target.get().prependJavaScript(script);
    }

    /**
     * Model that fires a 'picked' event when events are enabled and a new path is set. By default, events are
     * disabled.
     */
    public class PickedNodeModel extends Model<DocumentLinkInfo> {

        private boolean enabledEvents;

        public PickedNodeModel() {
            enabledEvents = false;
        }

        public void enableEvents() {
            enabledEvents = true;
        }

        @Override
        public void setObject(final DocumentLinkInfo documentLinkInfo) {
            super.setObject(documentLinkInfo);

            if (enabledEvents) {
                fireLinkPickerFactoryEvent("picked", documentLinkInfo.getPath(), documentLinkInfo.getDocumentName());
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final PickedNodeModel that = (PickedNodeModel) o;
            return enabledEvents == that.enabledEvents;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), enabledEvents);
        }
    }
}
