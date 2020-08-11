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
package org.hippoecm.hst.proxy;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.commons.beanutils.LazyDynaMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TestDynaBeanProxyUtils {

    @Test
    public void testDynaBeanProxy() throws IllegalAccessException, InstantiationException {
        
        DynaProperty[] props = new DynaProperty[] { 
                new DynaProperty("firstName", String.class), 
                new DynaProperty("lastName", String.class), 
                new DynaProperty("addresses", Map.class),
                new DynaProperty("address", Map.class),
                new DynaProperty("favorites", String[].class),
                new DynaProperty("favorite", String[].class),
                };
        
        DynaClass dynaClass = new BasicDynaClass("employee", null, props);
        DynaBean dynaBean = dynaClass.newInstance();

        IPersonInfo personInfo = (IPersonInfo) DynaBeanProxyUtils.createDynaBeanProxy(dynaBean, IPersonInfo.class);
        
        personInfo.setFirstName("Fanny");
        assertEquals("Fanny", personInfo.getFirstName());
        assertEquals("Fanny", dynaBean.get("firstName"));
        
        personInfo.setLastName("Blankers-Koen");
        assertEquals("Blankers-Koen", personInfo.getLastName());
        assertEquals("Blankers-Koen", dynaBean.get("lastName"));
        
        Map<String, String> addresses = new HashMap<String, String>();
        addresses.put("home", "111 B.Stamplein Hoofddorp");
        addresses.put("work", "222 Oosteinde Amterdam");
        personInfo.setAddresses(addresses);
        dynaBean.set("address", addresses);
        assertEquals(addresses, personInfo.getAddresses());
        assertEquals(addresses, dynaBean.get("addresses"));
        
        personInfo.setAddress("work", "101 Petaluma");
        assertEquals("101 Petaluma", personInfo.getAddress("work"));
        assertEquals("101 Petaluma", ((Map) dynaBean.get("addresses")).get("work"));

        String [] favorites = { "Football", "Horse Riding", "Hockey" };
        personInfo.setFavorites(favorites);
        dynaBean.set("favorite", favorites);
        assertTrue(favorites == personInfo.getFavorites());
        assertTrue(favorites == dynaBean.get("favorites"));
        
        personInfo.setFavorite(1, "Handball");
        assertEquals("Handball", personInfo.getFavorite(1));
        assertEquals("Handball", ((String []) dynaBean.get("favorites"))[1]);
    }

    @Test
    public void testLazyDynaBeanProxy() throws IllegalAccessException, InstantiationException {
        
        DynaBean dynaBean = new LazyDynaBean();

        IPersonInfo personInfo = (IPersonInfo) DynaBeanProxyUtils.createDynaBeanProxy(dynaBean, IPersonInfo.class);
        
        personInfo.setFirstName("Fanny");
        assertEquals("Fanny", personInfo.getFirstName());
        assertEquals("Fanny", dynaBean.get("firstName"));
        
        personInfo.setLastName("Blankers-Koen");
        assertEquals("Blankers-Koen", personInfo.getLastName());
        assertEquals("Blankers-Koen", dynaBean.get("lastName"));
        
        Map<String, String> addresses = new HashMap<String, String>();
        addresses.put("home", "111 B.Stamplein Hoofddorp");
        addresses.put("work", "222 Oosteinde Amterdam");
        personInfo.setAddresses(addresses);
        dynaBean.set("address", addresses);
        assertEquals(addresses, personInfo.getAddresses());
        assertEquals(addresses, dynaBean.get("addresses"));
        
        personInfo.setAddress("work", "101 Petaluma");
        assertEquals("101 Petaluma", personInfo.getAddress("work"));
        assertEquals("101 Petaluma", ((Map) dynaBean.get("addresses")).get("work"));

        String [] favorites = { "Football", "Horse Riding", "Hockey" };
        personInfo.setFavorites(favorites);
        dynaBean.set("favorite", favorites);
        assertTrue(favorites == personInfo.getFavorites());
        assertTrue(favorites == dynaBean.get("favorites"));
        
        personInfo.setFavorite(1, "Handball");
        assertEquals("Handball", personInfo.getFavorite(1));
        assertEquals("Handball", ((String []) dynaBean.get("favorites"))[1]);
    }
    
    @Test
    public void testLazyDynaBeanProxyWithMethods() throws IllegalAccessException, InstantiationException {
        
        DynaBean dynaBean = new LazyDynaBean() {
            // Just example method implementation
            public String toXMLString() {
                StringBuilder sb = new StringBuilder();
                sb.append("<bean>");
                for (Object entryItem : getMap().entrySet()) {
                    Map.Entry entry = (Map.Entry) entryItem;
                    sb.append("<" + entry.getKey() + ">");
                    sb.append(entry.getValue());
                    sb.append("</" + entry.getKey() + ">");
                }
                sb.append("</bean>");
                return sb.toString();
            }
        };

        IPersonInfo personInfo = (IPersonInfo) DynaBeanProxyUtils.createDynaBeanProxy(dynaBean, IPersonInfo.class);
        
        personInfo.setFirstName("Fanny");
        assertEquals("Fanny", personInfo.getFirstName());
        assertEquals("Fanny", dynaBean.get("firstName"));
        
        personInfo.setLastName("Blankers-Koen");
        assertEquals("Blankers-Koen", personInfo.getLastName());
        assertEquals("Blankers-Koen", dynaBean.get("lastName"));
        
        Map<String, String> addresses = new HashMap<String, String>();
        addresses.put("home", "111 B.Stamplein Hoofddorp");
        addresses.put("work", "222 Oosteinde Amterdam");
        personInfo.setAddresses(addresses);
        dynaBean.set("address", addresses);
        assertEquals(addresses, personInfo.getAddresses());
        assertEquals(addresses, dynaBean.get("addresses"));
        
        personInfo.setAddress("work", "101 Petaluma");
        assertEquals("101 Petaluma", personInfo.getAddress("work"));
        assertEquals("101 Petaluma", ((Map) dynaBean.get("addresses")).get("work"));

        String [] favorites = { "Football", "Horse Riding", "Hockey" };
        personInfo.setFavorites(favorites);
        dynaBean.set("favorite", favorites);
        assertTrue(favorites == personInfo.getFavorites());
        assertTrue(favorites == dynaBean.get("favorites"));
        
        personInfo.setFavorite(1, "Handball");
        assertEquals("Handball", personInfo.getFavorite(1));
        assertEquals("Handball", ((String []) dynaBean.get("favorites"))[1]);
        
        String xml = personInfo.toXMLString();
        assertNotNull(xml);
        assertTrue(xml.startsWith("<bean>"));
        assertTrue(xml.endsWith("</bean>"));
    }
    
    @Test
    public void testLazyDynaMapProxyWithMethods() throws IllegalAccessException, InstantiationException {
        
        Map<String, Object> values = new HashMap<String, Object>();
        
        values.put("firstName", "Fanny");
        values.put("lastName", "Blankers-Koen");
        Map<String, String> addresses = new HashMap<String, String>();
        addresses.put("home", "111 B.Stamplein Hoofddorp");
        addresses.put("work", "222 Oosteinde Amterdam");
        values.put("addresses", addresses);
        values.put("address", addresses);
        String [] favorites = { "Football", "Horse Riding", "Hockey" };
        values.put("favorites", favorites);
        values.put("favorite", favorites);
        
        LazyDynaMap dynaBean = new LazyDynaMap(values);

        IPersonInfo personInfo = (IPersonInfo) DynaBeanProxyUtils.createDynaBeanProxy(dynaBean, IPersonInfo.class);
        
        assertEquals("Fanny", personInfo.getFirstName());
        assertEquals("Fanny", dynaBean.get("firstName"));
        
        assertEquals("Blankers-Koen", personInfo.getLastName());
        assertEquals("Blankers-Koen", dynaBean.get("lastName"));
        
        assertEquals(addresses, personInfo.getAddresses());
        assertEquals(addresses, dynaBean.get("addresses"));
        
        personInfo.setAddress("work", "101 Petaluma");
        assertEquals("101 Petaluma", personInfo.getAddress("work"));
        assertEquals("101 Petaluma", ((Map) dynaBean.get("addresses")).get("work"));

        dynaBean.set("favorite", favorites);
        assertTrue(favorites == personInfo.getFavorites());
        assertTrue(favorites == dynaBean.get("favorites"));
        
        personInfo.setFavorite(1, "Handball");
        assertEquals("Handball", personInfo.getFavorite(1));
        assertEquals("Handball", ((String []) dynaBean.get("favorites"))[1]);
    }
    
}
