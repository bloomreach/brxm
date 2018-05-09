/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services.htmlprocessor.serialize;

import java.io.IOException;
import java.io.Writer;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CompactHtmlSerializer;
import org.htmlcleaner.TagNode;

class NormalizingCompactHtmlSerializer extends CompactHtmlSerializer {

    private final ThreadLocal<Boolean> escapeText;

    NormalizingCompactHtmlSerializer(final CleanerProperties props) {
        super(props);
        escapeText = ThreadLocal.withInitial(() -> true);
    }

    @Override
    protected void serializeOpenTag(final TagNode tagNode, final Writer writer, final boolean newLine) throws IOException {
        escapeText.set(false);
        try {
            super.serializeOpenTag(tagNode, writer, newLine);
        } finally {
            escapeText.set(true);
        }
    }

    @Override
    protected String escapeText(final String content) {
        if (escapeText.get()) {
            return CharacterReferenceNormalizer.normalize(content);
        }
        return content;
    }
}
