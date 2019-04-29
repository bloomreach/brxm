/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;

import org.apache.commons.lang3.time.StopWatch;
import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;

import com.google.common.cache.Cache;

import static org.awaitility.Awaitility.await;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class VersionedObjectConverterProxyTest extends AbstractBeanTestCase {

    /**
     * Verify that instance cache of ObjectConverters/ContentTypes gets invalidated with time
     * as soon as there are no strong references to ContentTypes anymore
     */
    @Test
    public void testCacheInvalidation() throws RepositoryException, ObjectBeanManagerException, NoSuchFieldException, IllegalAccessException, InterruptedException {

        final ContentTypeService contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
        final HippoContentTypeService unproxiedContentTypeService = unproxyService(contentTypeService);

        //Get reference to EventListener of ContentTypeService to invalidate ContentTypesCache
        final EventListener ctEventListener = on(unproxiedContentTypeService).field("contentTypesChangeListener").get();

        final ObjectConverter objectConverter = getObjectConverter();

        //Get a reference to ObjectConverter instance cache of VersionedObjectConverterProxy
        final Cache<ContentTypes, ObjectConverter> instanceCache = on(objectConverter).field("instanceCache").get();
        Assert.assertEquals(0, instanceCache.size());

        final Node node = session.getNode("/unittestcontent/documents/unittestproject/common/homepage");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ObjectConverter previousObjectConverter = null;

        // Create 1000 instances of ObjectConverter
        final int overallRequestCount = 1000;

        for (int i = 0; i < overallRequestCount; i++) {

            //Fetch an object operation triggers new ObjectConverter instance creation
            final Object object = objectConverter.getObject(node);

            //Validate that new object converter is not the same as previous one
            final ObjectConverter currentObjectConverter = on(objectConverter).call("getOrCreateObjectConverter").get();
            assertNotEquals(currentObjectConverter, previousObjectConverter);
            previousObjectConverter = currentObjectConverter;

            //Trigger invalidation of current ContentTypes so that on next getObject() invocation, new instance of ObjectConverter
            //including new cache entry will be created
            ctEventListener.onEvent(null);
        }
        stopWatch.stop();

        //Wait 5 seconds most until cache size will become less than overall OC instances
        await().atMost(5, TimeUnit.SECONDS).until(() -> instanceCache.size() < overallRequestCount);

        System.gc();

        assertTrue("Amount of cache entries should be less than in the beginning",
                instanceCache.size() < overallRequestCount);
    }

    private <T> T unproxyService(Object object) throws NoSuchFieldException, IllegalAccessException {

        final Object handler = Proxy.getInvocationHandler(object);
        final Class handlerClass = handler.getClass();
        final Field objField = handlerClass.getDeclaredField("arg$1");
        objField.setAccessible(true);
        final ProxiedServiceHolder serviceHolder = (ProxiedServiceHolder) objField.get(handler);

        return (T) serviceHolder.getServiceObject();
    }
}