/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @version $Id$
 */
public class TestHstContainerURL {
    
    private HstContainerURLImpl testUrl1;
    private Map<String, String []> testActionParams;
    private Map<String, String []> testRenderParams;
    
    @Before
    public void setUp() {
        testUrl1 = new HstContainerURLImpl();
        
        testUrl1.setCharacterEncoding("ISO-8859-1");
        testUrl1.setURIEncoding("UTF-8");
        testUrl1.setHostName("localhost");
        testUrl1.setPortNumber(8085);
        testUrl1.setContextPath("/site1");
        testUrl1.setResolvedMountPath("/preview");
        testUrl1.setPathInfo("/news");
        testUrl1.setRequestPath("/preview/news");
        testUrl1.setResourceId("resource1");
        testUrl1.setResourceWindowReferenceNamespace("r1");
        
        testActionParams = new HashMap<String, String []>();
        testActionParams.put("p1", new String [] { "ap1" });
        testActionParams.put("p2", new String [] { "ap2" });
        testActionParams.put("p3", new String [] { "ap3" });
        
        testUrl1.setActionParameters(testActionParams);
        
        testRenderParams = new HashMap<String, String []>();
        testRenderParams.put("p1", new String [] { "rp1" });
        testRenderParams.put("p2", new String [] { "rp2" });
        testRenderParams.put("p3", new String [] { "rp3" });
        
        testUrl1.setParameters(testRenderParams);
    }
    
    @Test
    public void testClone() throws Exception {
        HstContainerURLImpl cloned = (HstContainerURLImpl) testUrl1.clone();
        
        cloned.setCharacterEncoding("UNKNOWN-8");
        assertFalse(cloned.getCharacterEncoding().equals(testUrl1.getCharacterEncoding()));
        
        cloned.setURIEncoding("UNKNOWN-8");
        assertFalse(cloned.getURIEncoding().equals(testUrl1.getURIEncoding()));

        cloned.setHostName("UnknownHost");
        assertFalse(cloned.getHostName().equals(testUrl1.getHostName()));
        
        cloned.setPortNumber(Integer.MAX_VALUE);
        assertFalse(cloned.getPortNumber() == testUrl1.getPortNumber());
        
        cloned.setContextPath("/unknown");
        assertFalse(cloned.getContextPath().equals(testUrl1.getContextPath()));
        
        cloned.setResolvedMountPath("/unknownmount");
        assertFalse(cloned.getResolvedMountPath().equals(testUrl1.getResolvedMountPath()));
        
        cloned.setPathInfo("/unknowncontent");
        assertFalse(cloned.getPathInfo().equals(testUrl1.getPathInfo()));
        
        cloned.setRequestPath("/unknownmount/unknowncontent");
        assertFalse(cloned.getRequestPath().equals(testUrl1.getRequestPath()));
        
        cloned.setResourceId("unknownresource");
        assertFalse(cloned.getResourceId().equals(testUrl1.getResourceId()));
        
        cloned.setResourceWindowReferenceNamespace("ur1");
        assertFalse(cloned.getResourceWindowReferenceNamespace().equals(testUrl1.getResourceWindowReferenceNamespace()));
        
        Map<String, String []> actionParams = new HashMap<String, String []>();
        actionParams.put("p1", new String [] { "cloned - ap1" });
        actionParams.put("p2", new String [] { "cloned - ap2" });
        actionParams.put("p3", new String [] { "cloned - ap3" });
        
        cloned.setActionParameters(actionParams);
        assertFalse(isEqualParams(cloned.getActionParameterMap(), testUrl1.getActionParameterMap()));
        
        Map<String, String []> renderParams = new HashMap<String, String []>();
        renderParams.put("p1", new String [] { "cloned - rp1" });
        renderParams.put("p2", new String [] { "cloned - rp2" });
        renderParams.put("p3", new String [] { "cloned - rp3" });
        
        cloned.setParameters(renderParams);
        assertFalse(isEqualParams(cloned.getParameterMap(), testUrl1.getParameterMap()));
    }
    
    private boolean isEqualParams(Map<String, String []> params1, Map<String, String []> params2) {
        if (params1 == params2 || params1.equals(params2)) {
            return true;
        }
        
        if (params1.size() != params2.size()) {
            return false;
        }
        
        for (Map.Entry<String, String []> entry : params1.entrySet()) {
            String key = entry.getKey();
            
            if (!params2.containsKey(key)) {
                return false;
            }
            
            String [] valueArray1 = entry.getValue();
            String [] valueArray2 = params2.get(key);
            
            if ((valueArray1 != null && valueArray2 == null) || (valueArray1 == null && valueArray2 != null)) {
                return false;
            } else if (valueArray1 != null && valueArray2 != null) {
                if (valueArray1.length != valueArray2.length) {
                    return false;
                } else {
                    for (int i = 0; i < valueArray1.length; i++) {
                        if (!StringUtils.equals(valueArray1[i], valueArray2[i])) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
}
