/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.diff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;

import org.apache.wicket.Session;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.htmldiff.DiffHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * The default HTML diff service using
 * {@link org.hippoecm.htmldiff.DiffHelper#diffHtml(String, String, javax.xml.transform.Result, java.util.Locale)}
 *
 * @author cngo
 * @version $Id$
 * @since 2015-02-02
 */
public class DefaultHtmlDiffService implements DiffService {
    static final Logger log = LoggerFactory.getLogger(DefaultHtmlDiffService.class);

    public static final String DEFAULT_MAX_DIFF_SIZE = "100KB";
    public static final String MAX_DIFF_SIZE = "max.diff.size";

    private final IValueMap params;

    public DefaultHtmlDiffService() {
        this(ValueMap.EMPTY_MAP);
    }

    public DefaultHtmlDiffService(IValueMap params){
        this.params = params;

    }

    @Override
    public String diff(final String originalValue, final String currentValue) {
        Bytes maxDiffSize = Bytes.valueOf(params.getString(MAX_DIFF_SIZE, DEFAULT_MAX_DIFF_SIZE));
        final int maxLenSize = Math.max(originalValue.length(), currentValue.length());

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (maxLenSize <= maxDiffSize.bytes()){
                DiffHelper.diffHtml(originalValue, currentValue, new StreamResult(baos),
                        Session.get().getLocale());
                return baos.toString("UTF-8");
            } else {
                log.warn("Unable to diff a large content of size {} KB, which is exceeds the limitation {} KB ",
                        Bytes.bytes(maxLenSize).kilobytes(), maxDiffSize.kilobytes());
            }
        } catch (TransformerConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.info(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
