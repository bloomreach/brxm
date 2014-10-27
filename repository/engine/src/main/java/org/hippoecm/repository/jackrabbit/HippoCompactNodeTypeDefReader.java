/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.io.Reader;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.hippoecm.repository.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoCompactNodeTypeDefReader extends CompactNodeTypeDefReader<QNodeTypeDefinition,NamespaceMapping> {

    static final Logger log = LoggerFactory.getLogger(HippoCompactNodeTypeDefReader.class);

    public HippoCompactNodeTypeDefReader(Reader reader, String systemId, NamespaceRegistry registry) throws ParseException {
        super(reader, systemId, new HippoNamespaceMapping(registry), new QDefinitionBuilderFactory());
    }

    private static class HippoNamespaceMapping<N> extends NamespaceMapping {
        private static Set<String> autoCompatibleNamespaces = new TreeSet<>(Arrays.asList(new String[] {"hippo", "hipposys", "hipposysedit", "hippofacnav"}));
        private NamespaceRegistry registry;

        private HippoNamespaceMapping(NamespaceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void setMapping(String prefix, String uri) throws NamespaceException {
            try {
                if (autoCompatibleNamespaces.contains(prefix)) {
                    VersionNumber version = VersionNumber.versionFromURI(uri);
                    String[] versions = version.toString().split("\\.");
                    try {
                        String currentUri = registry.getURI(prefix);
                        VersionNumber currentVersion = VersionNumber.versionFromURI(currentUri);
                        String[] currentVersions = currentVersion.toString().split("\\.");
                        if (versions.length >= 2 && currentVersions.length >= 2 &&
                                versions[0].equals(currentVersions[0]) &&
                                versions[1].equals(currentVersions[1])) {
                            int compare = version.compareTo(currentVersion);
                            if (compare > 0) {
                                log.info("using outdated version in repository {} in stead of {}", new Object[] {currentUri, uri});
                                uri = currentUri;
                            } else if (compare < 0) {
                                log.info("using more up-to-date version in repository {} in stead of {}", new Object[] {currentUri, uri});
                                uri = currentUri;
                            }
                        }
                    } catch (NamespaceException ex) {
                        if (log.isDebugEnabled()) {
                            log.debug("namespace {} as yet undefined", new Object[] {uri});
                        }
                        // deliberate fall through
                    }
                }
            } catch (RepositoryException ex) {
                log.error("unexpected exception while determining namespace", ex);
                // deliberate fall through
            }
            if (log.isDebugEnabled()) {
                log.debug("set mapping of {} to {}", prefix, uri);
            }
            super.setMapping(prefix, uri);
        }
    }
}
