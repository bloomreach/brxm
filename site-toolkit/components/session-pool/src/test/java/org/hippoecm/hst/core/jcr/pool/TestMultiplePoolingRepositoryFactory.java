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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMultiplePoolingRepositoryFactory {
    
    private Map<String, String> multiplePoolConfigMap = new HashMap<String, String>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() {
        multiplePoolConfigMap.put("repositoryAddress", " , ");
        multiplePoolConfigMap.put("defaultCredentialsUserID", "admin@1, admin@2");
        multiplePoolConfigMap.put("defaultCredentialsUserIDSeparator", "@, @");
        multiplePoolConfigMap.put("defaultCredentialsPassword", "admin, admin");
        multiplePoolConfigMap.put("maxActive", "4, 4");
        multiplePoolConfigMap.put("maxIdle", "2, 2");
        multiplePoolConfigMap.put("minIdle", "1, 1");
        multiplePoolConfigMap.put("initialSize", "1, 1");
        multiplePoolConfigMap.put("maxWait", "10000, 10000");
        multiplePoolConfigMap.put("testOnBorrow", "true, true");
        multiplePoolConfigMap.put("testOnReturn", "false, false");
        multiplePoolConfigMap.put("testWhileIdle", "false, false");
        multiplePoolConfigMap.put("timeBetweenEvictionRunsMillis", "60000, 60000");
        multiplePoolConfigMap.put("numTestsPerEvictionRun", "1, 1");
        multiplePoolConfigMap.put("minEvictableIdleTimeMillis", "60000, 60000");
        multiplePoolConfigMap.put("refreshOnPassivate", "true, true");
    }
    
    @Test
    public void testMultiplePoolingRepositoryFactory() throws Exception {
        ObjectFactory factory = new MultiplePoolingRepositoryFactory();
        
        MultipleRepository multipleRepository = (MultipleRepository) factory.getObjectInstance(getMultiplePoolReference(), null, null, null);
        
        Map<Credentials, Repository> repoMap = multipleRepository.getRepositoryMap();
        assertEquals("The repository map size is wrong.", 2, repoMap.size());
        
        Credentials firstCreds = new SimpleCredentials("admin@1", "admin".toCharArray());
        Repository firstRepo = multipleRepository.getRepositoryByCredentials(firstCreds);
        assertNotNull(firstRepo);
        assertTrue(firstRepo instanceof PoolingRepository);
        
        Credentials secondCreds = new SimpleCredentials("admin@2", "admin".toCharArray());
        Repository secondRepo = multipleRepository.getRepositoryByCredentials(secondCreds);
        assertNotNull(secondRepo);
        assertTrue(secondRepo instanceof PoolingRepository);
        
        assertMultiplePoolProperties(multiplePoolConfigMap, 0, firstRepo);
        assertMultiplePoolProperties(multiplePoolConfigMap, 1, secondRepo);
        
        Session session = multipleRepository.login(firstCreds);
        assertNotNull(session);
        assertTrue(session instanceof PooledSession);
        assertEquals("admin@1", session.getUserID());
        session.logout();
        
        session = multipleRepository.login(secondCreds);
        assertNotNull(session);
        assertTrue(session instanceof PooledSession);
        assertEquals("admin@2", session.getUserID());
        session.logout();
    }
    
    private void assertMultiplePoolProperties(Map<String, String> expectedPropMap, int index, Object bean) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (Map.Entry<String, String> entry : expectedPropMap.entrySet()) {
            String propName = entry.getKey();
            String [] propValues = StringUtils.splitPreserveAllTokens(entry.getValue(), ",");
            assertTrue(propValues.length > index);
            Object beanPropValue = PropertyUtils.getProperty(bean, entry.getKey());
            assertNotNull("Cannot find a property from the bean: " + bean + ", " + propName, beanPropValue);
            
            if (beanPropValue instanceof char []) {
                assertEquals("The property has a different value: " + propName, propValues[index].trim(), new String((char []) beanPropValue));
            } else {
                assertEquals("The property has a different value: " + propName, propValues[index].trim(), beanPropValue.toString());
            }
        }
    }
    
    private Reference getMultiplePoolReference() {
        Reference reference = null;
        
        final List<RefAddr> refAddrs = new ArrayList<RefAddr>();
        
        for (Map.Entry<String, String> entry : multiplePoolConfigMap.entrySet()) {
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
