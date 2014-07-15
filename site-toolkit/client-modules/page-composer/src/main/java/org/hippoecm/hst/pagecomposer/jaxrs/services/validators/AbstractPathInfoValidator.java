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

import com.google.common.base.Optional;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.XSSUrlFilter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPathInfoValidator implements Validator {

    private static final Logger log = LoggerFactory.getLogger(AbstractPathInfoValidator.class);

    /**
     * Depending on the implementation in the concrete class it will either return the path info or throw a validation
     * exception. If getting the path info violates a precondition then this method should throw such an exception.
     *
     * @return path info
     * @throws ClientException if a precondition/constraint is violated while determining the path info
     */
    protected abstract String getPathInfo() throws ClientException;

    @Override
    public final void validate(final HstRequestContext requestContext) throws RuntimeException {
        final String info = getPathInfo();
        if (StringUtils.isEmpty(info)) {
            return;
        }

        final String encoding = Optional.fromNullable(requestContext.getServletRequest().getCharacterEncoding()).or("UTF-8");
        if (containsEncodedDirectoryTraversalChars(info, encoding)) {
            String msg = String.format("Invalid pathInfo '%s' because contains invalid encoded chars like " +
                    " %%2f, %%5c or %%2e which are typically used for directory traversal (attacks)", info);
            throw new ClientException(msg, ClientError.INVALID_PATH_INFO);
        }

        if (XSSUrlFilter.containsMarkups(info)) {
            String msg = String.format("Invalid pathInfo '%s' because it contains XSS markup", info);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_PATH_INFO);
        }

        HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        if (hstManager.isExcludedByHstFilterInitParameter(info)) {
            String msg = String.format("PathInfo '%s' cannot be used because it is skipped through web.xml prefix or postfix " +
                    "exclusions.", info);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_PATH_INFO);
        }

        if (requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().isExcluded(info)) {
            String msg = String.format("PathInfo '%s' cannot be used because it is skipped through prefix or postfix " +
                    "exclusions on /hst:hst/hst:hosts configuration.", info);
            log.info(msg);
            throw new ClientException(msg, ClientError.INVALID_PATH_INFO);
        }
    }

    public static boolean containsEncodedDirectoryTraversalChars(String pathInfo, String characterEncoding) {
        pathInfo = pathInfo.toLowerCase();
        if (pathInfo.contains("%2f") || pathInfo.contains("%5c") || pathInfo.contains("%2e")) {
            log.info("PathInfo '{}' contains invalid encoded '/' or '\\' or a '.'", pathInfo);
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
