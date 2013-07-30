/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.util.List;

import org.onehippo.cms7.utilities.xml.SystemViewFilter;
import org.onehippo.repository.util.JcrConstants;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Filters out all namespace declarations except {http://www.jcp.org/jcr/sv/1.0};
 * excludes all declared subcontexts from export, filters out uuid properties on declared paths,
 * and filters global exclusions
 */
final class FilterContentHandler extends SystemViewFilter {


    private final List<String> subContextPaths;
    private final List<String> filterUuidPaths;
    private final ExclusionContext exclusionContext;
    private String svprefix;

    FilterContentHandler(ContentHandler handler, String rootPath, List<String> subContextPaths, List<String> filterUuidPaths, ExclusionContext exclusionContext) {
        super(handler, rootPath);
        this.subContextPaths = subContextPaths;
        this.filterUuidPaths = filterUuidPaths;
        this.exclusionContext = exclusionContext;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // only forward prefix mappings in the jcr/sv namespace
        if (uri.equals(SV_URI)) {
            svprefix = prefix;
            handler.startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (prefix.equals(svprefix)) {
            handler.endPrefixMapping(prefix);
        }
    }

    @Override
    protected boolean shouldFilterProperty(final String path, final String name) {
        return name.equals(JcrConstants.JCR_UUID) && isFilteredUuidPath(path);
    }

    @Override
    protected boolean shouldFilterNode(final String path, final String name) {
        for (String subContextPath : subContextPaths) {
            if (path.equals(subContextPath)) {
                return true;
            }
        }
        return exclusionContext.isExcluded(path);
    }

    private boolean isFilteredUuidPath(String path) {
        for (String filterPath : filterUuidPaths) {
            if (path.startsWith(filterPath)) {
                return true;
            }
        }
        return false;
    }

}