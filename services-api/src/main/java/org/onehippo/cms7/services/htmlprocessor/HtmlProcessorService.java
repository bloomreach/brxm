/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor;

/**
 * Looks up {@link HtmlProcessor} instances and checks whether HTML is visible.
 */
public interface HtmlProcessorService {

    /**
     * Returns instance of {@link HtmlProcessor} or null if the configuration cannot be found
     *
     * @param id The HTML processor id
     * @return Instance of HTML processor
     */
    HtmlProcessor getHtmlProcessor(final String id);

    /**
     * Checks whether the provided HTML is visible. For example, "<p></p>" is not visible,
     * while "<p>text</p>" is.
     *
     * @param html the HTML to check
     * @return true if the HTML is visible, false otherwise
     */
    boolean isVisible(final String html);
}
