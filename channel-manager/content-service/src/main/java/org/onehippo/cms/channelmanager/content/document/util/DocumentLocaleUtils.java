/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.document.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentLocaleUtils {

    private static final Logger log = LoggerFactory.getLogger(DocumentLocaleUtils.class);

    private DocumentLocaleUtils() {
    }

    /**
     * Returns the locale of a document.
     *
     * @param variant a variant node of the document (i.e. a child node of the document's handle node)
     * @return the document's locale or null if no locale is set
     */
    public static String getDocumentLocale(final Node variant) {
        try {
            if (variant.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                // hippotranslation:locale is a mandatory property of the mixin hippotranslation:translated 
                return variant.getProperty(HippoTranslationNodeType.LOCALE).getString();
            }
        } catch (IllegalArgumentException | RepositoryException e) {
            log.warn("Could not determine locale of document variant '{}', returning 'null'",
                    JcrUtils.getNodePathQuietly(variant), e);
        }
        return null;
    }
}
