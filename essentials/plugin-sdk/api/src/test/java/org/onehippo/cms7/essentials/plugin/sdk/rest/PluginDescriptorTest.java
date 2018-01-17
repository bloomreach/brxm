/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.rest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.onehippo.cms7.essentials.plugin.sdk.rest.MavenRepository;
import org.onehippo.cms7.essentials.plugin.sdk.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.Module;

import static org.junit.Assert.assertEquals;

public class PluginDescriptorTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void testJaxb() throws Exception {
        PluginDescriptor value = new PluginDescriptor();
        value.setName("com.foo.name");
        final Calendar today = Calendar.getInstance();
        value.setDateInstalled(today);
        value.setRestClasses(Arrays.asList("com.foo.Foo", "com.foo.Bar"));

        final PluginDescriptor.Vendor vendor = new PluginDescriptor.Vendor();
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
        repository.setTargetPom(Module.PROJECT.getName());
        value.setRepositories(Collections.singletonList(repository));

        // test json:

        final String json = JSON.writeValueAsString(value);
        final PluginDescriptor fromJson = value = JSON.readValue(json, PluginDescriptor.class);
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
