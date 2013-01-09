package org.hippoecm.hst.utils;

/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

public class TestBeanUtilsIsReadable {

    @Test
    public void testhasAccessor() throws Exception {
        
        Object o = new TestObject();
        assertTrue("TestObject should have getIt ",PropertyUtils.isReadable(o, "it"));
        assertTrue("TestObject should have isItAlso ",PropertyUtils.isReadable(o, "itAlso"));
        assertTrue("TestObject should have getTestObject2 and the returned Object must have getIt ",PropertyUtils.isReadable(o, "testObject2.it"));
        assertTrue("TestObject should have getObject",PropertyUtils.isReadable(o, "object"));
        assertFalse("TestObject should have getObject but the returned Object does not have getIt",PropertyUtils.isReadable(o, "object.it"));
        assertFalse("TestObject should not have getItPublic as public ",PropertyUtils.isReadable(o, "itPublic"));
        assertFalse("TestObject should not have isItPublicAlso as public ",PropertyUtils.isReadable(o, "itPublicAlso"));
        
    }
    
    
    public class TestObject {

        public String getIt(){
            return "it";
        }
        
        public boolean isItAlso(){
            return true;
        }

        public TestObject2 getTestObject2() {
            return new TestObject2();
        }
        public Object getObject() {
            return new Object();
        }
        
        String getItPublic(){
            return "public";
        }
        
        boolean isItPublicAlso(){
            return false;
        }
        
       
    }
    
    public class TestObject2 {

        public String getIt(){
            return "it";
        }
        
    }
}
