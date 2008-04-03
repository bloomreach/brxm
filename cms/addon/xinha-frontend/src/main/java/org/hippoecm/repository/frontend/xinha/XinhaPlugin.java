/*
 * Copyright 2007 Hippo
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
package org.hippoecm.repository.frontend.xinha;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.form.TextArea;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(XinhaPlugin.class);

    public final static String XINHA_SAVED_FLAG = "XINHA_SAVED_FLAG";

    private JcrPropertyValueModel valueModel;

    private String content;
    private TextArea editor;
    private Configuration configuration;
    private XinhaEditorBehavior sharedBehavior;
    private AbstractDefaultAjaxBehavior postBehavior;

    public XinhaPlugin(PluginDescriptor pluginDescriptor, final IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        valueModel = model.getJcrPropertyValueModel();

        editor = new TextArea("value", valueModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                PluginDescriptor descriptor = getDescriptor();
                StringBuilder sb = new StringBuilder();
                ParameterValue value = descriptor.getParameter("width");
                if (value != null) {
                    sb.append("width: ");
            if(value.getStrings().size() > 0)
                        sb.append(value.getStrings().get(0));
                    sb.append(";");
                }
                value = descriptor.getParameter("height");
                if (value != null) {
                    sb.append("height: ");
            if(value.getStrings().size() > 0)
                        sb.append(value.getStrings().get(0));
                    sb.append(";");
                }
                if (sb.length() > 0) {
                    tag.put("style", sb.toString());
                }
                super.onComponentTag(tag);
            }
        };

        postBehavior = new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget target) {
                RequestCycle requestCycle = RequestCycle.get();
                boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save")).booleanValue();
                if (save) {
                    editor.processInput();
                    target.appendJavascript(XINHA_SAVED_FLAG);
                }

                String browse = requestCycle.getRequest().getParameter("browse");
                if (browse != null) {
                    
                    if ("".equals(browse)) {
                        browse = "/";
                    }

                    StringBuilder sb = new StringBuilder();

                    //TODO: implement browsing through FolderTreeNode
                    //custom handle browsing
                    Node pluginNode = (Node) pluginModel.getObject();                    
                    try {
                        Session session = pluginNode.getSession();
                        Item item = session.getItem(browse);
                        if (item.isNode()) {
                            Node itemNode = (Node) item;
                            NodeIterator iterator = itemNode.getNodes();
                            while (iterator.hasNext()) {
                                Node childNode = iterator.nextNode();
                                Node canonicalNode = ((HippoNode)childNode).getCanonicalNode();
                                if(canonicalNode != null && canonicalNode.isSame(childNode)) {
                                    sb.append("{");
                                    sb.append("title: '").append(childNode.getName()).append("'");
                                    boolean isHandle = childNode.isNodeType(HippoNodeType.NT_HANDLE); 
                                    sb.append(", clickable: ").append(isHandle);
                                    sb.append(", url: '").append(childNode.getPath()).append("'");
                                    if (childNode.getNodes().hasNext() && !isHandle) {
                                        sb.append(", children: []");
                                    }
                                    sb.append("}");
                                    if (iterator.hasNext()) {
                                        sb.append(",");
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    }
                    
                    target.appendJavascript("[" + sb.toString() + "]");

                }
            }
        };

        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        editor.add(postBehavior);

        add(this.new XinhaHeaderContributor());
        add(editor);

        //on tab-switch a new configuration is created while an existing on is in the sharedBehaviour
        //does provide a way to change configurations dynamically without implementing cache
        configuration = new Configuration(pluginDescriptor.getParameters());

        Page page = getPluginPage();
        for (Iterator iter = page.getBehaviors().iterator(); iter.hasNext();) {
            IBehavior behavior = (IBehavior) iter.next();
            if (behavior instanceof XinhaEditorBehavior) {
                sharedBehavior = (XinhaEditorBehavior) behavior;
                break;
            }
        }
        if (sharedBehavior == null) {
            sharedBehavior = new XinhaEditorBehavior(page);
        }
        sharedBehavior.register(configuration);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public void onBeforeRender() {
        configuration.setName(editor.getMarkupId());
        configuration.addProperty("callbackUrl", postBehavior.getCallbackUrl().toString());
        configuration.addProperty("saveSuccessFlag", XINHA_SAVED_FLAG);
        super.onBeforeRender();
    }

    @Override
    public void onDestroy() {
        if (sharedBehavior != null) {
            sharedBehavior.unregister(configuration);
            sharedBehavior = null;
        }
        super.onDestroy();
    }

    //Move this to editorBehaviour?
    class XinhaHeaderContributor extends AbstractHeaderContributor {
        private static final long serialVersionUID = 1L;

        @Override
        public final IHeaderContributor[] getHeaderContributors() {
            if (sharedBehavior != null) {
                return sharedBehavior.getHeaderContributorsPartly();
            } else {
                return null;
            }
        }
    }

    class Configuration extends BaseConfiguration {
        private static final long serialVersionUID = 1L;

        private static final String XINHA_PREFIX = "Xinha.";
        private static final String XINHA_CONFIG_PREFIX = "Xinha.config.";
        private static final String XINHA_PLUGINS = "Xinha.plugins";
        private static final String XINHA_TOOLBAR = "Xinha.config.toolbar";
        private static final String XINHA_CSS = "Xinha.config.css";
        private static final String XINHA_SKIN = "Xinha.skin";
        

        private Map<String, PluginConfiguration> pluginConfigurations = new HashMap<String, PluginConfiguration>();
        private List<String> toolbarItems;
        private List<String> styleSheets;
        private String skin;

        public Configuration(Map<String, ParameterValue> parameters) {

            for (String paramKey : parameters.keySet()) {
                if (paramKey.startsWith(XINHA_PREFIX)) {
                    List<String> paramValues = parameters.get(paramKey).getStrings();
                    if (paramKey.equals(XINHA_TOOLBAR)) {
                        toolbarItems = paramValues;
                    } else if (paramKey.equals(XINHA_CSS)) {
                        styleSheets = paramValues;
                    } else if(paramKey.equals(XINHA_SKIN)) {
                        skin = paramValues.get(0);
                    } else if (paramKey.equals(XINHA_PLUGINS)) {
                        for (String pluginName : paramValues) {
                            PluginConfiguration pluginConfig = new PluginConfiguration(pluginName);
                            ParameterValue pluginParameter = parameters.get(pluginName);
                            if (pluginParameter != null) {
                                List<String> pluginProperties = pluginParameter.getStrings();
                                if (pluginProperties != null) {
                                    for (String pluginProperty : pluginProperties) {
                                        pluginConfig.addProperty(pluginProperty);
                                    }
                                }
                            }
                            addPluginConfiguration(pluginConfig);
                        }
                    } else {
                        for (String propertyValue : paramValues) {
                            int equalsIndex = propertyValue.indexOf("=");
                            if (equalsIndex > -1) {
                                addProperty(propertyValue.substring(0, equalsIndex), propertyValue
                                        .substring(equalsIndex + 1));
                            } else {
                                String propertyKey = paramKey.substring(XINHA_CONFIG_PREFIX.length());
                                if (getProperties().containsKey(propertyKey)) {
                                    addProperty(propertyKey, getProperty(propertyKey) + "," + propertyValue);
                                } else {
                                    addProperty(propertyKey, propertyValue);
                                }
                            }
                        }
                    }
                }
            }
        }

        public List<String> getToolbarItems() {
            return toolbarItems;
        }

        public List<String> getStyleSheets() {
            return styleSheets;
        }
        
        public String getSkin() {
            return skin;
        }

        public void setPluginConfigurations(Set<PluginConfiguration> plugins) {
            for (PluginConfiguration conf : plugins) {
                addPluginConfiguration(conf);
            }
        }

        public void addPluginConfiguration(PluginConfiguration config) {
            if (!pluginConfigurations.containsKey(config.getName())) {
                pluginConfigurations.put(config.getName(), config);
            } else {
                pluginConfigurations.get(config.getName()).addProperties(config.getProperties());
            }
        }

        public PluginConfiguration getPluginConfiguration(String name) {
            return pluginConfigurations.get(name);
        }

        public Set<PluginConfiguration> getPluginConfigurations() {
            Set<PluginConfiguration> returnSet = new HashSet<PluginConfiguration>();
            returnSet.addAll(pluginConfigurations.values());
            return returnSet;
        }

    }

    class PluginConfiguration extends BaseConfiguration {

        private static final long serialVersionUID = 1L;

        public PluginConfiguration(String name) {
            setName(name);
        }

        public void addProperty(String keyValue) {
            int equalsIndex = keyValue.indexOf("=");
            if (equalsIndex == -1) {
                throw new IllegalArgumentException("Invalid key/value argument, no seperator found: " + keyValue);
            }
            addProperty(keyValue.substring(0, equalsIndex), keyValue.substring(equalsIndex + 1));
        }
    }

    class BaseConfiguration implements Serializable {
        private static final long serialVersionUID = 1L;

        //id
        private String name = null;

        //properties
        private Map<String, String> properties = new HashMap<String, String>();

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        //override all properties
        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        //merge properties
        public void addProperties(Map<String, String> properties) {
            for (String key : properties.keySet()) {
                addProperty(key, properties.get(key));
            }
        }

        public void addProperty(String name, String value) {
            properties.put(name, value);
        }

        public String getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Configuration) {
                if (name != null) {
                    return name.equals(((Configuration) o).getName());
                } else {
                    return ((Configuration) o).getName() == null;
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return XinhaPlugin.this.hashCode();
        }
    }
}
