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
package org.hippoecm.hst.jackrabbit.ocm.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.collections.list.SetUniqueList;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.digester.DigesterMapperImpl;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdDirectory;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdDocument;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdFacetSelect;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdFixedDirectory;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdFolder;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdHtml;
import org.hippoecm.hst.jackrabbit.ocm.manager.cache.NOOPObjectCache;
import org.hippoecm.hst.jackrabbit.ocm.manager.impl.HstAnnotationMapperImpl;
import org.hippoecm.hst.jackrabbit.ocm.manager.impl.HstObjectConverterImpl;
import org.hippoecm.hst.jackrabbit.ocm.query.impl.HstQueryManagerImpl;

/**
 * OCMUtils
 * 
 * @version $Id$
 */
public class OCMUtils {
    
    private static Collection<Class> hippoBaseBeanClasses = new ArrayList<Class>();
    
    static {
        hippoBaseBeanClasses.add(HippoStdHtml.class);
        hippoBaseBeanClasses.add(HippoStdDocument.class);
        hippoBaseBeanClasses.add(HippoStdFacetSelect.class);
        hippoBaseBeanClasses.add(HippoStdFolder.class);
        hippoBaseBeanClasses.add(HippoStdDirectory.class);
        hippoBaseBeanClasses.add(HippoStdFixedDirectory.class);
    }
    
    private OCMUtils() {
        
    }
    
    public static ObjectContentManager createObjectContentManager(Session session, Class [] annotatedClasses) {
        return createObjectContentManager(session, null, annotatedClasses);
    }
    
    public static ObjectContentManager createObjectContentManager(Session session, String [] fallBackJcrNodeTypes, Class [] annotatedClasses) {
        List<Class> classes = new ArrayList<Class>();
        
        if (annotatedClasses != null) {
            for (Class annotatedClass : annotatedClasses) {
                classes.add(annotatedClass);
            }
        }
        
        classes.addAll(hippoBaseBeanClasses);
        
        List<Class> uniqueClasses = SetUniqueList.decorate(classes);
        
        Mapper mapper = null;
        
        if (fallBackJcrNodeTypes == null) {
            mapper = new HstAnnotationMapperImpl(uniqueClasses);
        } else {
            mapper = new HstAnnotationMapperImpl(uniqueClasses, fallBackJcrNodeTypes);
        }
        
        return createObjectContentManager(session, mapper);
    }
    
    public static ObjectContentManager createObjectContentManager(Session session, InputStream [] xmlMappingFiles) {
        Mapper mapper = new DigesterMapperImpl(xmlMappingFiles);
        return createObjectContentManager(session, mapper);
    }
    
    public static ObjectContentManager createObjectContentManager(Session session, String [] xmlMappingFiles) {
        Mapper mapper = new DigesterMapperImpl(xmlMappingFiles);
        return createObjectContentManager(session, mapper);
    }
    
    public static ObjectContentManager createObjectContentManager(Session session, Mapper mapper) {
        return createObjectContentManager(session, mapper, new RequestObjectCacheImpl());
    }
    
    public static ObjectContentManager createObjectContentManager(Session session, Mapper mapper, ObjectCache requestObjectCache) {
        try {
            DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
            Map atomicTypeConverters = converterProvider.getAtomicTypeConverters();
            QueryManager queryManager = new HstQueryManagerImpl(mapper, atomicTypeConverters, session.getValueFactory());
            ProxyManager proxyManager = new ProxyManagerImpl();
            
            if (requestObjectCache == null) {
                requestObjectCache = new NOOPObjectCache();
            }
            
            ObjectConverter objectConverter = new HstObjectConverterImpl(mapper, converterProvider, proxyManager, requestObjectCache);
            return new ObjectContentManagerImpl(mapper, objectConverter, queryManager, requestObjectCache, session);
        } catch (UnsupportedRepositoryOperationException e) {
            throw new RuntimeException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
    
}
