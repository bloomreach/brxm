/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.XSSUrlFilter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathInfoValidator implements Validator {

    private static final Logger log = LoggerFactory.getLogger(PathInfoValidator.class);

    String pathInfo;
    SiteMapItemRepresentation siteMapItem;
    String parentId;
    SiteMapHelper siteMapHelper;

    public PathInfoValidator(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public PathInfoValidator(final SiteMapItemRepresentation siteMapItem,
                             final String parentId,
                             final SiteMapHelper siteMapHelper) {

        this.siteMapItem = siteMapItem;
        this.parentId = parentId;
        this.siteMapHelper = siteMapHelper;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {
        if (pathInfo == null && siteMapItem != null) {
            if (parentId == null) {
                pathInfo = "/" + siteMapItem.getName();
            } else {
                HstSiteMapItem parent = siteMapHelper.getConfigObject(parentId);
                pathInfo = "";
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
        if (containsEncodedDirectoryTraversalChars(pathInfo, requestContext.getServletRequest().getCharacterEncoding())) {
            String msg = String.format("Invalid pathInfo '%s' because contains invalid encoded chars like " +
                    " %%2f, %%5c or %%2e which are typically used for directory traversal (attacks)", pathInfo);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_URL);
        }

        if (StringUtils.isEmpty(pathInfo)) {
            return;
        }
        if (XSSUrlFilter.containsMarkups(pathInfo)) {
            String msg = String.format("Invalid pathInfo '%s' because it contains XSS markup", pathInfo);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_URL);
        }

        HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        if (hstManager.isExcludedByHstFilterInitParameter(pathInfo)) {
            String msg = String.format("PathInfo '%s' cannot be used because it is skipped through web.xml prefix or postfix " +
                    "exclusions.", pathInfo);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_URL);
        }

        if (requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().isExcluded(pathInfo)) {
            String msg = String.format("PathInfo '%s' cannot be used because it is skipped through prefix or postfix " +
                    "exclusions on /hst:hst/hst:hosts configuration.", pathInfo);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_URL);
        }
    }

    public static boolean containsEncodedDirectoryTraversalChars(String pathInfo, String characterEncoding) {
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        pathInfo = pathInfo.toLowerCase();
        if (pathInfo.contains("%2f") || pathInfo.contains("%5c") || pathInfo.contains("%2e")) {
            log.info("PathInfo '{}' contains invalid encoded '/' or '\\' or a '.'", pathInfo, characterEncoding);
            return true;
        }
        try {
            // for if the path info contains incomplete trailing escape (%) pattern, decoding will fail with an
            // IllegalArgumentException and the path info is incorrect
            URLDecoder.decode(pathInfo, characterEncoding);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            log.info("PathInfo '{}' cannot be decoded with '{}'.", pathInfo, characterEncoding);
            return true;
        }
        return false;
    }
}
