/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.translation.HippoTranslationNodeType.NT_TRANSLATED;

/**
 * Utility class.
 */
public class TranslationUtil {

    private static final Logger log = LoggerFactory.getLogger(TranslationUtil.class);

    private TranslationUtil() {
        // prevent instantiation
    }

    public static boolean isNtTranslated(Node node) throws RepositoryException {
        if (node.isNodeType(NT_TRANSLATED)) {
            return true;
        }
        return false;
    }

}
