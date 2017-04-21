/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.List;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;

public class ValidatorFactory {

    private static final Validator VOID_VALIDATOR = new Validator() {
        @Override
        public void validate(HstRequestContext requestContext) throws RuntimeException {
            // intentionally left blank
        }
    };

    public Validator getVoidValidator() {
        return VOID_VALIDATOR;
    }

    public Validator getChildExistsValidator(final String parentId, final String childId) {
        return new ChildExistsValidator(parentId, childId);
    }

    public Validator getNotNullValidator(final Object notNull, final ClientError clientError) {
        return getNotNullValidator(notNull, clientError, null);
    }

    public Validator getNotNullValidator(final Object notNull, final ClientError clientError, final String errorMessage) {
        return new NotNullValidator(notNull, clientError, errorMessage);
    }

    public Validator getNodePathPrefixValidator(final String nodePathPrefix, final String id, final String requiredNodeType) {
        return new NodePathPrefixValidator(nodePathPrefix, id, requiredNodeType);
    }

    public Validator getNodePathPrefixValidator(final String nodePathPrefix, final boolean liveAllowed, final String id, final String requiredNodeType) {
        return new NodePathPrefixValidator(nodePathPrefix, liveAllowed, id, requiredNodeType);
    }

    public Validator getSiteMenuItemRepresentationValidator(final Predicate<String> uriValidator, final SiteMenuItemRepresentation representation) {
        return new SiteMenuItemRepresentationValidator(uriValidator, representation);
    }

    /**
     * @deprecated since HST 3.2.0. Use {@link #getConfigurationExistsValidator} instead
     */
    @Deprecated
    public Validator getCurrentPreviewConfigurationValidator(final String id, final SiteMapHelper siteMapHelper) {
        return getConfigurationExistsValidator(id, siteMapHelper);
    }

    public Validator getConfigurationExistsValidator(final String id, final SiteMapHelper siteMapHelper) {
        return new ConfigurationExistsValidator(id, siteMapHelper);
    }

    public Validator getConfigurationExistsValidator(final String id, final String mountId, final SiteMapHelper siteMapHelper) {
        return new ConfigurationExistsValidator(id, mountId, siteMapHelper);
    }


    public Validator getPrototypePageValidator(final String prototypeUuid) {
        return new PrototypePageValidator(prototypeUuid);
    }

    public Validator getHasPreviewConfigurationValidator(final PageComposerContextService pageComposerContextService) {
        return new HasPreviewConfigurationValidator(pageComposerContextService);
    }
    public Validator getHasPreviewConfigurationValidator(final PageComposerContextService pageComposerContextService,
                                                         final String mountId) {
        return new HasPreviewConfigurationValidator(pageComposerContextService, mountId);
    }

    public Validator getHasWorkspaceConfigurationValidator(final String mountId) {
        return new HasWorkspaceConfigurationValidator(mountId);
    }

    public Validator getNameValidator(String name) {
        return new NameValidator(name);
    }

    public Validator getNamePathInfoValidator(String name) {
        return new NamePathInfoValidator(name);
    }

    public Validator getPathInfoValidator(final SiteMapItemRepresentation siteMapItem,
                                          final String parentId,
                                          final SiteMapHelper siteMapHelper) {
        return new SiteMapItemBasedPathInfoValidator(siteMapItem, parentId, siteMapHelper);
    }

    public Validator getItemNotSameOrDescendantOfValidator(final String validateUUID, final String targetUUID) {
        return new ItemNotSameOrDescendantOfValidator(validateUUID, targetUUID);
    }

    public Validator getHasNoChildMountNodeValidator(final List<Mount> mounts) {
        return new HasNoChildMountNodeValidator(mounts);
    }

    private static final class SiteMapItemBasedPathInfoValidator extends AbstractPathInfoValidator {

        private final SiteMapItemRepresentation siteMapItem;
        private final String parentId;
        private final SiteMapHelper siteMapHelper;

        private SiteMapItemBasedPathInfoValidator(final SiteMapItemRepresentation siteMapItem, final String parentId, final SiteMapHelper siteMapHelper) {
            this.siteMapItem = siteMapItem;
            this.parentId = parentId;
            this.siteMapHelper = siteMapHelper;
        }

        @Override
        protected String getPathInfo() throws ClientException {
            String pathInfo = "";
            if (siteMapItem != null) {
                if (parentId == null) {
                    pathInfo = "/" + siteMapItem.getName();
                } else {
                    // Calling getConfigObject can throw a ClientException
                    HstSiteMapItem parent = siteMapHelper.getConfigObject(parentId);
                    while (parent != null) {
                        pathInfo = parent.getValue() + "/" + pathInfo;
                        parent = parent.getParentItem();
                    }
                    if (!pathInfo.endsWith("/")) {
                        pathInfo += "/";
                    }
                    pathInfo = pathInfo + siteMapItem.getName();
                }
            }
            return pathInfo;
        }
    }

    private static final class NamePathInfoValidator extends AbstractPathInfoValidator {

        private final String name;
        private NamePathInfoValidator(final String name) {
            this.name = name;
        }

        @Override
        protected String getPathInfo() throws ClientException {
            return name;
        }
    }
}
