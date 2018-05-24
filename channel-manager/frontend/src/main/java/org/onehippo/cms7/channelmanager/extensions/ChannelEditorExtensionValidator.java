/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that a {@link CmsExtension} can be used in the Channel Editor.
 * Validation errors will be logged as warnings.
 */
public class ChannelEditorExtensionValidator implements CmsExtensionValidator {

    private static final Logger log = LoggerFactory.getLogger(ChannelEditorExtensionValidator.class);

    @Override
    public boolean validate(final CmsExtension extension) {
        return validateId(extension)
                && validateDisplayName(extension)
                && validateContext(extension)
                && validateUrlPath(extension);
    }

    private boolean validateId(final CmsExtension extension) {
        final String id = extension.getId();

        // only allow alphanumeric IDs
        // - usable as ui-router state names (no spaces, no dots)
        // - prevents XSS (no quotes, HTML, etc.)
        // - keeps it simple
        if (!StringUtils.isAlphanumeric(id)) {
            log.warn("Ignoring CMS extension '{}': extension IDs must be alphanumeric.", id);
            return false;
        }

        return true;
    }

    private boolean validateDisplayName(final CmsExtension extension) {
        final String displayName = extension.getDisplayName();

        if (StringUtils.isBlank(displayName)) {
            log.warn("Ignoring CMS extension '{}': no display name provided.", extension.getId());
            return false;
        }

        return true;
    }

    private boolean validateContext(final CmsExtension extension) {
        final CmsExtensionContext context = extension.getContext();

        if (context == null) {
            final List<String> contextNames = Arrays.stream(CmsExtensionContext.values())
                    .map(CmsExtensionContext::getLowerCase)
                    .collect(Collectors.toList());
            log.warn("Ignoring CMS extension '{}': context unknown. Valid contexts are: {}.",
                    extension.getId(), contextNames);
            return false;
        }

        return true;
    }

    private boolean validateUrlPath(final CmsExtension extension) {
        final String urlPath = extension.getUrlPath();

        if (StringUtils.isBlank(urlPath)) {
            log.warn("Ignoring CMS extension '{}': no URL path provided.", extension.getId());
            return false;
        }

        return true;
    }
}
