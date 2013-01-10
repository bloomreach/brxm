/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channels;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.translation.HippoTranslationNodeType;

/**
 * Helper class to resolve the locale of a JCR node.
 */
class LocaleResolver implements IClusterable {

    private static final long serialVersionUID = 1L;
    private ILocaleProvider localeProvider;

    LocaleResolver(ILocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    /**
     * Retrieves the locale of a node.
     *
     * @param node a JCR node
     * @return the locale of the node, or null if no locale could be determined.
     * @throws RepositoryException when an unexpected error occurred while retrieving the locale of the node
     */
    Locale getLocale(Node node) throws RepositoryException {
        if (node.hasProperty(HippoTranslationNodeType.LOCALE)) {
            String localeName = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
            return localeProvider.getLocale(localeName).getLocale();
        }
        return null;
    }

}
