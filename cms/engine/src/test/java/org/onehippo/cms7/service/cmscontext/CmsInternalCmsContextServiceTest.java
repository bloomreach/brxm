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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
    public void root_session_invalidation_invalidates_secondary_sessions() {
        CmsContextServiceImpl service = new CmsContextServiceImpl();
        MockHttpSession session1 = new MockHttpSession();

        CmsSessionContext ctx1 = service.create(session1);
        service.setData(ctx1, "foo", "bar");

        MockHttpSession session2 = new MockHttpSession();
        CmsSessionContext ctx2 = service.attachSessionContext(ctx1.getId(), session2);

        MockHttpSession session3 = new MockHttpSession();
        CmsSessionContext ctx3 = service.attachSessionContext(ctx1.getId(), session3);

        assertFalse(session2.isInvalid());

        session1.invalidate();

        assertTrue(session2.isInvalid());
        assertTrue(session3.isInvalid());

        assertNull(ctx1.getCmsContextServiceId());
        assertNull(ctx1.get("foo"));

        assertNull(ctx2.getCmsContextServiceId());
        assertNull(ctx2.get("foo"));

        assertNull(ctx3.getCmsContextServiceId());
        assertNull(ctx3.get("foo"));
    }

    @Test
    public void secondary_session_invalidation_does_not_invalidate_other_sessions() {
        CmsContextServiceImpl service = new CmsContextServiceImpl();
        MockHttpSession session1 = new MockHttpSession();
        CmsSessionContext ctx1 = service.create(session1);
        service.setData(ctx1, "foo", "bar");

        MockHttpSession session2 = new MockHttpSession();
        service.attachSessionContext(ctx1.getId(), session2);

        MockHttpSession session3 = new MockHttpSession();
        service.attachSessionContext(ctx1.getId(), session3);

        assertFalse(session2.isInvalid());

        CmsSessionContext ctx2 = service.attachSessionContext(ctx1.getId(), session2);
        CmsSessionContext ctx3 = service.attachSessionContext(ctx1.getId(), session3);

        session2.invalidate();

        assertTrue(session2.isInvalid());

        assertFalse("root session should still be valid ", session1.isInvalid());
        assertFalse("secondary session3 should still be valid ", session3.isInvalid());

        assertNotNull(ctx1.getCmsContextServiceId());
        assertNotNull(ctx1.get("foo"));

        assertNotNull(ctx3.getCmsContextServiceId());
        assertNotNull(ctx3.get("foo"));

        assertNull(ctx2.getCmsContextServiceId());
        assertNull(ctx2.get("foo"));
    }

}
