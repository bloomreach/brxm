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

package org.onehippo.cms7.essentials.sdk.api.rest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;

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

        // test json:

        final String json = JSON.writeValueAsString(value);
        final PluginDescriptor fromJson = value = JSON.readValue(json, PluginDescriptor.class);
        assertEquals(2, fromJson.getRestClasses().size());
        assertEquals(today.getTime(), fromJson.getDateInstalled().getTime());
        assertEquals(vendor.getName(), fromJson.getVendor().getName());
    }
}
