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
package org.hippoecm.hst.jackrabbit.ocm.jndi;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;


/**
 * ObjectContentManagerProviderObjectFactory
 * 
 * @version $Id$
 */
public class ObjectContentManagerProviderObjectFactory implements ObjectFactory 
{
    
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception 
    {
        String providerClassName = null;
        String annotatedClassNames = null;
        String xmlMappingResources = null;
        
        if (obj != null && obj instanceof Reference) {
            Reference ref = (Reference) obj;
            Enumeration<RefAddr> addrs = ref.getAll();
            
            while (addrs.hasMoreElements()) {
                RefAddr addr = (RefAddr) addrs.nextElement();
                String type = addr.getType();
                String value = (String) addr.getContent();
                
                if ("providerClassName".equals(type))
                {
                    providerClassName = (value != null ? value.trim() : value);
                }
                else if ("annotatedClassNames".equals(type))
                {
                    annotatedClassNames = (value != null ? value.trim() : value);
                }
                else if ("xmlMappingResources".equals(type))
                {
                    xmlMappingResources = (value != null ? value.trim() : value);
                }
            }
        }
        
        ObjectContentManagerProvider provider = null;
        
        if (annotatedClassNames != null && !"".equals(annotatedClassNames))
        {
            List<Class> annotatedBeanClasses = new ArrayList<Class>();
            
            for (String annotatedClassName : split(annotatedClassNames, " ,\t\r\n"))
            {
                annotatedBeanClasses.add(Thread.currentThread().getContextClassLoader().loadClass(annotatedClassName));
            }
            
            if (providerClassName != null && !"".equals(providerClassName))
            {
                Object providerObj = Thread.currentThread().getContextClassLoader().loadClass(providerClassName).getConstructor(new Class [] { List.class }).newInstance(new Object [] { annotatedBeanClasses });
                
                if (providerObj instanceof ObjectContentManagerProvider)
                {
                    provider = (ObjectContentManagerProvider) providerObj;
                }
                else
                {
                    provider = new DelegatingObjectContentManagerProvider(providerObj);
                }
            }
            else
            {
                provider = new DefaultObjectContentManagerProvider(annotatedBeanClasses);
            }
        }
        else if (xmlMappingResources != null && !"".equals(xmlMappingResources))
        {
            String [] xmlMappingFiles = split(xmlMappingResources, " ,\t\r\n");
            
            if (providerClassName != null && !"".equals(providerClassName))
            {
                Object providerObj = (ObjectContentManagerProvider) Thread.currentThread().getContextClassLoader().loadClass(providerClassName).getConstructor(new Class [] { String [].class }).newInstance(new Object [] { xmlMappingFiles });
                
                if (providerObj instanceof ObjectContentManagerProvider)
                {
                    provider = (ObjectContentManagerProvider) providerObj;
                }
                else
                {
                    provider = new DelegatingObjectContentManagerProvider(providerObj);
                }
            }
            else
            {
                provider = new DefaultObjectContentManagerProvider(xmlMappingFiles);
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid configuration. annotatedClassNames or xmlMappingResources should be configured.");
        }
        
        return provider;
    }
    
    private String [] split(String source, String tokens)
    {
        ArrayList<String> list = new ArrayList<String>();
        
        StringTokenizer st = new StringTokenizer(source, tokens);
        
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            
            if (!"".equals(token))
            {
                list.add(token);
            }
        }
        
        return list.toArray(new String[list.size()]);
    }
}