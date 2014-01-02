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

package org.onehippo.cms7.channelmanager.channels;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelNotFoundException;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.FieldGroupInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.onehippo.cms7.channelmanager.ChannelManagerHeaderItem;
import org.onehippo.cms7.channelmanager.model.AbsoluteRelativePathModel;
import org.onehippo.cms7.channelmanager.model.UuidFromPathModel;
import org.onehippo.cms7.channelmanager.widgets.DropDownListWidget;
import org.onehippo.cms7.channelmanager.widgets.ImageSetPathWidget;
import org.onehippo.cms7.channelmanager.widgets.JcrPathWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.data.ActionFailedException;
import org.wicketstuff.js.ext.form.ExtFormPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;

@ExtClass(ChannelPropertiesWindow.EXT_CLASS)
public class ChannelPropertiesWindow extends ExtFormPanel {

    static final Logger log = LoggerFactory.getLogger(ChannelPropertiesWindow.class);

    public static final String CHANNEL_PROPERTIES_WINDOW_JS = "ChannelPropertiesWindow.js";
    public static final String EXT_CLASS = "Hippo.ChannelManager.ChannelPropertiesWindow";

    private static final String EVENT_SAVE_CHANNEL = "savechannel";
    private static final String EVENT_SELECT_CHANNEL = "selectchannel";
    private static final String EVENT_SELECT_CHANNEL_PARAM_ID = "id";
    private static final String WICKET_ID_FIELDGROUPS = "fieldgroups";
    private static final String WICKET_ID_FIELDGROUPTITLE = "fieldgrouptitle";
    private static final String WICKET_ID_KEY = "key";
    private static final String WICKET_ID_PROPERTIES = "properties";
    private static final String WICKET_ID_VALUE = "value";
    private static final String WICKET_ID_HELP = "help";
    private static final String HELP_SUFFIX = ".help";
    private static final long serialVersionUID = 1L;

    private Channel channel;
    private String channelPropertiesContainerClass = "hide-channel-properties";
    private final ChannelStore channelStore;

    private class ChannelChoiceRenderer implements IChoiceRenderer<String> {

        private static final long serialVersionUID = 1L;

        private final String key;

        public ChannelChoiceRenderer(final String key) {
            this.key = key;
        }

        @Override
        public String getDisplayValue(final String object) {
            try {
                Properties resourcesPorps = channelStore.getChannelResourceValues(channel);
                if (resourcesPorps != null) {
                    return resourcesPorps.getProperty(key + "/" + object);
                }
            } catch (ChannelException ce) {
                if (log.isDebugEnabled()) {
                    log.warn("Could not get display value of '" + object + "' for channel with id '" + channel.getId() + "'", ce);
                } else {
                    log.warn("Could not get display value of '{}' for channel with id '{}' - {}",
                            new String[] {object, channel.getId(), ce.toString()});

                }
            }

            return null;
        }

        @Override
        public String getIdValue(final String object, final int index) {
            return object;
        }
    }

    public ChannelPropertiesWindow(final IPluginContext context, final ChannelStore channelStore) {
        this.channelStore = channelStore;

        final WebMarkupContainer container = new WebMarkupContainer("channel-properties-container");
        container.add(new AttributeModifier("class", true, new PropertyModel(this, "channelPropertiesContainerClass")));
        container.add(new ListView<FieldGroupInfo>(WICKET_ID_FIELDGROUPS, new LoadableDetachableModel<List<FieldGroupInfo>>() {
            @Override
                    protected List<FieldGroupInfo> load() {
                        return getFieldGroups();
                    }
                }) {
            @Override
            protected void populateItem(final ListItem<FieldGroupInfo> item) {
                final FieldGroupInfo fieldGroup = item.getModelObject();

                item.add(new Label(WICKET_ID_FIELDGROUPTITLE, new ChannelResourceModel(fieldGroup.getTitleKey(), channel, channelStore)) {
                    @Override
                    public boolean isVisible() {
                        return getModelObject() != null;
                    }
                });

                item.add(new ListView<String>(WICKET_ID_PROPERTIES, new LoadableDetachableModel<List<String>>() {
                    @Override
                    protected List<String> load() {
                        if (channel == null) {
                            return Collections.emptyList();
                        } else {
                            String[] strings = fieldGroup.getValue();
                            List<String> keys = new ArrayList<String>(strings.length);
                            for (String key : strings) {
                                if (getPropertyDefinition(key) != null) {
                                    keys.add(key);
                                }
                            }
                            return keys;
                        }
                    }

                }) {
                    @Override
                    protected void populateItem(final ListItem<String> item) {
                        final String key = item.getModelObject();
                        final HstPropertyDefinitionInfo propDefInfo = getPropertyDefinition(key);
                        Annotation parameterAnnotation = propDefInfo.getAnnotation(Parameter.class);
                        if (parameterAnnotation != null && ((Parameter) parameterAnnotation).hideInChannelManager()) {
                            return;
                        }
                        final Label label = new Label(WICKET_ID_KEY, new ChannelResourceModel(key, channel, channelStore));
                        item.add(label);
                        final Component widget = getWidget(context, channel, propDefInfo, key);
                        item.add(widget);

                        final String cmsUser = UserSession.get().getJcrSession().getUserID();
                        final String channelNodeLockedBy = channel.getChannelNodeLockedBy();
                        if (StringUtils.isNotEmpty(channelNodeLockedBy) && !channelNodeLockedBy.equals(cmsUser)) {
                            label.setEnabled(false);
                            widget.setEnabled(false);
                        }

                        // add help text
                        final IModel<String> helpModel = new ChannelResourceModel(key + HELP_SUFFIX, channel, channelStore);
                        final Label helpLabel = new Label(WICKET_ID_HELP, helpModel);
                        helpLabel.setVisible(StringUtils.isNotBlank(helpModel.getObject()));
                        item.add(helpLabel);
                    }
                });
            }
        });

        container.setOutputMarkupId(true);
        add(container);

        addEventListener(EVENT_SELECT_CHANNEL, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                if (parameters.containsKey(EVENT_SELECT_CHANNEL_PARAM_ID)) {
                    JSONArray jsonChannelId = parameters.get(EVENT_SELECT_CHANNEL_PARAM_ID);
                    if (jsonChannelId.length() > 0) {
                        try {
                            String channelId = (String) jsonChannelId.get(0);
                            if (!channelId.endsWith("-preview")) {
                                channelId = channelId + "-preview";
                            }
                            channel = getChannel(channelId);
                            channelPropertiesContainerClass = "channel-properties";
                        } catch (JSONException e) {
                            log.error("Invalid JSON", e);
                        }
                    }
                }
                target.add(container);
            }
        });
        addEventListener(EVENT_SAVE_CHANNEL, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                try {
                    save();
                } catch (ActionFailedException e) {
                    target.appendJavaScript("(function(instance) {Hippo.Msg.alert(instance.resources['channel-properties-editor-error'], instance.resources['could-not-save-changes'], function(id) {\n" +
                            "                instance.pageContainer.refreshIframe();\n" +
                            "            }, instance); })(Hippo.ChannelManager.TemplateComposer.Instance)");
                }

                channelStore.reload();
                target.prependJavaScript("Hippo.ChannelManager.TemplateComposer.Instance.refreshIframe();");
            }
        });
    }

    private Component getWidget(IPluginContext context, Channel channel, HstPropertyDefinitionInfo propDefInfo, String key) {
        final HstValueType propType = propDefInfo.getValueType();

        // render an image set field?
        ImageSetPath imageSetPath = (ImageSetPath) propDefInfo.getAnnotation(ImageSetPath.class);
        if (imageSetPath != null && propType.equals(HstValueType.STRING)) {
            IModel<String> delegate = new StringModel(channel.getProperties(), key);
            IModel<String> model = new UuidFromPathModel(delegate);
            return new ImageSetPathWidget(context, WICKET_ID_VALUE, imageSetPath, model);
        }

        // render a JCR path field?
        JcrPath jcrPath = (JcrPath) propDefInfo.getAnnotation(JcrPath.class);
        if (jcrPath != null && propType.equals(HstValueType.STRING)) {
            IModel<String> delegate = new StringModel(channel.getProperties(), key);
            IModel<String> absToRelModel = new AbsoluteRelativePathModel(delegate, delegate.getObject(),
                    jcrPath.isRelative(), channel.getContentRoot());
            IModel<String> uuidFromPathModel = new UuidFromPathModel(absToRelModel);
            return new JcrPathWidget(context, WICKET_ID_VALUE, jcrPath, channel.getContentRoot(), uuidFromPathModel);
        }

        // render a drop-down list?
        DropDownList dropDownList = (DropDownList) propDefInfo.getAnnotation(DropDownList.class);
        if (dropDownList != null) {
            IModel<String> model = new StringModel(channel.getProperties(), key);
            return new DropDownListWidget(WICKET_ID_VALUE, dropDownList, model, new ChannelChoiceRenderer(key));
        }

        // render a boolean field?
        if (propType.equals(HstValueType.BOOLEAN)) {
            return new BooleanFieldWidget(WICKET_ID_VALUE, new BooleanModel(channel.getProperties(), key));
        }

        // default: render a text field
        return new TextFieldWidget(WICKET_ID_VALUE, new StringModel(channel.getProperties(), key));
    }

    public String getChannelPropertiesContainerClass() {
        return channelPropertiesContainerClass;
    }

    private void save() throws ActionFailedException {
        if (channel == null) {
            return;
        }

        channelStore.saveChannel(channel);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(ChannelManagerHeaderItem.get());
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        if (EVENT_SELECT_CHANNEL.equals(event)) {
            return new ExtEventAjaxBehavior(EVENT_SELECT_CHANNEL_PARAM_ID);
        } else if (EVENT_SAVE_CHANNEL.equals(event)) {
            return new ExtEventAjaxBehavior();
        }
        return super.newExtEventBehavior(event);
    }

    private List<FieldGroupInfo> getFieldGroups() {
        if (channel == null) {
            return Collections.emptyList();
        }

        final ChannelInfoClassInfo channelInfoClassInfo;
        try {
            channelInfoClassInfo = channelStore.getChannelInfoClassInfo(channel);
        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve channel info class for channel with id '" + channel.getId() + "'", ce);
            } else {
                log.warn("Failed to retrieve channel info class for channel with id '{}' - {}", channel.getId(), ce.toString());
            }

            return Collections.emptyList();
        }

        if (channelInfoClassInfo == null) {
            return Collections.emptyList();
        }

        List<FieldGroupInfo> fieldGroupList = channelInfoClassInfo.getFieldsGroup();

        if (fieldGroupList == null) {
            log.warn("Channel info class '{}' contains a '{}' annotation with a null value: no channel properties will be shown",
                    channelInfoClassInfo.getClassName(), FieldGroupList.class.getName());
            return Collections.emptyList();
        } else if (fieldGroupList.size() == 0) {
            log.warn("Channel info class '{}' does not contain any '{}' annotations: no channel properties will be shown",
                    channelInfoClassInfo.getClassName(), FieldGroup.class.getName());
            return Collections.emptyList();
        }

        return fieldGroupList;
    }

    private Channel getChannel(String channelId) {
        try {
            return channelStore.getChannel(channelId);
        } catch (ChannelNotFoundException cnfe) {
            throw new RuntimeException("Unable to get channel with id '" + channelId + "'from Channel Manager", cnfe);
        }
    }

    private HstPropertyDefinitionInfo getPropertyDefinition(String propertyName) {
        for (HstPropertyDefinitionInfo definition : channelStore.getChannelPropertyDefinitions(channel)) {
            if (definition.getName().equals(propertyName)) {
                return definition;
            }
        }

        log.warn("Could not find definition for property '{}'", propertyName);
        return null;
    }

    /**
     * Abstract model that can store a string in a map under a certain key.
     *
     * @param <T> the type of the model's object
     */
    private abstract class AbstractPropertiesModel<T> implements IModel<T> {

        final Map<String, Object> properties;
        final String key;

        AbstractPropertiesModel(final Map<String, Object> properties, final String key) {
            this.properties = properties;
            this.key = key;
        }

        protected void setObjectFromString(final String s) {
            HstPropertyDefinitionInfo def = getPropertyDefinition(key);
            if (def != null) {
                properties.put(key, def.getValueType().from(s));
            }
        }

        @Override
        public void detach() {
        }

    }

    /**
     * Model that stores a string in a map under a certain key.
     */
    private class StringModel extends AbstractPropertiesModel<String> {

        private static final long serialVersionUID = 1L;

        StringModel(final Map<String, Object> properties, final String key) {
            super(properties, key);
        }

        @Override
        public String getObject() {
            Object value = properties.get(key);
            return value == null ? null : value.toString();
        }

        @Override
        public void setObject(String s) {
            setObjectFromString(s);
        }

    }

    /**
     * Model that converts Booleans to strings and stores the strings.
     */
    private class BooleanModel extends AbstractPropertiesModel<Boolean> {

        private static final long serialVersionUID = 1L;

        BooleanModel(final Map<String, Object> properties, final String key) {
            super(properties, key);
        }

        @Override
        public Boolean getObject() {
            Object value = properties.get(key);
            return value == null ? null : Boolean.valueOf(value.toString());
        }

        @Override
        public void setObject(final Boolean b) {
            setObjectFromString(b.toString());
        }

    }

}
