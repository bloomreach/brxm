/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.plugins.standards.tree.icon.AbstractJcrTreeNodeIconProvider;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeNodeIconPlugin extends AbstractJcrTreeNodeIconProvider implements IPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TreeNodeIconPlugin.class);

    private ILocaleProvider locales;

    public TreeNodeIconPlugin(IPluginContext context, IPluginConfig config) {
        context.registerService(this, config.getString("tree.icon.id", ITreeNodeIconProvider.class.getName()));
        context.registerTracker(new ServiceTracker<ILocaleProvider>(ILocaleProvider.class) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onServiceAdded(ILocaleProvider service, String name) {
                if (locales == null) {
                    locales = service;
                }
                super.onServiceAdded(service, name);
            }

            @Override
            protected void onRemoveService(ILocaleProvider service, String name) {
                if (service == locales) {
                    locales = null;
                }
                super.onRemoveService(service, name);
            }
        }, config.getString("locale.id", ILocaleProvider.class.getName()));
    }

    public void start() {
    }

    public void stop() {
    }

    @Override
    public Component getNodeIcon(final String id, final TreeNode treeNode, final ITreeState state) {
        if (locales == null) {
            return null;
        }
        if (treeNode instanceof IJcrTreeNode) {
            IJcrTreeNode jcrTreeNode = (IJcrTreeNode) treeNode;
            if (!isVirtual(jcrTreeNode)) {
                Node node = jcrTreeNode.getNodeModel().getObject();
                if (node != null) {
                    try {
                        if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                            return getTranslatedNodeIcon(id, treeNode, state, node);
                        }
                    } catch (RepositoryException e) {
                        log.warn("Could not determine translation icon for node '{}'",
                                JcrUtils.getNodePathQuietly(node), e);
                    }
                }
            }
        }
        return null;
    }

    private Component getTranslatedNodeIcon(final String id, final TreeNode treeNode, final ITreeState state, final Node node) throws RepositoryException {
        final String locale = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
        final TreeNode parentTreeNode = treeNode.getParent();
        if (parentTreeNode instanceof IJcrTreeNode) {
            final Node parentNode = ((IJcrTreeNode) parentTreeNode).getNodeModel().getObject();
            if (parentNode != null && parentNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                final String parentLocale = parentNode.getProperty(HippoTranslationNodeType.LOCALE).getString();
                if (!locale.equals(parentLocale)) {
                    return getTranslatedNodeIcon(id, treeNode, state, locale);
                } else {
                    return null;
                }
            }
        }
        return getTranslatedNodeIcon(id, treeNode, state, locale);
    }

    private HippoIconStack getTranslatedNodeIcon(final String id, final TreeNode treeNode, final ITreeState state, final String locale) {
        final HippoIconStack nodeIcon = new HippoIconStack(id, IconSize.M);
        final HippoLocale hippoLocale = locales.getLocale(locale);

        if (state.isNodeExpanded(treeNode)) {
            nodeIcon.addFromSprite(Icon.FOLDER_OPEN);
            nodeIcon.addFromResource(hippoLocale.getIcon(IconSize.M, LocaleState.FOLDER_OPEN));
        } else {
            nodeIcon.addFromSprite(Icon.FOLDER);
            nodeIcon.addFromResource(hippoLocale.getIcon(IconSize.M, LocaleState.FOLDER));
        }
        return nodeIcon;
    }

}
