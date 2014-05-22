/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.TemplateLoader;

/**
 * Delegating template loading to the given delegatee.
 * <P>
 * First of all, this template loader returns null if the template path starts with any of the given <code>prefixExclusions</code>.
 * <code>prefixExclusions</code> option always precedes <code>prefixInclusions</code> option.
 * </P>
 * <P>
 * Second, this template loader does delegation if <code>prefixInclusions</code> is empty or 
 * if the template path starts with any of the given <code>prefixInclusions</code>
 * unless the template path starts with any of <code>prefixExclusions</code>.
 * </P>
 * <P>
 * Third, if the template path doesn't start with any of a non-empty <code>prefixInclusions</code> regardless of <code>prefixExclusions</code>,
 * then this simply returns null.
 * </P>
 */
public class DelegatingTemplateLoader implements TemplateLoader {

    private static Logger log = LoggerFactory.getLogger(DelegatingTemplateLoader.class);

    private final TemplateLoader delegatee;
    private final String [] prefixInclusions;
    private final String [] prefixExclusions;

    public DelegatingTemplateLoader(final TemplateLoader delegatee, final String [] prefixInclusions, final String [] prefixExclusions) {
        this.delegatee = delegatee;

        if (prefixInclusions == null || prefixInclusions.length == 0) {
            this.prefixInclusions = ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            this.prefixInclusions = new String[prefixInclusions.length];
            System.arraycopy(prefixInclusions, 0, this.prefixInclusions, 0, prefixInclusions.length);
        }

        if (prefixExclusions == null || prefixExclusions.length == 0) {
            this.prefixExclusions = ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            this.prefixExclusions = new String[prefixExclusions.length];
            System.arraycopy(prefixExclusions, 0, this.prefixExclusions, 0, prefixExclusions.length);
        }
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        if (name == null) {
            return null;
        }

        if (prefixExclusions.length != 0) {
            for (String prefixExclusion : prefixExclusions) {
                if (name.startsWith(prefixExclusion)) {
                    return null;
                }
            }
        }

        Object templateSource = null;

        if (prefixInclusions.length == 0) {
            templateSource = delegatee.findTemplateSource(name);
        } else {
            for (String prefixInclusion : prefixInclusions) {
                if (name.startsWith(prefixInclusion)) {
                    templateSource = delegatee.findTemplateSource(name);
                    break;
                }
            }
        }

        if (templateSource == null) {
            log.warn("Template not found: '{}'", name);
        }

        return templateSource;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return delegatee.getLastModified(templateSource);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return delegatee.getReader(templateSource, encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        delegatee.closeTemplateSource(templateSource);
    }

}
