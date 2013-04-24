/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.eventbus;

import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;

import com.google.common.collect.Multimap;
import com.google.common.eventbus.HippoAnnotationHandlerFinder;

public class HippoAnnotationHandlerFinderTest {

    @Test
    public void testAnonymousAnnotationsFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        
        Multimap map = finder.findAllHandlers(new Object() {
            @Subscribe
            public void thisIsAHandler(HippoEvent<?> event) {
                // do nothing
            }
        });
        
        assertTrue(map.size() == 1);
    }
    
    @Test
    public void testDirectAnnotationsFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        
        Multimap map = finder.findAllHandlers(new A());
        
        assertTrue(map.size() == 2);
    }
    
    @Test
    public void testInheritedAnnotationsFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        Multimap map = finder.findAllHandlers(new B());
        
        assertTrue(map.size() == 2);
    }
 
    @Test
    public void testInheritedAnnotationsWithOverrdeWithoutAnnotationFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        Multimap map = finder.findAllHandlers(new C());
        // overrding a method and then not including the annotation should take the annotation
        // from the overriden class
        assertTrue(map.size() == 2);
    }
 
    
    @Test
    public void testInterfaceAnnonymousAnnotationsFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        
        Multimap map = finder.findAllHandlers(new I() {
            @Override
            public void thisIsAHandler(HippoEvent<?> event) {
                // do nothing
            }
        });
        
        assertTrue(map.size() == 1);
    }
    
    
    @Test
    public void testInterfaceAnnonymousAnnotationWithExtraSubscribeMethodFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        
        Multimap map = finder.findAllHandlers(new I() {
            @Override
            public void thisIsAHandler(HippoEvent<?> event) {
                // do nothing
            }
            
            @Subscribe
            public void testExtra(HippoEvent<?> event) {
                //
            }
        });
        // added an extra Subscribe method
        assertTrue(map.size() == 2);
    }
    
    @Test
    public void testInterfaceImplAnnotationsFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        
        Multimap map = finder.findAllHandlers(new Impl() {
           
        });
        
        assertTrue(map.size() == 1);
    } 
    
    @Test
    public void testInterfaceSubImplWithExtraInterfaceAnnotationsFinder() {
        HippoAnnotationHandlerFinder finder = new HippoAnnotationHandlerFinder();
        
        Multimap map = finder.findAllHandlers(new SubImpl() {
           
        });
        
        // we have now 2 Subscribe annotations through two different interfaces
        assertTrue(map.size() == 2);
    }
    
    public class SubImpl extends Impl implements I2 {

        @Override
        public void thisIsAHandler(HippoEvent<?> event) {
           // nothing 
        }

        @Override
        public void thisIsASecondHandler(HippoEvent<?> event) {
           // nothing
        }
        
    }
    
    public class Impl implements I {

        @Override
        public void thisIsAHandler(HippoEvent<?> event) {
           // nothing 
        }
        
    }
    
    public interface I {
        @Subscribe
        public void thisIsAHandler(HippoEvent<?> event);
    }
    
    public interface I2 {
        @Subscribe
        public void thisIsASecondHandler(HippoEvent<?> event);
    }
    
    public class A extends B {
        
    }
    
    public class C extends B {
        @Override
        public void thisIsAHandler(HippoEvent<?> event) {
           
        } 
    }
    
    public class B {
        @Subscribe
        public void thisIsAHandler(HippoEvent<?> event) {
            // do nothing
        }
        @Subscribe
        public void thisIsASecondHandler(HippoEvent<?> event) {
            // do nothing
        }
    }
    
}
