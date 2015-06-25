/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.journal;

import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNull;

public class ExternalRepositorySyncRevisionServiceNoClusterTest extends RepositoryTestCase {

    @Test
    public void testNoCluster() throws Exception {
        InternalHippoRepository internalHippoRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(session.getRepository());
        ExternalRepositorySyncRevision syncRevision = internalHippoRepository.getExternalRepositorySyncRevisionService().getSyncRevision("test");
        assertNull("Without cluster DatabaseJournal syncRevision should be null", syncRevision);
    }
}
