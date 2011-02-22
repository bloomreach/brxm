package org.hippoecm.hst.utils;

/*
 *  Copyright 2011 Hippo.
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

import org.junit.Test;

public class TestBeanUtilsHasAccessor {

    @Test
    public void testhasAccessor() throws Exception {
        
        Object o = new TestObject();
        assertTrue("TestObject should have getIt ",BeanUtils.hasAccessor(o, "it"));
        assertTrue("TestObject should have isItAlso ",BeanUtils.hasAccessor(o, "itAlso"));
        assertFalse("TestObject should not have getItPublic as public ",BeanUtils.hasAccessor(o, "itPublic"));
        assertFalse("TestObject should not have isItPublicAlso as public ",BeanUtils.hasAccessor(o, "itPublicAlso"));
        
    }
    
    
    public class TestObject {

        public String getIt(){
            return "it";
        }
        
        public boolean isItAlso(){
            return true;
        }

        String getItPublic(){
            return "public";
        }
        boolean isItPublicAlso(){
            return false;
        }
    }
}
