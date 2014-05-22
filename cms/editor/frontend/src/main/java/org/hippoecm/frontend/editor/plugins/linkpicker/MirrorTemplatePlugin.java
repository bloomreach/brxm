/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
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
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorTemplatePlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MirrorTemplatePlugin.class);

    private static final String EMPTY_LINK_TEXT = "[...]";

    private final String mode;

    public MirrorTemplatePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> displayModel = getDisplayModel();

        mode = config.getString("mode", "view");
        if ("edit".equals(mode)) {
            IDialogFactory dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<String> createDialog() {
                    final JcrPropertyValueModel<String> docbaseModel = getDocBaseModel();
                    final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(getPluginConfig(), docbaseModel);
                    return new LinkPickerDialog(context, dialogConfig, new IChainingModel<String>() {
                        private static final long serialVersionUID = 1L;

                        public String getObject() {
                            return docbaseModel.getObject();
                        }

                        public void setObject(String uuid) {
                            docbaseModel.setObject(uuid);
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
                    });
                }
            };
            add(new ClearableDialogLink("docbase", displayModel, dialogFactory, getDialogService()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClear() {
                    Node node = MirrorTemplatePlugin.this.getModelObject();
                    try {
                        node.setProperty(HippoNodeType.HIPPO_DOCBASE, node.getSession().getRootNode().getUUID());
                    } catch (RepositoryException e) {
                        log.error("Unable to reset docbase to rootnode uuid", e);
                    }
                    redraw();
                }

                @Override
                public boolean isClearVisible() {
                    // Checking for string literals ain't pretty. It's probably better to create a better display model.
                    return !getEmptyLinkText().equals(displayModel.getObject());
                }
            });
        } else {
            add(new Label("docbase", displayModel));
        }
        setOutputMarkupId(true);
    }

    protected IModel<String> getDisplayModel() {
        return new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                Node node = MirrorTemplatePlugin.this.getModelObject();
                try {
                    if (node != null && node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        String docbaseUUID = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        if (docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.startsWith("cafebabe-")) {
                            return getEmptyLinkText();
                        }
                        return node.getSession().getNodeByUUID(docbaseUUID).getPath();
                    }
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase " + e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (ItemNotFoundException e) {
                    log.info("Docbase " + e.getMessage() + " could not be dereferenced");
                } catch (RepositoryException e) {
                    log.error("Invalid docbase " + e.getMessage(), e);
                }
                return getEmptyLinkText();
            }
        };
    }

    protected String getEmptyLinkText() {
        return EMPTY_LINK_TEXT;
    }

    protected JcrPropertyValueModel<String> getDocBaseModel() {
        return new JcrPropertyValueModel<String>(new JcrPropertyModel<String>(((JcrNodeModel) getModel())
                .getItemModel().getPath()
                + "/hippo:docbase"));
    }
}
