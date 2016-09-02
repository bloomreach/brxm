/*
 *  Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.HashSet;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FactoryComponentInstanceCreationIT extends AbstractSpringTestCase {

    @Test
    public void testComponentInstances() {

        Mount mount1 = createNiceMock(Mount.class);
        expect(mount1.getIdentifier()).andReturn("cafe-babe").anyTimes();

        Mount mount2 = createNiceMock(Mount.class);
        // same identifier as mount1 on purpose!
        expect(mount2.getIdentifier()).andReturn("cafe-babe").anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount1).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<String>()).anyTimes();

        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        //expect(compConfig.getReferenceName()).andReturn("refName").anyTimes();
        expect(compConfig.getId()).andReturn("some//path").anyTimes();

        replay(mockHstContainerConfig, compConfig, mount1, mount2, resolvedMount, requestContext);

        HstComponentFactory componentFactory = getComponent(HstComponentFactory.class.getName());
        HstComponent componentInstance1 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
        HstComponent componentInstance2 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);

        assertTrue("Component instances should be same for same mount and compConfig instance",componentInstance1 == componentInstance2);

        // now create an instance with mount2. Even though mount2 has the same identifier as mount1, still
        // a different component instance should be created this time, because the hashCode is also accounted for by
        // the componentFactory. If the hashCode by coincidence is the same, the component instance would be the same
        // hence also check for the hashCode to be sure
        HstComponent componentInstance3 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount2);

        if (mount2.hashCode() != mount1.hashCode()) {
            assertTrue("Component instances should not be the same because different mount instances", componentInstance1 != componentInstance3);
        }
    }

    @Test
    public void testComponentCreationFails() {

        Mount mount1 = createNiceMock(Mount.class);
        expect(mount1.getIdentifier()).andReturn("cafe-babe").anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount1).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<>()).anyTimes();

        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getId()).andReturn("some//path").anyTimes();
        expect(compConfig.getComponentClassName()).andReturn("org.hippoecm.hst.core.container.NotExistingClass").anyTimes();

        replay(mockHstContainerConfig, compConfig, mount1, resolvedMount, requestContext);

        HstComponentFactory componentFactory = getComponent(HstComponentFactory.class.getName());
        try {
            componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
            fail("Exception should had been thrown");
        } catch (HstComponentException e) {
            try {
                componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
                fail("Exception should had been thrown");
            } catch (HstComponentException e2) {
                // second failure should result in exact same exception instance
                assertTrue(e == e2);
            }
        }

    }

}
