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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that a {@link UiExtension} can be used in the Channel Editor. Validation errors will be logged as
 * warnings.
 */
public class ChannelEditorUiExtensionValidator implements UiExtensionValidator {

    private static final Logger log = LoggerFactory.getLogger(ChannelEditorUiExtensionValidator.class);

    @Override
    public boolean validate(final UiExtension extension) {
        return validateId(extension)
                && validateDisplayName(extension)
                && validateExtensionPoint(extension)
                && validateUrl(extension);
    }

    private boolean validateId(final UiExtension extension) {
        final String id = extension.getId();

        // only allow alphanumeric IDs
        // - usable as ui-router state names (no spaces, no dots)
        // - prevents XSS (no quotes, HTML, etc.)
        // - keeps it simple
        if (!StringUtils.isAlphanumeric(id)) {
            log.warn("Ignoring UI extension '{}': extension IDs must be alphanumeric.", id);
            return false;
        }

        return true;
    }

    private boolean validateDisplayName(final UiExtension extension) {
        final String displayName = extension.getDisplayName();

        if (StringUtils.isBlank(displayName)) {
            log.warn("Ignoring UI extension '{}': no display name provided.", extension.getId());
            return false;
        }

        return true;
    }

    private boolean validateExtensionPoint(final UiExtension extension) {
        final String extensionPoint = extension.getExtensionPoint();

        if (StringUtils.isBlank(extensionPoint)) {
            log.warn("Ignoring UI extension '{}': no extensionPoint provided.", extensionPoint);
            return false;
        }

        return true;
    }

    private boolean validateUrl(final UiExtension extension) {
        final String url = extension.getUrl();

        if (StringUtils.isBlank(url)) {
            log.warn("Ignoring UI extension '{}': no URL provided.", extension.getId());
            return false;
        }

        return true;
    }
}
