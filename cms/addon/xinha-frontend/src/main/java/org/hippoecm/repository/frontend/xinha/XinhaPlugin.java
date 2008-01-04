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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.template.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.ITemplatePlugin;
import org.hippoecm.frontend.plugins.template.TemplateEngine;

public class XinhaPlugin extends Plugin implements ITemplatePlugin {
    private static final long serialVersionUID = 1L;

    private FieldDescriptor descriptor;
    private TemplateEngine engine;

    private String content;
    private TextArea editor;
    private Configuration configuration;
    private XinhaEditorBehavior sharedBehavior;
    private AbstractDefaultAjaxBehavior postBehavior;

    public XinhaPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(descriptor, model, parentPlugin);

        add(new Label("name", new PropertyModel(this, "name")));
        editor = new TextArea("value", getModel());

        postBehavior = new AbstractDefaultAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    final String saveCall = "{wicketAjaxGet('" + getCallbackUrl()
                        + "&save=true&'+this.name+'='+wicketEncode(this.value)); return false;}";
                    tag.put("onblur", saveCall);
                }

                protected void respond(AjaxRequestTarget target) {
                    RequestCycle requestCycle = RequestCycle.get();
                    boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save")).booleanValue();
                    if (save) {
                        editor.processInput();
                    }
                }
            };

        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        editor.add(postBehavior);
        add(this . new XinhaHeaderContributor());
        add(editor);

        configuration = this . new Configuration();
        configuration.setPlugins(new String[] { "CharacterMap", "ContextMenu", "ListType", "SpellChecker",
                                                "SuperClean", "Stylist","TableOperations", "SaveSubmit" });
    }

    public String getName() {
        if(descriptor != null) {
            return descriptor.getName();
        }
        return null;
    }

    public void initTemplatePlugin(FieldDescriptor descriptor, TemplateEngine engine) {
        this.descriptor = descriptor;
        this.engine = engine;

        setModel(getModel());
    }

    @Override
    public Component setModel(IModel model) {
        JcrNodeModel jcrModel = (JcrNodeModel) model;
        try {
            Property property = null;
            if(jcrModel.getNode().hasProperty(descriptor.getPath())) {
                property = jcrModel.getNode().getProperty(descriptor.getPath());
            } else {
                property = jcrModel.getNode().setProperty(descriptor.getPath(), "");
            }
            if (property.getDefinition().isMultiple()) {
                editor.setModel(null);
                return super.setModel(model);
            }
            JcrPropertyModel propModel = new JcrPropertyModel(property);
            editor.setModel(new JcrPropertyValueModel(0, property.getValue().getString(), propModel));
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
        return super.setModel(model);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void onBeforeRender() {
        if (sharedBehavior == null) {
            Page page = findPage();
            for(Iterator iter = page.getBehaviors().iterator(); iter.hasNext(); ) {
                IBehavior behavior = (IBehavior) iter.next();
                if(behavior instanceof XinhaEditorBehavior) {
                    sharedBehavior = (XinhaEditorBehavior) behavior;
                    break;
                }
            }
            if(sharedBehavior == null) {
                sharedBehavior = new XinhaEditorBehavior(page);
            }
        }

        configuration.setName(editor.getMarkupId());
        Map m = new Hashtable();
        m.put("postUrl", postBehavior.getCallbackUrl());
        configuration.setConfiguration(m);

        sharedBehavior.register(configuration);

        super.onBeforeRender();
    }

    protected void onDetach() {
        super.onDetach();
        if(sharedBehavior != null) {
                sharedBehavior.unregister(configuration);
                sharedBehavior = null;
            }
    }

    class XinhaHeaderContributor extends AbstractHeaderContributor {
        public final IHeaderContributor[] getHeaderContributors() {
            if(sharedBehavior != null) {
                return sharedBehavior.getHeaderContributorsPartly();
            } else {
                return null;
            }
        }
    }

    class Configuration implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name = null;
        private String[] plugins = null;
        private Map configuration = null;

        public Configuration() {
        }

        public void setPlugins(String[] plugins) {
            this.plugins = plugins;
        }

        public String[] getPlugins() {
            return plugins;
        }

        public void setConfiguration(Map configuration) {
            this.configuration = configuration;
        }

        public Map getConfiguration() {
            return configuration;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

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

        public int hashCode() {
            return name.hashCode();
        }
    }
}
