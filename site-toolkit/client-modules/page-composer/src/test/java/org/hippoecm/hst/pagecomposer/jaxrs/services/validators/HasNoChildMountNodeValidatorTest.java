/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class HasNoChildMountNodeValidatorTest {

    private HstRequestContext context;
    private Mount mountFoo;

    private HasNoChildMountNodeValidator validator;

    @Before
    public void setUp() {
        context = createMock(HstRequestContext.class);
        final VirtualHost mockVirtualHost = createMock(VirtualHost.class);
        final VirtualHosts mockVirtualHosts = createMock(VirtualHosts.class);
        mountFoo = createMock(Mount.class);

        expect(context.getVirtualHost()).andReturn(mockVirtualHost);
        expect(mockVirtualHost.getVirtualHosts()).andReturn(mockVirtualHosts);
        expect(mockVirtualHosts.getMountByIdentifier("mount-foo")).andReturn(mountFoo);

        replay(context, mockVirtualHost, mockVirtualHosts);
        validator = new HasNoChildMountNodeValidator("mount-foo");
    }

    @Test
    public void no_exception_when_validating_mount_without_children_mounts() throws Exception {
        expect(mountFoo.getChildMounts()).andReturn(Collections.emptyList());
        replay(mountFoo);

        validator.validate(context);
    }

    @Test(expected = ClientException.class)
    public void has_exception_when_validating_mount_with_children_mounts() throws Exception {
        final List<Mount> childrenMounts = new ArrayList<>();
        final Mount childMount = createMock(Mount.class);
        childrenMounts.add(childMount);

        expect(mountFoo.getChildMounts()).andReturn(childrenMounts).anyTimes();

        replay(mountFoo);

        try {
            validator.validate(context);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.CHILD_MOUNT_EXISTS));
            throw e;
        }
    }

}