/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_TEMPLATE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_TEMPLATES;

public class TemplateHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(TemplateHelper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getConfigObject(final String itemId, final Mount mount) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    protected String getNodeType() {
        return NODETYPE_HST_TEMPLATE;
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
        final Session session = pageCopyContext.getRequestContext().getSession();

        if (templateName != null) {
            if (target.getTemplates().containsKey(templateName)) {
                // If 'hst:templates' section turns out to be locked by *another* user, then the template might be
                // present in preview but not yet in live. If the current user then publishes his changes, the template won't
                // get published because the current user does not own the lock. We need to double check that if
                // 'hst:templates' section is locked by someone else, that the template we need is already live.
                final String templatesPath = getWorkspaceTemplatesPath(pageCopyContext.getTargetMount());
                if (session.nodeExists(templatesPath)) {
                    // check whether locked by someone else
                    final boolean locked = lockHelper.getUnLockableNode(session.getNode(templatesPath), false, false) != null;
                    if (locked) {
                        // 'hst:templates' is locked by another user. Now, if the 'templateName' is not available in live
                        // configuration, throw an exception because the cross channel page copy cannot be successfully
                        // completed.
                        final String liveTemplatesPath = templatesPath.replace("-preview/", "/");
                        if (session.nodeExists(liveTemplatesPath + "/" + templateName)) {
                            log.debug("Template '{}' is already available in live config so no problem.", liveTemplatesPath);
                        } else {
                            log.info("Template '{}' does not exist and '{}' is locked by someone else. Cannot copy template",
                                    liveTemplatesPath, templatesPath);
                            // force an exception by trying to acquire the lock
                            lockHelper.acquireSimpleLock(session.getNode(templatesPath), 0L);
                        }
                    }
                }
            } else {
                // missing template in target. Fetch it from the source
                final HstComponentsConfigurationService.Template template = source.getTemplates().get(templateName);
                if (template == null) {
                    log.warn("Cannot copy template since the source channel also misses the template. Ignore template '{}' copy for " +
                            "'{}'", templateName, pageCopyContext);
                } else {
                    copyTemplate(template, pageCopyContext.getTargetMount(), session);
                }
            }
        }

        for (HstComponentConfiguration child : sourceComponent.getChildren().values()) {
            copyMissingTemplates(child, pageCopyContext, source, target);
        }
    }

    private void copyTemplate(final HstComponentsConfigurationService.Template template,
                              final Mount targetMount,
                              final Session session) throws RepositoryException {
        final String templatesPath = getWorkspaceTemplatesPath(targetMount);
        if (!session.nodeExists(templatesPath)) {
            session.getNode(targetMount.getHstSite().getConfigurationPath() +"/" + NODENAME_HST_WORKSPACE).addNode(NODENAME_HST_TEMPLATES, NODETYPE_HST_TEMPLATES);
        }
        // TODO make the template more fine-grained locked: Instead of locking hst:templates just lock the specific template
        // TODO unless 'hst:templates' node is just added, see HSTTWO-3959
        lockHelper.acquireSimpleLock(session.getNode(templatesPath), 0L);
        JcrUtils.copy(session, template.getPath(), templatesPath + "/" + template.getName());
    }

    private String getWorkspaceTemplatesPath(final Mount targetMount) {
        return targetMount.getHstSite().getConfigurationPath() +"/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_TEMPLATES;
    }
}
