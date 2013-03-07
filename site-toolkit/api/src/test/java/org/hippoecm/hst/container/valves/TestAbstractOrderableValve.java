/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container.valves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hippoecm.hst.container.annotations.Orderable;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.OrderableValve;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.Test;

/**
 * TestAbstractOrderableValve
 */
public class TestAbstractOrderableValve {

    @Test
    public void testDefaultAnnotatedOrderableValve() throws Exception {
        OrderableValve valve = new DefaultAnnotatedValve();
        assertEquals(DefaultAnnotatedValve.class.getName(), valve.getName());
        assertTrue(isEmpty(valve.getAfter()));
        assertTrue(isEmpty(valve.getBefore()));
    }

    @Test
    public void testCustomRoleAnnotatedOrderableValve() throws Exception {
        OrderableValve valve = new CustomRoleAnnotatedValve();
        assertEquals(CustomRole.class.getName(), valve.getName());
        assertTrue(isEmpty(valve.getAfter()));
        assertTrue(isEmpty(valve.getBefore()));
    }

    @Test
    public void testCustomAnnotatedOrderableValve() throws Exception {
        OrderableValve valve = new CustomAnnotatedValve();
        assertEquals(CustomRole.class.getName(), valve.getName());
        assertEquals("some,postrequisites", valve.getBefore());
        assertEquals("some,prerequisites", valve.getAfter());
    }

    @Test
    public void testCustomManuallyConfiguredOrderableValve() throws Exception {
        AbstractOrderableValve valve = new CustomAnnotatedValve();
        valve.setName("manualName");
        valve.setBefore("some,manual,postrequisites");
        valve.setAfter("some,manual,prerequisites");

        assertEquals("manualName", valve.getName());
        assertEquals("some,manual,postrequisites", valve.getBefore());
        assertEquals("some,manual,prerequisites", valve.getAfter());
    }

    @Test
    public void testAnnotationInheritanceBySimpleInterface() throws Exception {
        AbstractOrderableValve valve = new CustomAnnotatedValve();
        assertEquals(CustomRole.class.getName(), valve.getName());
    }

    @Test
    public void testAnnotationInheritanceBySuperClass() throws Exception {
        OrderableValve valve = new ChildOfCustomValve();
        assertEquals(ChildOfCustomValve.class.getName(), valve.getName());

        valve = new OverridingChildOfCustomValve();
        assertEquals(OverridingChildOfCustomValve.class.getName(), valve.getName());
    }

    private static boolean isEmpty(String s) {
        return (s == null || "".equals(s));
    }

    @Orderable
    private static class DefaultAnnotatedValve extends AbstractOrderableValve {
        @Override
        public void invoke(ValveContext context) throws ContainerException {
        }
    }

    private interface CustomRole {
    }

    @Orderable(role=CustomRole.class)
    private static class CustomRoleAnnotatedValve extends AbstractOrderableValve implements CustomRole {
        @Override
        public void invoke(ValveContext context) throws ContainerException {
        }
    }

    @Orderable(
            role = CustomRole.class,
            before = "some,postrequisites",
            after = "some,prerequisites"
            )
    private static class CustomAnnotatedValve extends AbstractOrderableValve implements CustomRole {
        @Override
        public void invoke(ValveContext context) throws ContainerException {
        }
    }

    @Orderable(role = CustomValve.class)
    private static class CustomValve extends AbstractOrderableValve {
        @Override
        public void invoke(ValveContext context) throws ContainerException {
        }
    }

    private static class ChildOfCustomValve extends CustomValve {
    }

    @Orderable(role = OverridingChildOfCustomValve.class)
    private static class OverridingChildOfCustomValve extends ChildOfCustomValve {
    }
}
