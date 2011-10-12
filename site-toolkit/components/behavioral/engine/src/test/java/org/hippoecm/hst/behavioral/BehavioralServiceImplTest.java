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
package org.hippoecm.hst.behavioral;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.UserPrincipal;
import org.hippoecm.repository.TestCase;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

public class BehavioralServiceImplTest extends TestCase {
    
    @Test
    public void testDo() {
        BehavioralServiceImpl service = new BehavioralServiceImpl();
        service.setRepository(super.server.getRepository());
        service.setCredentials(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
        service.setBehavioralDataStore(new BehavioralDataHttpSessionStore());
        service.setConfigNodePath("/behavioral:configuration");
        
        // case 1: search site for term skateboard and have come from searching google for bicycles
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("query", "skateboard");
        request.setUserPrincipal(new UserPrincipal("test"));
        request.addHeader("referer", "http://www.google.com?q=bicycles");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        service.updateBehavioralData(request, response);
        
        BehavioralProfile profile = service.getBehavioralProfile(request);
        
//        printProfile(profile);
        
        assertTrue(profile.isPersona("youngcyclist"));
        assertTrue(profile.isPersona("authorizeduser"));
        
        
        // case 2: search site once for wheelchair and come from searching yahoo for cars
        request = new MockHttpServletRequest();
        request.setParameter("query", "wheelchair");
        request.setUserPrincipal(null);
        request.addHeader("referer", "http://www.yahoo.com?p=cars");
        
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);        
        ((MockHttpSession)request.getSession()).setNew(true);
        
        service.updateBehavioralData(request, response);
        
        profile = service.getBehavioralProfile(request);
        assertFalse(profile.isPersona("olddriver"));
        
        // case 3: with the same session as the previous case, search once more for wheelchair, thus meeting
        // the required threshold
        
        request = new MockHttpServletRequest();
        request.setParameter("query", "wheelchair");
        request.setSession(session);
        
        service.updateBehavioralData(request, response);
        
        profile = service.getBehavioralProfile(request);
        assertTrue(profile.isPersona("olddriver"));
    }
    
//    private void printProfile(BehavioralProfile profile) {
//        for (String personaId : profile.getPersonaIds()) {
//            System.out.println(personaId);  
//        }
//    }

}
