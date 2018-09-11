/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.util;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.util.WebFileUtils.getBundleName;
import static org.junit.Assert.assertThat;

public class WebFileUtilsTest {

    private HstRequestContext context;
    private ResolvedMount resolvedMount;
    private Mount mount;

    @Before
    public void setUp() {

        this.context = createMock(HstRequestContext.class);
        this.resolvedMount = createMock(ResolvedMount.class);
        this.mount = createMock(Mount.class);

        expect(context.getResolvedMount()).andReturn(resolvedMount);
        expect(resolvedMount.getMount()).andReturn(mount);
        replay(context, resolvedMount);
    }

    @Test
    public void testGetBundleName_returns_default_when_mount_has_no_contextpath() {
        expect(mount.getContextPath()).andReturn(null);
        expect(mount.getParent()).andReturn(null);
        replay(mount);
        assertThat(getBundleName(context), is(WebFileUtils.DEFAULT_BUNDLE_NAME));
    }

    @Test
    public void testGetBundleName_removes_start_slash_from_context_path() {
        expect(mount.getContextPath()).andReturn("/a/b/c/d/");
        replay(mount);
        assertThat(getBundleName(context), is("a/b/c/d/"));
    }
}
