/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBasicPoolingRepositoryFactory {
    
    private Map<String, String> basicPoolConfigMap = new HashMap<String, String>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }
    
    @Before
    public void setUp() {
        basicPoolConfigMap.put("repositoryAddress", "");
        basicPoolConfigMap.put("defaultCredentialsUserID", "admin");
        basicPoolConfigMap.put("defaultCredentialsPassword", "admin");
        basicPoolConfigMap.put("maxActive", "4");
        basicPoolConfigMap.put("maxIdle", "2");
        basicPoolConfigMap.put("minIdle", "1");
        basicPoolConfigMap.put("initialSize", "1");
        basicPoolConfigMap.put("maxWait", "10000");
        basicPoolConfigMap.put("testOnBorrow", "true");
        basicPoolConfigMap.put("testOnReturn", "false");
        basicPoolConfigMap.put("testWhileIdle", "false");
        basicPoolConfigMap.put("timeBetweenEvictionRunsMillis", "60000");
        basicPoolConfigMap.put("numTestsPerEvictionRun", "1");
        basicPoolConfigMap.put("minEvictableIdleTimeMillis", "60000");
        basicPoolConfigMap.put("refreshOnPassivate", "true");
    }
    
    @Test
    public void testBasicPoolingRepositoryFactory() throws Exception {
        ObjectFactory factory = new BasicPoolingRepositoryFactory();
        
        BasicPoolingRepository poolRepository = (BasicPoolingRepository) factory.getObjectInstance(getBasicPoolReference(), null, null, null);
        assertPoolProperties(basicPoolConfigMap, poolRepository);
        
        Session session = poolRepository.login();
        assertNotNull(session);
        session.logout();
    }

    private void assertPoolProperties(Map<String, String> expectedPropMap, Object bean) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (Map.Entry<String, String> entry : expectedPropMap.entrySet()) {
            String propName = entry.getKey();
            String propValue = entry.getValue();
            Object beanPropValue = PropertyUtils.getProperty(bean, entry.getKey());
            assertNotNull("Cannot find a property from the bean: " + bean + ", " + propName, beanPropValue);
            
            if (beanPropValue instanceof char []) {
                assertEquals("The property has a different value: " + propName, propValue, new String((char []) beanPropValue));
            } else {
                assertEquals("The property has a different value: " + propName, propValue, beanPropValue.toString());
            }
        }
    }
    
    private Reference getBasicPoolReference() {
        Reference reference = null;
        
        final List<RefAddr> refAddrs = new ArrayList<RefAddr>();
        
        for (Map.Entry<String, String> entry : basicPoolConfigMap.entrySet()) {
            StringRefAddr refAddr = new StringRefAddr(entry.getKey(), entry.getValue());
            refAddrs.add(refAddr);
        }
        
        reference = new Reference("test") {
            @Override
            public RefAddr get(int position) {
                return refAddrs.get(position);
            }
            
            @Override
            public RefAddr get(String addrType) {
                for (RefAddr refAddr : refAddrs) {
                    if (refAddr.getType().equals(addrType)) {
                        return refAddr;
                    }
                }
                return null;
            }
            
            @Override
            public Enumeration<RefAddr> getAll() {
                return new IteratorEnumeration(refAddrs.iterator());
            }
            
            @Override
            public int size() {
                return refAddrs.size();
            }
        };

        return reference;
    }
}
