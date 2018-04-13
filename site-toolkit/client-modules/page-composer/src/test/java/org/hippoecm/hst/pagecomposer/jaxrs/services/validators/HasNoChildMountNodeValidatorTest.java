/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HasNoChildMountNodeValidatorTest {

    private Mount mountFoo;
    private Mount mountBah;

    private HasNoChildMountNodeValidator validator;

    @Before
    public void setUp() {
        mountFoo = createMock(Mount.class);
        mountBah = createMock(Mount.class);
        validator = new HasNoChildMountNodeValidator(Arrays.asList(mountFoo, mountBah));
    }

    @Test
    public void no_exception_when_validating_mount_without_children_mounts() throws Exception {
        expect(mountFoo.getChildMounts()).andReturn(Collections.emptyList());
        expect(mountBah.getChildMounts()).andReturn(Collections.emptyList());
        replay(mountFoo, mountBah);

        validator.validate(null);
    }

    @Test(expected = ClientException.class)
    public void has_exception_when_validating_mount_with_explicit_child_mounts() throws Exception {
        final Mount childMountA = createMock(Mount.class);
        final Mount childMountB = createMock(Mount.class);
        final Mount childMountC = createMock(Mount.class);
        final List<Mount> childMounts = Arrays.asList(childMountA, childMountB, childMountC);
        final Channel channel = createMock(Channel.class);

        expect(mountFoo.getChildMounts()).andReturn(childMounts).anyTimes();
        expect(mountFoo.getChannel()).andReturn(channel).anyTimes();
        expect(mountBah.getChildMounts()).andReturn(Collections.emptyList()).anyTimes();
        expect(channel.getName()).andReturn("foo").anyTimes();
        expect(childMountA.getMountPath()).andReturn("/a").anyTimes();
        expect(childMountB.getMountPath()).andReturn("/b").anyTimes();
        expect(childMountC.getMountPath()).andReturn("/a").anyTimes();
        expect(childMountA.isExplicit()).andReturn(true).anyTimes();
        expect(childMountB.isExplicit()).andReturn(true).anyTimes();
        expect(childMountC.isExplicit()).andReturn(true).anyTimes();

        replay(mountFoo, mountBah, channel, childMountA, childMountB, childMountC);

        try {
            validator.validate(null);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.CHILD_MOUNT_EXISTS));
            assertThat(e.getParameterMap().get("channel"), is("foo"));
            assertThat(e.getParameterMap().get("childMountList"), is("/a, /b"));
            throw e;
        }
    }

    @Test
    public void no_exception_when_validating_mount_with_only_implicit_children_mounts() throws Exception {
        final Mount childMountA = createMock(Mount.class);
        final Mount childMountB = createMock(Mount.class);
        final Mount childMountC = createMock(Mount.class);
        final List<Mount> childMounts = Arrays.asList(childMountA, childMountB, childMountC);
        final Channel channel = createMock(Channel.class);

        expect(mountFoo.getChildMounts()).andReturn(childMounts).anyTimes();
        expect(mountFoo.getChannel()).andReturn(channel).anyTimes();
        expect(mountBah.getChildMounts()).andReturn(Collections.emptyList()).anyTimes();
        expect(channel.getName()).andReturn("foo").anyTimes();
        expect(childMountA.getMountPath()).andReturn("/a").anyTimes();
        expect(childMountB.getMountPath()).andReturn("/b").anyTimes();
        expect(childMountC.getMountPath()).andReturn("/a").anyTimes();
        expect(childMountA.isExplicit()).andReturn(false).anyTimes();
        expect(childMountB.isExplicit()).andReturn(false).anyTimes();
        expect(childMountC.isExplicit()).andReturn(false).anyTimes();

        replay(mountFoo, mountBah, channel, childMountA, childMountB, childMountC);

        validator.validate(null);

    }

}