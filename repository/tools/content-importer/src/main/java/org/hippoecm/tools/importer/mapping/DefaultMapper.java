/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.importer.mapping;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.ImportException;
import org.hippoecm.tools.importer.api.Mapper;
import org.hippoecm.tools.importer.api.Mapping;

/**
 * Default mapping for content importers.
 */
public class DefaultMapper implements Mapper {

    final static String SVN_ID = "$Id$";

    public void setup(Configuration config) throws ImportException {
    }

    public Mapping map(Content content) throws ImportException {
        String name = content.getName();
        if ("system".equals(name)) {
            return null;
        }

        String location = content.getLocation();
        String[] elements = StringUtils.split(location, '/');
        if (elements.length > 0) {
            String fileName = elements[elements.length - 1];
            if (fileName.endsWith(".xml")) {
                elements[elements.length - 1] = fileName.substring(0, fileName.length() - 4);
            }
        }
        final String path = StringUtils.join(elements, '/');
        return new Mapping() {

            public String getNodeType() {
                return "defaultcontent:article";
            }

            public String getPath() {
                return path;
            }

        };
    }

}
