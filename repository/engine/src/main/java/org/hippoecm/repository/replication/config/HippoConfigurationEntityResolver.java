/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.replication.config;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity resolver for Hippo configuration files.
 * This simple resolver contains mappings for the following
 * public identifiers used for the Jackrabbit configuration files:
 * <ul>
 * <li><code>-//Hippo//DTD Replication 1.0//EN</code></li>
 * </ul>
 * <p>
 * Also the following system identifiers are mapped to local resources:
 * <ul>
 * <li><code>http://repository.hippocms.org/dtd/repository-1.0.dtd</code></li>
 * </ul>
 * <p>
 * The public identifiers are mapped to document type definition
 * files included in the hippo-ecm-repository-engine jar archive.
 */
class HippoConfigurationEntityResolver implements EntityResolver {

    /**
     * The singleton instance of this class.
     */
    public static final EntityResolver INSTANCE =
        new HippoConfigurationEntityResolver();

    /**
     * Public identifiers.
     */
    private final Map<String, String> publicIds = new HashMap<String, String>();

    /**
     * System identifiers.
     */
    private final Map<String, String> systemIds = new HashMap<String, String>();

    /**
     * Creates the singleton instance of this class.
     */
    private HippoConfigurationEntityResolver() {
        // Hippo Replication 1.0 DTD
        publicIds.put(
                "-//Hippo//DTD Replication 1.0//EN",
                "replication-1.0.dtd");
        systemIds.put(
                "http://repository.hippocms.org/dtd/replication-1.0.dtd",
                "replication-1.0.dtd");
    }

    /**
     * Resolves an entity to the corresponding input source.
     *
     * @param publicId public identifier
     * @param systemId system identifier
     * @return resolved entity source
     * @throws SAXException on SAX errors
     * @throws IOException on IO errors
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        String name;

        name = (String) publicIds.get(publicId);
        if (name != null) {
            InputStream stream = getClass().getResourceAsStream(name);
            if (stream != null) {
                return new InputSource(stream);
            }
        }

        name = (String) systemIds.get(systemId);
        if (name != null) {
            InputStream stream = getClass().getResourceAsStream(name);
            if (stream != null) {
                return new InputSource(stream);
            }
        }

        return null;
    }

}
