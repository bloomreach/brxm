/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.jcr;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator for link tags ('a') that removes the 'href' attribute from internal links.
 */
class InternalLinkRemoveHrefDecorator extends InternalLinkDecorator {

    private static final Logger log = LoggerFactory.getLogger(InternalLinkRemoveHrefDecorator.class);

    InternalLinkRemoveHrefDecorator() {
        super("href");
    }

    @Override
    public String internalLink(final String href) {
        return StringUtils.EMPTY;
    }

}
