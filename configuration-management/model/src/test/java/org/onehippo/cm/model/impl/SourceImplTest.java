/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.source.Source;

import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SourceImplTest {

    @Test
    public void testEquals() {

        final Set<SourceImpl> sortedSources = new TreeSet<>(comparing(Source::getPath).thenComparing(x -> x.getClass().getSimpleName()));

        ProjectImpl project = new ProjectImpl("project", new GroupImpl("group"));

        ModuleImpl module = new ModuleImpl("module", project);

        ConfigSourceImpl configSource = new ConfigSourceImpl("content.yaml", module);
        boolean configAdded = sortedSources.add(configSource);
        assertTrue(configAdded);

        ContentSourceImpl contentSource = new ContentSourceImpl("content.yaml", module);
        boolean contentAdded = sortedSources.add(contentSource);
        assertTrue(contentAdded);

        ContentSourceImpl contentSource2 = new ContentSourceImpl("content.yaml", module);

        assertNotEquals(configSource, contentSource);
        assertNotEquals(configSource.hashCode(), contentSource.hashCode());
        assertEquals(contentSource, contentSource2);
        assertEquals(contentSource.hashCode(), contentSource2.hashCode());

    }

}