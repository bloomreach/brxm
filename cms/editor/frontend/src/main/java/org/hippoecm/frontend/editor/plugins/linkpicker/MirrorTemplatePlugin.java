/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.ajax.BrLink;
import org.hippoecm.frontend.ajax.NoDoubleClickAjaxLink;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorTemplatePlugin extends RenderPlugin<Node> {
    private static final Logger log = LoggerFactory.getLogger(MirrorTemplatePlugin.class);

    private final Fragment fragment;

    public MirrorTemplatePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final Mode mode = Mode.fromString(config.getString(ITemplateEngine.MODE), Mode.VIEW);
        if (mode == Mode.EDIT) {
            fragment = new Fragment("fragment", "edit", this);
            addOpenLinkPickerLink();
            addButtons();
        } else {
            fragment = new Fragment("fragment", "viewCompare", this);
            addOpenLink();
        }
        add(fragment);
    }

    public Fragment getFragment() {
        return fragment;
    }

    protected boolean hasLink() {
        return StringUtils.isNotEmpty(getPathModel().getObject());
    }

    protected void addButtons() {
        addSelectButton();
        addOpenButton();
        addClearButton();
    }

    private void addOpenLinkPickerLink() {
        final IModel<String> displayModel = getLocalizedNameModel();
        final IDialogFactory factory = getDialogFactory();
        final IDialogService service = getDialogService();
        final DialogLink openPickerLink = new DialogLink("openLinkPickerLink", displayModel, factory, service) {
            @Override
            public boolean isVisible() {
                return hasLink();
            }
        };
        openPickerLink.add(TitleAttribute.set(getPathModel()));
        fragment.add(openPickerLink);
    }

    private void addOpenLink() {
        final AjaxLink<Void> openLink = new NoDoubleClickAjaxLink<Void>("openLink") {
            @Override
            public boolean isVisible() {
                return hasLink();
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                open();
            }
        };
        openLink.add(new Label("value", getLocalizedNameModel()));
        openLink.add(TitleAttribute.set(getPathModel()));
        openLink.setOutputMarkupId(true);
        fragment.add(openLink);
    }

    private void addOpenButton() {
        final AjaxLink<Void> openButton = new BrLink<Void>("open") {
            @Override
            public boolean isVisible() {
                return hasLink();
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                open();
            }
        };
        openButton.setOutputMarkupId(true);
        fragment.add(openButton);
    }

    private void addSelectButton() {
        fragment.add(new BrLink<Void>("select") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final Dialog<String> linkPickerDialog = createLinkPickerDialog();
                getDialogService().show(linkPickerDialog);
            }
        });
    }

    private void addClearButton() {
        final AjaxLink<Void> clearButton = new BrLink<Void>("clear") {
            @Override
            public boolean isVisible() {
                return hasLink();
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                clearModel();
            }
        };
        fragment.add(clearButton);
    }

    private void clearModel() {
        final Node node = this.getModelObject();
        try {
            getDocBaseModel().setObject(node.getSession().getRootNode().getIdentifier());
        } catch (final RepositoryException e) {
            log.error("Unable to reset docbase to root node uuid", e);
        }
        redraw();
    }

    private IDialogFactory getDialogFactory() {
        return this::createLinkPickerDialog;
    }

    private Dialog<String> createLinkPickerDialog() {
        final JcrPropertyValueModel<String> docBaseModel = getDocBaseModel();
        final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(getPluginConfig(), docBaseModel);
        final IChainingModel<String> linkPickerModel = new IChainingModel<String>() {
            public String getObject() {
                return docBaseModel.getObject();
            }

            public void setObject(final String uuid) {
                getDocBaseModel().setObject(uuid);
                redraw();
            }

            public IModel<String> getChainedModel() {
                return docBaseModel;
            }

            public void setChainedModel(final IModel<?> model) {
                throw new UnsupportedOperationException("Value model cannot be changed");
            }

            public void detach() {
                docBaseModel.detach();
            }
        };

        final IPluginContext context = getPluginContext();
        return new LinkPickerDialog(context, dialogConfig, linkPickerModel);
    }

    private String getMirrorPath() {
        final Node node = MirrorTemplatePlugin.this.getModelObject();
        String path = "";
        String docBase = "";
        try {
            if (node != null && node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                path = node.getPath();
                docBase = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();

                return getPath(docBase);
            }
        } catch (final ValueFormatException e) {
            log.warn("Invalid value format for docbase {} at path {}", docBase, path);
        } catch (final PathNotFoundException e) {
            log.info("Path not found for docbase {} at path {}", docBase, path);
        } catch (final ItemNotFoundException e) {
            log.info("Item could not be dereferenced for docbase {} at {}", docBase, path);
        } catch (final RepositoryException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                log.warn("Invalid value for docbase {} at path {}", docBase, path);
            }
            else {
                log.error("Invalid docbase '{}' at path '{}'", docBase, path, e);
            }
        }
        return StringUtils.EMPTY;
    }

    private String getPath(final String docBaseUuid) throws RepositoryException {
        if (StringUtils.isNotEmpty(docBaseUuid) && !docBaseUuid.equals(JcrConstants.ROOT_NODE_ID)) {
            return getJcrSession().getNodeByIdentifier(docBaseUuid).getPath();
        }
        return StringUtils.EMPTY;
    }

    private Session getJcrSession() throws RepositoryException {
        final Node node = this.getModelObject();
        return node.getSession();
    }

    private String getDisplayName(final String identifier) {
        try {
            final Node node = getJcrSession().getNodeByIdentifier(identifier);
            final HippoNode hippoNode = (HippoNode) node;
            return hippoNode.getDisplayName();
        } catch (final RepositoryException e) {
            log.error("Cannot get display name " + e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private void open() {
        final IPluginConfig config = getPluginConfig();
        final IPluginContext context = getPluginContext();
        final String browserId = config.getString("browser.id", "service.browse");
        @SuppressWarnings("unchecked")
        final IBrowseService<IModel<Node>> browseService = context.getService(browserId, IBrowseService.class);
        final String location = config.getString("option.location", getPathModel().getObject());
        if (browseService != null) {
            browseService.browse(new JcrNodeModel(location));
        } else {
            log.warn("no browse service found with id '{}', cannot browse to '{}'", browserId, location);
        }
    }

    JcrPropertyValueModel<String> getDocBaseModel() {
        final JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        final JcrItemModel<Node> itemModel = nodeModel.getItemModel();
        final String docBasePath = itemModel.getPath() + "/" + HippoNodeType.HIPPO_DOCBASE;
        return new JcrPropertyValueModel<>(new JcrPropertyModel<String>(docBasePath));
    }

    IModel<String> getPathModel() {
        return new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return getMirrorPath();
            }
        };
    }

    IModel<String> getLocalizedNameModel() {
        return new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return getDisplayName(getDocBaseModel().getObject());
            }
        };
    }

}
