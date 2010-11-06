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
package org.hippoecm.frontend.translation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends org.hippoecm.frontend.plugins.cms.edit.EditPerspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(EditPerspective.class);

    private static final long serialVersionUID = 1L;

    public EditPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public ResourceReference getIcon(IconSize iconSize) {
        JcrNodeModel nodeModel = (JcrNodeModel) EditPerspective.this.getDefaultModel();
        if (nodeModel != null) {
            ILocaleProvider localeProvider = getLocaleProvider();
            if (localeProvider != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                            String localeName = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
                            for (HippoLocale locale : localeProvider.getLocales()) {
                                if (localeName.equals(locale.getName())) {
                                    return locale.getIcon(iconSize, LocaleState.EXISTS);
                                }
                            }
                            log.warn("Locale '" + localeName + "' was not found in provider");
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Node " + node.getPath() + " is not translated");
                            }
                        }
                    } catch (RepositoryException e) {
                        log.error("Repository error while retrieving locale for edited document", e);
                    }
                }
            }
        }
        return super.getIcon(iconSize);
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(getPluginConfig().getString("locale.id", ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }
}
