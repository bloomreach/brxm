/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_TEMPLATES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_TEMPLATES;

public class TemplateHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(TemplateHelper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * @param pageCopyContext in case the {@code pageCopyContext} is a cross-channel copy paste event, possible missing
     *                        templates in the target channel need to be added
     */
    public void copyTemplates(final PageCopyContext pageCopyContext) throws RepositoryException {
        if (pageCopyContext.getTargetMount().getIdentifier().equals(pageCopyContext.getEditingMount().getIdentifier())) {
            log.debug("Copy page is within same channel. No need to copy templates.", pageCopyContext);
            return;
        }

        // possible templates need to be copied over. First check all templates from original page
        final HstComponentConfiguration sourcePage = pageCopyContext.getSourcePage();
        final HstComponentsConfigurationService source = (HstComponentsConfigurationService)pageCopyContext.getEditingMount().getHstSite().getComponentsConfiguration();
        final HstComponentsConfigurationService target = (HstComponentsConfigurationService)pageCopyContext.getTargetMount().getHstSite().getComponentsConfiguration();

        copyMissingTemplates(sourcePage, pageCopyContext, source, target);
    }

    private void copyMissingTemplates(final HstComponentConfiguration sourceComponent,
                                      final PageCopyContext pageCopyContext,
                                      final HstComponentsConfigurationService source,
                                      final HstComponentsConfigurationService target) throws RepositoryException {
        final String templateName = ((HstComponentConfigurationService)sourceComponent).getHstTemplate();

        if (templateName != null && !target.getTemplates().containsKey(templateName)) {
            // missing template in target. Fetch it from the source
            final HstComponentsConfigurationService.Template template = source.getTemplates().get(templateName);
            if (template == null) {
                log.warn("Cannot copy template since the source channel also misses the template. Ignore template '{}' copy for " +
                        "'{}'", templateName, pageCopyContext);
            } else {
                copyTemplate(template, pageCopyContext.getTargetMount(), pageCopyContext.getRequestContext().getSession());
            }
        }

        for (HstComponentConfiguration child : sourceComponent.getChildren().values()) {
            copyMissingTemplates(child, pageCopyContext, source, target);
        }
    }

    private void copyTemplate(final HstComponentsConfigurationService.Template template,
                              final Mount targetMount,
                              final Session session) throws RepositoryException {
        final String templatesPath = targetMount.getHstSite().getConfigurationPath() + "/" + NODENAME_HST_TEMPLATES;
        if (!session.nodeExists(templatesPath)) {
            session.getNode(targetMount.getHstSite().getConfigurationPath()).addNode(NODENAME_HST_TEMPLATES, NODETYPE_HST_TEMPLATES);
        }
        JcrUtils.copy(session, template.getPath(), templatesPath + "/" + template.getName());
        lockHelper.acquireSimpleLock(session.getNode(templatesPath), 0L);
    }
}
