/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.ClearableDialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorTemplatePlugin extends RenderPlugin<Node> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MirrorTemplatePlugin.class);

    private static final String EMPTY_LINK_TEXT = "[...]";

    private final String mode;

    public MirrorTemplatePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> displayModel = new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                Node node = MirrorTemplatePlugin.this.getModelObject();
                try {
                    if (node != null && node.hasProperty("hippo:docbase")) {
                        String docbaseUUID = node.getProperty("hippo:docbase").getString();
                        if (docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.startsWith("cafebabe-")) {
                            return EMPTY_LINK_TEXT;
                        }
                        return node.getSession().getNodeByUUID(docbaseUUID).getPath();
                    }
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase " + e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (RepositoryException e) {
                    log.error("Invalid docbase" + e.getMessage(), e);
                }
                return EMPTY_LINK_TEXT;
            }
        };

        mode = config.getString("mode", "view");
        if ("edit".equals(mode)) {
            final List<String> nodetypes = new ArrayList<String>();
            if (config.getStringArray("nodetypes") != null) {
                String[] nodeTypes = config.getStringArray("nodetypes");
                nodetypes.addAll(Arrays.asList(nodeTypes));
            }
            if (nodetypes.size() == 0) {
                log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
            }
            //add(new TextFieldWidget("docbase", docbaseModel));
            IDialogFactory dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<String> createDialog() {
                    JcrNodeModel jcrNodeModel = (JcrNodeModel) MirrorTemplatePlugin.this.getDefaultModel();
                    final JcrPropertyValueModel<String> docbaseModel = new JcrPropertyValueModel<String>(
                            new JcrPropertyModel<String>(jcrNodeModel.getItemModel().getPath() + "/hippo:docbase"));
                    return new LinkPickerDialog(context, getPluginConfig(), new IChainingModel<String>() {
                        private static final long serialVersionUID = 1L;

                        public String getObject() {
                            return docbaseModel.getObject();
                        }

                        public void setObject(String object) {
                            docbaseModel.setObject(object);
                            redraw();
                        }

                        public IModel<String> getChainedModel() {
                            return docbaseModel;
                        }

                        public void setChainedModel(IModel<?> model) {
                            throw new UnsupportedOperationException("Value model cannot be changed");
                        }

                        public void detach() {
                            docbaseModel.detach();
                        }
                    }, nodetypes);
                }
            };
            add(new ClearableDialogLink("docbase", displayModel, dialogFactory, getDialogService()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClear() {
                    Node node = ((JcrNodeModel) MirrorTemplatePlugin.this.getDefaultModel()).getNode();
                    try {
                        node.setProperty("hippo:docbase", node.getSession().getRootNode().getUUID());
                    } catch (RepositoryException e) {
                        log.error("Unable to reset docbase to rootnode uuid", e);
                    }
                    redraw();
                }

                @Override
                public boolean isClearVisible() {
                    // Checking for string literals ain't pretty. It's probably better to create a better display model.
                    return !EMPTY_LINK_TEXT.equals((String) displayModel.getObject());
                }
            });
        } else {
            add(new Label("docbase", displayModel));
        }
        setOutputMarkupId(true);
    }
}
