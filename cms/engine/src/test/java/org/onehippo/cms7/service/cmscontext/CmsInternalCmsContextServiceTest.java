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
package org.onehippo.cms7.service.cmscontext;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsContextServiceImpl;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CmsInternalCmsContextServiceTest {

    @Test
    public void testCreateAndInvalidateSessionContext() {
        CmsContextServiceImpl service = new CmsContextServiceImpl();
        MockHttpSession session = new MockHttpSession();
        CmsSessionContext ctx = service.create(session);

        assertSame(service.getSessionContext(ctx.getId()), ctx);
        assertEquals(ctx.getCmsContextServiceId(), service.getId());
        assertSame(CmsSessionContext.getContext(session), ctx);

        // this will not be allowed/immediately undone, without invalidating the ctx itself
        session.setAttribute("foo", ctx);
        assertNull(session.getAttribute("foo"));
        assertSame(service.getSessionContext(ctx.getId()), ctx);
        assertEquals(ctx.getCmsContextServiceId(), service.getId());
        assertSame(CmsSessionContext.getContext(session), ctx);

        // adding it to another session likewise not allowed/immediately undone, without invalidating the ctx itself
        MockHttpSession session2 = new MockHttpSession();
        session2.setAttribute(CmsSessionContext.SESSION_KEY, ctx);
        assertNull(session2.getAttribute(CmsSessionContext.SESSION_KEY));
        assertSame(service.getSessionContext(ctx.getId()), ctx);
        assertEquals(ctx.getCmsContextServiceId(), service.getId());
        assertSame(CmsSessionContext.getContext(session), ctx);

        // this will invalidate/detach the ctx
        session.removeAttribute(CmsSessionContext.SESSION_KEY);
        assertNull(service.getSessionContext(ctx.getId()));
        assertNull(ctx.getCmsContextServiceId());

        // recreate
        ctx = service.create(session);
        assertSame(service.getSessionContext(ctx.getId()), ctx);
        assertEquals(ctx.getCmsContextServiceId(), service.getId());

        // only create once
        assertSame(ctx, service.create(session));

        // invalidate session, will remove attributes and call HttpSessionBindingListener.valueUnbound()
        session.invalidate();
        assertNull(service.getSessionContext(ctx.getId()));
        assertNull(ctx.getCmsContextServiceId());

        // recreate
        session = new MockHttpSession();
        ctx = service.create(session);
        assertSame(service.getSessionContext(ctx.getId()), ctx);
        assertEquals(ctx.getCmsContextServiceId(), service.getId());

        // test sessionWillPassivate to detach the context as well
        ((HttpSessionActivationListener)ctx).sessionWillPassivate(new HttpSessionEvent(session));
        assertNull(service.getSessionContext(ctx.getId()));
        assertNull(ctx.getCmsContextServiceId());
    }

    @Test
    public void testsetDataAndClearDataOnDetach() {
        CmsContextServiceImpl service = new CmsContextServiceImpl();
        MockHttpSession session = new MockHttpSession();
        CmsSessionContext ctx = service.create(session);
        service.setData(ctx, "foo", "bar");
        assertEquals("bar", ctx.get("foo"));
        session.removeAttribute(CmsSessionContext.SESSION_KEY);
        assertNull(ctx.get("foo"));
    }

    @Test
    public void testAttachSessionContext() {
        CmsContextServiceImpl service = new CmsContextServiceImpl();
        MockHttpSession session1 = new MockHttpSession();
        CmsSessionContext ctx1 = service.create(session1);
        service.setData(ctx1, "foo", "bar");

        MockHttpSession session2 = new MockHttpSession();
        CmsSessionContext ctx2 = service.attachSessionContext(ctx1.getId(), session2);
        assertNotSame(ctx1, ctx2);
        assertEquals(ctx2.getCmsContextServiceId(), service.getId());
        assertNull(service.getSessionContext(ctx2.getId()));

        // access ctx1 context data via ctx2
        assertEquals("bar", ctx2.get("foo"));

        // update to ctx1 context becomes accessible through ctx2
        assertNull(ctx2.get("bar"));
        service.setData(ctx1, "bar", "foo");
        assertEquals("foo", ctx2.get("bar"));

        // invalidate ctx2, context access to ctx1 is removed
        session2.removeAttribute(CmsSessionContext.SESSION_KEY);
        assertNull(ctx2.getCmsContextServiceId());
        assertNull(ctx2.get("foo"));

        // ctx1 context remains available
        assertEquals("bar", ctx1.get("foo"));
    }

    @Test
    public void testInvalidateAttachedSessionContext() {
        CmsContextServiceImpl service = new CmsContextServiceImpl();
        MockHttpSession session1 = new MockHttpSession();
        CmsSessionContext ctx1 = service.create(session1);
        service.setData(ctx1, "foo", "bar");

        MockHttpSession session2 = new MockHttpSession();
        CmsSessionContext ctx2 = service.attachSessionContext(ctx1.getId(), session2);

        session1.invalidate();
        assertNull(ctx2.getCmsContextServiceId());
        assertNull(ctx2.get("bar"));
    }
}
