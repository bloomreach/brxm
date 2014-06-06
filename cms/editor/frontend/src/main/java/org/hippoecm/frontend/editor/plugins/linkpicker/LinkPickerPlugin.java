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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
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
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerPlugin extends RenderPlugin<String> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    private static final String EMPTY_LINK_TEXT = "[...]";

    private IModel<String> valueModel;
    private List<String> nodetypes = new ArrayList<String>();

    public LinkPickerPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        valueModel = getModel();

        final IModel<String> displayModel = new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                String docbaseUUID = valueModel.getObject();
                if (docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.startsWith("cafebabe-")) {
                    return EMPTY_LINK_TEXT;
                }
                try {
                    return UserSession.get().getJcrSession().getNodeByUUID(docbaseUUID).getPath();
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase " + e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (ItemNotFoundException e) {
                    log.info("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (RepositoryException e) {
                    log.error("Invalid docbase " + e.getMessage(), e);
                }
                return EMPTY_LINK_TEXT;
            }
        };

        if ("edit".equals(config.getString("mode", "view"))) {
            IDialogFactory dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<String> createDialog() {
                    final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(getPluginConfig(), (JcrPropertyValueModel) valueModel);
                    return new LinkPickerDialog(context, dialogConfig, new IChainingModel<String>() {
                        private static final long serialVersionUID = 1L;

                        public String getObject() {
                            return valueModel.getObject();
                        }

                        public void setObject(String object) {
                            valueModel.setObject(object);
                            redraw();
                        }

                        public IModel<String> getChainedModel() {
                            return valueModel;
                        }

                        public void setChainedModel(IModel<?> model) {
                            throw new UnsupportedOperationException("Value model cannot be changed");
                        }

                        public void detach() {
                            valueModel.detach();
                        }

                    });
                }
            };

            add(new ClearableDialogLink("value", displayModel, dialogFactory, getDialogService()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClear() {
                    try {
                        valueModel.setObject(UserSession.get().getJcrSession().getRootNode().getUUID());
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
            add(new Label("value", displayModel));
        }
        setOutputMarkupId(true);
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

}
