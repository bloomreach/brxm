/*
 *  Copyright 2010 Hippo.
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

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.tree.icon.AbstractJcrTreeNodeIconProvider;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
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

    public ResourceReference getNodeIcon(TreeNode treeNode, ITreeState state) {
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
                            String locale = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
                            TreeNode parentTreeNode = treeNode.getParent();
                            if (parentTreeNode instanceof IJcrTreeNode) {
                                Node parentNode = ((IJcrTreeNode) parentTreeNode).getNodeModel().getObject();
                                if (parentNode != null && parentNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                                    String parentLocale = parentNode.getProperty(HippoTranslationNodeType.LOCALE)
                                            .getString();
                                    if (!locale.equals(parentLocale)) {
                                        return getIcon(treeNode, state, locale);
                                    } else {
                                        return null;
                                    }
                                }
                            }
                            return getIcon(treeNode, state, locale);
                        }
                    } catch (RepositoryException e) {
                        log.error("Could not determine translation icon for " + jcrTreeNode.getNodeModel());
                    }
                }
            }
        }
        return null;
    }

    private ResourceReference getIcon(TreeNode treeNode, ITreeState state, String locale) {
        HippoLocale hippoLocale = locales.getLocale(locale);
        if (state.isNodeExpanded(treeNode)) {
            return hippoLocale.getIcon(IconSize.TINY, LocaleState.FOLDER_OPEN);
        } else {
            return hippoLocale.getIcon(IconSize.TINY, LocaleState.FOLDER);
        }
    }

}
