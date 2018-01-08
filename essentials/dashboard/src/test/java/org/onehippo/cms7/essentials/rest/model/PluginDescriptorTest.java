/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import org.junit.Test;
import org.onehippo.cms7.essentials.WebUtils;
import org.onehippo.cms7.essentials.dashboard.model.MavenRepository;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.model.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class PluginDescriptorTest {

    private static Logger log = LoggerFactory.getLogger(PluginDescriptorTest.class);

    @Test
    public void testJaxb() throws Exception {
        PluginDescriptor value = new PluginDescriptor();
        value.setName("com.foo.name");
        final Calendar today = Calendar.getInstance();
        value.setDateInstalled(today);
        value.setRestClasses(Arrays.asList("com.foo.Foo", "com.foo.Bar"));

        final Vendor vendor = new Vendor();
        vendor.setName("hippo");
        value.setVendor(vendor);

        final MavenRepository.WithModule repository = new MavenRepository.WithModule();
        repository.setId("myId");
        repository.setUrl("http://onehippo.com/maven2");
        final MavenRepository.Policy snapshots = new MavenRepository.Policy();
        snapshots.setChecksumPolicy("test-snapshots-checksumpolicy");
        snapshots.setUpdatePolicy("test-snapshots-updatepolicy");
        repository.setSnapshotPolicy(snapshots);
        final MavenRepository.Policy repositoryPolicy = new MavenRepository.Policy();
        repositoryPolicy.setChecksumPolicy("testchecksumpolicy");
        repositoryPolicy.setUpdatePolicy("testupdatepolicy");
        repository.setReleasePolicy(repositoryPolicy);
        repository.setTargetPom(TargetPom.PROJECT.getName());
        value.setRepositories(Collections.singletonList(repository));

        // test json:

        final String json = WebUtils.toJson(value);
        final PluginDescriptor fromJson = value = WebUtils.fromJson(json, PluginDescriptor.class);
        assertEquals(2, fromJson.getRestClasses().size());
        assertEquals(today.getTime(), fromJson.getDateInstalled().getTime());
        assertEquals(vendor.getName(), fromJson.getVendor().getName());
        assertEquals("Expected 1 repository", 1, value.getRepositories().size());
        assertEquals("Expected repository checksum policy", "testchecksumpolicy", value.getRepositories().get(0).getReleasePolicy().getChecksumPolicy());
        assertEquals("Expected repository update policy", "testupdatepolicy", value.getRepositories().get(0).getReleasePolicy().getUpdatePolicy());
        assertEquals("Expected repository enabled not to be set", null, value.getRepositories().get(0).getReleasePolicy().getEnabled());
        assertEquals("Expected repository snapshots checksum policy", "test-snapshots-checksumpolicy", value.getRepositories().get(0).getSnapshotPolicy().getChecksumPolicy());
        assertEquals("Expected repository snapshots update policy", "test-snapshots-updatepolicy", value.getRepositories().get(0).getSnapshotPolicy().getUpdatePolicy());
        assertEquals("Expected repository snapshots enabled not to be set", null, value.getRepositories().get(0).getSnapshotPolicy().getEnabled());
    }
}
