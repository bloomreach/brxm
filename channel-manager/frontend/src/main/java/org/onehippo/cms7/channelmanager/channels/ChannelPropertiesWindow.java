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

package org.onehippo.cms7.channelmanager.channels;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Credentials;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
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
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.onehippo.cms7.channelmanager.model.UuidFromPathModel;
import org.onehippo.cms7.channelmanager.widgets.DropDownListWidget;
import org.onehippo.cms7.channelmanager.widgets.ImageSetPathWidget;
import org.onehippo.cms7.channelmanager.widgets.JcrPathWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtBoxComponent;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
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
    private static final FieldGroup[] ZERO_FIELD_GROUPS = new FieldGroup[0];
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

    private class ChannelChoiceRenderer implements IChoiceRenderer<String> {

        private static final long serialVersionUID = 1L;

        private final String key;

        public ChannelChoiceRenderer(final String key) {
            this.key = key;
        }

        @Override
        public Object getDisplayValue(final String object) {
            ResourceBundle resources = ChannelUtil.getResourceBundle(channel);
            if (resources != null) {
                return resources.getString(key + "/" + object);
            }
            return null;
        }

        @Override
        public String getIdValue(final String object, final int index) {
            return object;
        }
    }

    public ChannelPropertiesWindow(final IPluginContext context, final ChannelStore channelStore) {
        super();

        final WebMarkupContainer container = new WebMarkupContainer("channel-properties-container");
        container.add(new AttributeModifier("class", true, new PropertyModel(this, "channelPropertiesContainerClass")));
        container.add(new ListView<FieldGroup>(WICKET_ID_FIELDGROUPS, new LoadableDetachableModel<List<FieldGroup>>() {
            @Override
            protected List<FieldGroup> load() {
                return Arrays.asList(getFieldGroups());
            }
        }) {
            @Override
            protected void populateItem(final ListItem<FieldGroup> item) {
                final FieldGroup fieldGroup = item.getModelObject();

                item.add(new Label(WICKET_ID_FIELDGROUPTITLE, new ChannelResourceModel(channel, fieldGroup.titleKey())) {
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
                            return Arrays.asList(fieldGroup.value());
                        }
                    }

                }) {
                    @Override
                    protected void populateItem(final ListItem<String> item) {
                        final String key = item.getModelObject();
                        HstPropertyDefinition propDef = getPropertyDefinition(key);

                        if (propDef == null) {
                            return;
                        }

                        item.add(new Label(WICKET_ID_KEY, new ChannelResourceModel(channel, key)));

                        HstValueType propType = propDef.getValueType();

                        // render an image set field?
                        ImageSetPath imageSetPath = propDef.getAnnotation(ImageSetPath.class);
                        if (imageSetPath != null && propType.equals(HstValueType.STRING)) {
                            IModel<String> delegate = new StringModel(channel.getProperties(), key);
                            IModel<String> model = new UuidFromPathModel(delegate);
                            item.add(new ImageSetPathWidget(context, WICKET_ID_VALUE, imageSetPath, model));
                            return;
                        }

                        // render a JCR path field?
                        JcrPath jcrPath = propDef.getAnnotation(JcrPath.class);
                        if (jcrPath != null && propType.equals(HstValueType.STRING)) {
                            IModel<String> delegate = new StringModel(channel.getProperties(), key);
                            IModel<String> model = new UuidFromPathModel(delegate);
                            item.add(new JcrPathWidget(context, WICKET_ID_VALUE, jcrPath, model));
                            return;
                        }

                        // render a drop-down list?
                        DropDownList dropDownList = propDef.getAnnotation(DropDownList.class);
                        if (dropDownList != null) {
                            IModel<String> model = new StringModel(channel.getProperties(), key);
                            item.add(new DropDownListWidget(WICKET_ID_VALUE, dropDownList, model, new ChannelChoiceRenderer(key)));
                            return;
                        }

                        // render a boolean field?
                        if (propType.equals(HstValueType.BOOLEAN)) {
                            item.add(new BooleanFieldWidget(WICKET_ID_VALUE, new BooleanModel(channel.getProperties(), key)));
                            return;
                        }

                        // default: render a text field
                        item.add(new TextFieldWidget(WICKET_ID_VALUE, new StringModel(channel.getProperties(), key)));

                        // add help text
                        IModel<String> helpModel = new ChannelResourceModel(channel, key + HELP_SUFFIX);
                        Label helpLabel = new Label(WICKET_ID_HELP, helpModel);
                        helpLabel.setVisible(StringUtils.isNotBlank(helpModel.getObject()));
                        item.add(helpLabel);
                    }
                });
            }
        });

        container.setOutputMarkupId(true);
        ExtBoxComponent box = new ExtBoxComponent();
        box.add(container);
        add(box);

        addEventListener(EVENT_SELECT_CHANNEL, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                if (parameters.containsKey(EVENT_SELECT_CHANNEL_PARAM_ID)) {
                    JSONArray channelId = parameters.get(EVENT_SELECT_CHANNEL_PARAM_ID);
                    if (channelId.length() > 0) {
                        try {
                            channel = getChannel((String) channelId.get(0));
                            channelPropertiesContainerClass = "channel-properties";
                        } catch (JSONException e) {
                            log.error("Invalid JSON", e);
                        }
                    }
                }
                target.addComponent(container);
            }
        });
        addEventListener(EVENT_SAVE_CHANNEL, new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget target, final Map<String, JSONArray> parameters) {
                save();
                channelStore.reload();
                target.prependJavascript("Hippo.ChannelManager.TemplateComposer.Instance.refreshIframe();");
            }
        });
    }

    public String getChannelPropertiesContainerClass() {
        return channelPropertiesContainerClass;
    }

    private void save() {
        if (channel == null) {
            return;
        }
        // FIXME: move boilerplate to CMS engine
        UserSession session = (UserSession) Session.get();

        @SuppressWarnings("deprecation")
        Credentials credentials = session.getCredentials();

        Subject subject = new Subject();
        subject.getPrivateCredentials().add(credentials);
        subject.setReadOnly();

        final ChannelManager channelManager = ChannelUtil.getChannelManager();
        if (channelManager == null) {
            log.warn("Cannot save channel '{}' because the channel manager cannot be loaded. Is the site running?", channel.getId());
            return;
        }
        
        try {
            HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<Void>() {
                public Void run() throws ChannelException {
                    channelManager.save(channel);
                    return null;
                }
            }, null);
        } catch (PrivilegedActionException e) {
            log.error("Could not save channel", e.getException());
        } finally {
            HstSubject.clearSubject();
        }
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

    private FieldGroup[] getFieldGroups() {
        if (channel == null) {
            return ZERO_FIELD_GROUPS;
        }

        Class<? extends ChannelInfo> channelInfoClass = ChannelUtil.getChannelInfoClass(channel);
        if (channelInfoClass == null) {
            return ZERO_FIELD_GROUPS;
        }

        FieldGroupList fieldGroupList = channelInfoClass.getAnnotation(FieldGroupList.class);

        if (fieldGroupList != null) {
            FieldGroup[] result = fieldGroupList.value();
            if (result == null) {
                log.warn("Channel info class '{}' contains a '{}' annotation with a null value: no channel properties will be shown",
                        channelInfoClass.getName(), FieldGroupList.class.getName());
                return ZERO_FIELD_GROUPS;
            } else if (result.length == 0) {
                log.warn("Channel info class '{}' does not contain any '{}' annotations: no channel properties will be shown",
                        channelInfoClass.getName(), FieldGroup.class.getName());
                return ZERO_FIELD_GROUPS;
            }
            return result;
        }

        return ZERO_FIELD_GROUPS;
    }

    private Channel getChannel(String channelId) {
        ChannelManager channelManager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
        if (channelManager != null) {
            try {
                return channelManager.getChannels().get(channelId);
            } catch (ChannelException e) {
                throw new RuntimeException("Unable to get the channels from Channel Manager", e);
            }
        } else {
            throw new RuntimeException("Unable to get the channels from Channel Manager");
        }
    }

    private HstPropertyDefinition getPropertyDefinition(String propertyName) {
        ChannelManager channelManager = ChannelUtil.getChannelManager();
        if (channelManager == null) {
            log.info("Could not load the channel manager: the definition for property '{}' is unknown", propertyName);
            return null;
        }
        
        for (HstPropertyDefinition definition : channelManager.getPropertyDefinitions(channel)) {
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
            HstPropertyDefinition def = getPropertyDefinition(key);
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
