/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import javax.jcr.Node;
import javax.jcr.SimpleCredentials;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_KEY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY;

public class TaxonomyTranslationsUpdaterTest extends RepositoryTestCase {
    
    private String[] content = new String[] {
            "/test", "hippo:handle",
            "/test/test", NODETYPE_HIPPOTAXONOMY_TAXONOMY,
            "jcr:mixinTypes", MIX_VERSIONABLE,
            "hippostd:state", "unpublished",
            "hippostdpubwf:createdBy", "admin",
            "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
            "hippostdpubwf:lastModifiedBy", "admin",
            "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00",
            "/test/test/foo", NODETYPE_HIPPOTAXONOMY_CATEGORY,
            "jcr:mixinTypes", "hippotaxonomy:translated",
            HIPPOTAXONOMY_KEY, "foo",
            "/test/test/foo/hippotaxonomy:translation", "hippotaxonomy:translation",
            "hippo:language", "en",
            "hippo:message", "0",
            "/test/test/foo/bar", NODETYPE_HIPPOTAXONOMY_CATEGORY,
            "jcr:mixinTypes", "hippotaxonomy:translated",
            HIPPOTAXONOMY_KEY, "bar",
            "/test/test/foo/bar/hippotaxonomy:translation", "hippotaxonomy:translation",
            "hippo:language", "en",
            "hippo:message", "0",
    };
    
    @Test
    public void testUpdateTaxonomy() throws Exception {
        build(content, session);
        session.save();
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkpoint("/test/test");
        Node taxonomyNode = session.getNode("/test/test");
        for (int i = 1; i < 10; i++) {
            taxonomyNode.getProperty("foo/hippotaxonomy:translation/hippo:message").setValue(String.valueOf(i));
            taxonomyNode.getProperty("foo/bar/hippotaxonomy:translation/hippo:message").setValue(String.valueOf(i));
            session.save();
            versionManager.checkpoint("/test/test");
        }
        final TaxonomyTranslationsUpdater updater = new TaxonomyTranslationsUpdater();
        updater.initialize(session.impersonate(new SimpleCredentials("admin", new char[] {})));
        updater.doUpdate(taxonomyNode);
        taxonomyNode = session.getNode("/test/test");
        final VersionHistory versionHistory = versionManager.getVersionHistory(taxonomyNode.getPath());
        final VersionIterator versions = versionHistory.getAllVersions();
        int count = 0;
        while (versions.hasNext()) {
            final Version version = versions.nextVersion();
            if (!version.getName().equals("jcr:rootVersion")) {
                final Node frozenNode = version.getFrozenNode();
                final String fooName = frozenNode.getProperty("foo/bar/hippotaxonomy:categoryinfos/en/hippotaxonomy:name").getString();
                assertEquals(String.valueOf(count), fooName);
                final String barName = frozenNode.getProperty("foo/bar/hippotaxonomy:categoryinfos/en/hippotaxonomy:name").getString();
                assertEquals(String.valueOf(count), barName);
                count++;
            }
        }
    }

}
