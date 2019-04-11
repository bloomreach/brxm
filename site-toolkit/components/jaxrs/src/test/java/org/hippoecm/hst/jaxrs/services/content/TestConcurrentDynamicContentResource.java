package org.hippoecm.hst.jaxrs.services.content;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.observation.EventListener;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestConcurrentDynamicContentResource extends AbstractTestContentResource {

    private static final String CONTENT_TYPE_VERSION_KEY = "ContentTypeVersion";

    class MockedRequestExecutor implements Callable<MockHttpServletResponse> {

        final EventListener ctEventListener;
        final Cache<ContentTypes, ObjectConverter> instanceCache;
        final ContentTypeService contentTypeService;

        public MockedRequestExecutor(final EventListener ctEventListener, final Cache<ContentTypes, ObjectConverter> instanceCache,
                                     final ContentTypeService contentTypeService) {
            this.ctEventListener = ctEventListener;
            this.instanceCache = instanceCache;
            this.contentTypeService = contentTypeService;
        }

        @Override
        public MockHttpServletResponse call() throws Exception {

            //Sleep up to 1 second before executing the request
            final long millis = (long) (Math.random() * 1000);
            Thread.sleep(millis);

            MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
            request.setProtocol("HTTP/1.1");
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8085);
            request.setMethod("GET");
            request.setRequestURI("/site/preview/services/Products/HippoCMS");
            request.setContextPath("/site");
            request.setServletPath("/preview/services");
            request.setPathInfo("/Products/HippoCMS");
            request.setContent(new byte[0]);

            final MockHttpServletResponse response = new MockHttpServletResponse();
            invokeJaxrsPipelineAsAdmin(request, response);
            final HstMutableRequestContext mutableRequestContext = (HstMutableRequestContext) request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            final long versionOfCurrentRequest = mutableRequestContext.getContentTypes().version();
            response.setHeader(CONTENT_TYPE_VERSION_KEY, String.valueOf(versionOfCurrentRequest));
            return response;
        }
    }

    @Test
    public void object_converter_concurrecy_test() throws Exception {

        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        final ContentTypeService contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
        final HippoContentTypeService unproxiedContentTypeService = unproxyService(contentTypeService);

        //Get reference to EventListener of ContentTypeService to invalidate ContentTypesCache
        final EventListener ctEventListener = on(unproxiedContentTypeService).field("contentTypesChangeListener").get();

        final ObjectConverter objectConverter = getComponentManager().getComponent(ObjectConverter.class.getName());

        //Disable cache invalidation
        final Cache<ContentTypes, ObjectConverter> instanceCache = CacheBuilder.newBuilder().build();
        on(objectConverter).set("instanceCache", instanceCache);

        final Collection<MockedRequestExecutor> jobs = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            jobs.add(new MockedRequestExecutor(ctEventListener, instanceCache, contentTypeService));
        }

        //Periodically emulate ContentTypes change by resetting ContentTypesCache
        final Thread thread = new Thread(() -> {
            for (int i = 0; i < 60; i++) {
                try {
                    final long millis = 50L;
                    Thread.sleep(millis);
                    ctEventListener.onEvent(null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        final List<Future<MockHttpServletResponse>> futures = executorService.
                invokeAll(jobs);

        executorService.shutdown(); // Disable new tasks from being submitted

        // Wait a while for existing tasks to terminate
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
            executorService.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                fail("Pool did not terminate");
            }
        }

        Set<Long> versionSet = new HashSet<>();
        for (Future<MockHttpServletResponse> future : futures) {
            if (!future.isDone()) {
                fail("unfinished requests");
            }
            MockHttpServletResponse next = future.get();
            final long contentTypeVersion = Long.parseLong((String) next.getHeaderValue(CONTENT_TYPE_VERSION_KEY));
            versionSet.add(contentTypeVersion);
            assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(next.getStatus()).getFamily());
        }

        Assert.assertEquals(versionSet.size(), instanceCache.size());
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
