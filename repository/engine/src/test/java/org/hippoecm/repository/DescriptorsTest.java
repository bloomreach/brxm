/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Repository;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DescriptorsTest extends RepositoryTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testDescriptorKeys()  {
        Repository repository = session.getRepository();
        String[] expectedKeys = { Repository.OPTION_OBSERVATION_SUPPORTED, Repository.OPTION_VERSIONING_SUPPORTED, Repository.REP_VENDOR_DESC, Repository.REP_VENDOR_URL_DESC, Repository.REP_NAME_DESC, Repository.REP_VERSION_DESC, Repository.LEVEL_1_SUPPORTED, Repository.LEVEL_2_SUPPORTED };
        String[] keys = repository.getDescriptorKeys();
        Set<String> repositoryKeys = new TreeSet<String>();
        for(String key : keys) {
            repositoryKeys.add(key);
        }
        for(String expectedKey : expectedKeys) {
          assertTrue(repositoryKeys.contains(expectedKey));
        }
    }

    @Test
    public void testDescriptors()  {
        Repository repository = session.getRepository();
        String level1Supported = repository.getDescriptor(Repository.LEVEL_1_SUPPORTED);
        String level2Supported = repository.getDescriptor(Repository.LEVEL_2_SUPPORTED);
        String observationSupported = repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED);
        String versioningSupported = repository.getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED);
        String vendor = repository.getDescriptor(Repository.REP_VENDOR_DESC);
        String vendorURL = repository.getDescriptor(Repository.REP_VENDOR_URL_DESC);
        String repositoryName = repository.getDescriptor(Repository.REP_NAME_DESC);
        String repositoryVersion = repository.getDescriptor(Repository.REP_VERSION_DESC);
        assertEquals("true", level1Supported);
        assertEquals("true", level2Supported);
        assertEquals("true", observationSupported);
        assertEquals("true", versioningSupported);
        assertTrue(vendor.contains("Hippo"));
        assertTrue(vendorURL.contains("hippo"));
        assertTrue(repositoryName.contains("Hippo"));
    }
}
