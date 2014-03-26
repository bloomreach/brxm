/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConcurrentChannelManagerAndHstManagerLoadTest extends AbstractTestConfigurations {


    private final static String TEST_PROP =  "ConcurrentChannelManagerAndHstManagerLoadTest.testProp";

    private HstManager hstManager;
	private ChannelManager channelManager;
	private List<Session> sessionList = null;

	private enum Job {
		GET_VIRTUALHOSTS_SYNC,
		GET_VIRTUALHOSTS_ASYNC,
		MODIFY_CHANNEL,
		MODIFY_HSTHOSTS
	}

	final Job[] enumJobs = Job.values();

	@Override
    @Before
	public void setUp() throws Exception {
		super.setUp();
		this.hstManager = getComponent(HstManager.class.getName());
		((HstManagerImpl)hstManager).setStaleConfigurationSupported(true);
		this.channelManager = getComponent(ChannelManager.class.getName());
	}

	@Test
	public void testHstManagerASynchronousFirstLoad() throws Exception {
		// even though async, if the model is not built before, the async built is sync
		final VirtualHosts asyncVirtualHosts = hstManager.getVirtualHosts(true);
		assertNotNull(asyncVirtualHosts);
	}

	@Test
	public void testHstManagerASynchronousFirstLoadAfterEvent() throws Exception {
		// even though async, if the model is not built before, the async built is sync
		final VirtualHosts asyncVirtualHosts = hstManager.getVirtualHosts(true);
		assertNotNull(asyncVirtualHosts);
	}

	@Test
	public void testHstManagerConcurrentSynchronousLoad() throws Exception {
		try {
			Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(100);
			for (int i = 0; i < 100; i++) {
				jobs.add(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return hstManager.getVirtualHosts();
					}
				});
			}
			final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
			VirtualHosts current = null;
			for (Future<Object> future : futures) {
				if (!future.isDone()) {
					fail("unfinished jobs");
				}
				VirtualHosts next = (VirtualHosts)future.get();
				if (current == null) {
					current = next;
					continue;
				}
				assertTrue(current == next);
			}
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			fail(e.toString());
		}
	}

	@Test
	public void testConcurrentHstManagerSynchronousAndAsynchronousLoad() throws Exception {
		try {
			Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(100);
			final Random random = new Random();
			for (int i = 0; i < 100; i++) {
				final boolean allowStale;
				Job randomJob = enumJobs[random.nextInt(2)];
				switch (randomJob) {
					case GET_VIRTUALHOSTS_SYNC:
						allowStale = false;
						break;
					case GET_VIRTUALHOSTS_ASYNC:
						allowStale = true;
						break;
					default :
						allowStale = false;
				}
				jobs.add(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return hstManager.getVirtualHosts(allowStale);
					}
				});
			}
			final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
			VirtualHosts current = null;
			for (Future<Object> future : futures) {
				if (!future.isDone()) {
					fail("unfinished jobs");
				}
				VirtualHosts next = (VirtualHosts)future.get();
				if (current == null) {
					current = next;
					continue;
				}
				assertTrue(current == next);
			}
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			fail(e.toString());
		}
	}

	@Test
	public void testHstManagerLoadingAfterConfigChanges() throws Exception {
		populateSessions(2);
		Node mountNode = getSession1().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
		int counter = 0;
		mountNode.setProperty(TEST_PROP, "testVal"+counter);
		mountNode.getSession().save();
		// load the model first ones to make sure async model is really async
		hstManager.getVirtualHosts();
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());

		try {
			final int synchronousJobCount = 1000;
			for (int i = 0; i < synchronousJobCount; i++) {
                String prevVal = "testVal"+counter;
				counter++;
				String nextVal = "testVal"+counter;
				mountNode.setProperty(TEST_PROP, nextVal);
				// Make sure to directly invalidate and do not wait for jcr event which is async and might arrive too late
                String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(mountNode.getSession(), mountNode.getSession().getNode("/hst:hst"), false);
                mountNode.getSession().save();
                invalidator.eventPaths(pathsToBeChanged);

				// ASYNC load
				final VirtualHosts asyncHosts = hstManager.getVirtualHosts(true);
				String testPropOfAsyncLoadedHosts = asyncHosts.matchMount("localhost", "/site", "").getMount().getProperty(TEST_PROP);
				// SYNC load
				final VirtualHosts syncHosts = hstManager.getVirtualHosts();
				String testPropOfSyncLoadedHosts = syncHosts.matchMount("localhost", "/site", "").getMount().getProperty(TEST_PROP);

				assertTrue("Expectation failed in run '"+i+"' : Expected value was '"+nextVal+"' but found" +
                        " value was '"+testPropOfSyncLoadedHosts+"'. ",testPropOfSyncLoadedHosts.equals(nextVal));

				// because the jobs above are done in a synchronous loop (single threaded) and AFTER every ASYNC
				// there is a SYNC load, we expect that the ASYNC model in this case is always one instance behind :
				// This would mean that testPropOfAsyncLoadedHosts would always be equal to 'prevVal'
				// HOWEVER, there is a very small (but it happens on certain machines, most likely with not so many CPU's)
				// chance that hstManager.getVirtualHosts(true) returns the UP2DATE model because the background thread that
				// is started to load the async model gets all the CPU for some time resulting in replacing the global
				// virtualhosts volatile instance that the main thread fetches. Hence, there is a small chance that
				// testPropOfAsyncLoadedHosts = nextVal

				if (asyncHosts == syncHosts) {
					// can happen in race condition explained above
                    assertTrue("Expectation failed in run '"+i+"'", testPropOfAsyncLoadedHosts.equals(nextVal));
				} else {
					  assertTrue("The async model should be one version behind but this was not the case. the async model has a version with " +
						"value '"+testPropOfAsyncLoadedHosts+"' and the expected value was '"+prevVal+"'",
						testPropOfAsyncLoadedHosts.equals(prevVal));
				}
			}

		} finally {
			mountNode.getProperty(TEST_PROP).remove();
			mountNode.getSession().save();
			logoutSessions(sessionList);
		}
	}

	@Test
	public void testConcurrentSyncAndAsyncHstManagerAndChannelManagerWithConfigChanges() throws Exception {
		populateSessions(2);
		Node mountNode = getSession1().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
		final AtomicInteger counter = new AtomicInteger(0);
		mountNode.setProperty(TEST_PROP, "testVal"+counter);
		mountNode.getSession().save();

		final Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels();
		assertTrue(channels.size() == 1);
		final Channel existingChannel = channels.values().iterator().next();
        final EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        try {
			final int jobCount = 1000;
			Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(jobCount);
			final Random random = new Random();
			final AtomicLong channelModCount = new AtomicLong(0);
			final Object MUTEX = new Object();
			final Object MUTEX2 = new Object();
			for (int i = 0; i < jobCount; i++) {
				int rand = random.nextInt(4);
				Job randomJob = enumJobs[rand];
				switch (randomJob) {
					case GET_VIRTUALHOSTS_SYNC:
						jobs.add(new Callable<Object>() {
							@Override
							public Boolean call() throws Exception {
								hstManager.getVirtualHosts();
								return Boolean.TRUE;
							}
						});
						break;
					case GET_VIRTUALHOSTS_ASYNC:
						jobs.add(new Callable<Object>() {
							@Override
							public Boolean call() throws Exception {
								hstManager.getVirtualHosts(true);
								return Boolean.TRUE;
							}
						});
						break;
					case MODIFY_CHANNEL:
						jobs.add(new Callable<Object>() {
							@Override
							public Object call() throws Exception {
								synchronized (MUTEX2) {
									Long newTestValue = channelModCount.incrementAndGet();
									// need to set the channel info to be able to store properties
									existingChannel.setChannelInfoClassName(ConcurrentChannelManagerAndHstManagerLoadTest.class.getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
									existingChannel.getProperties().put(TEST_PROP, newTestValue);

                                    MockHstRequestContext ctx = new MockHstRequestContext();
                                    ctx.setSession(getSession2());
                                    final VirtualHost dummyHost = hstManager.getVirtualHosts().getMountsByHostGroup("dev-localhost").get(0).getVirtualHost();
                                    ctx.setVirtualHost(dummyHost);
                                    ModifiableRequestContextProvider.set(ctx);
                                    channelManager.save(existingChannel);
                                    ModifiableRequestContextProvider.clear();
									// get channel must always reflect LATEST version. Since this MODIFY_CHANNEL is
									// called concurrently, we can only guarantee that the loaded value for TEST_PROP
									// is AT LEAST AS big as newTestValue

									final Map<String, Channel> loadedChannels = hstManager.getVirtualHosts().getChannels();

									JobResultWrapperModifyChannel result = new JobResultWrapperModifyChannel();
									result.loadedChannels = loadedChannels;
									result.expectedNewTestValue = newTestValue;
									return result;
								}
							}
						});
						break;
					case MODIFY_HSTHOSTS:
						jobs.add(new Callable<Object>() {
							@Override
							public Object call() throws Exception {
								synchronized (MUTEX) {
									String nextVal = "testVal" + counter.incrementAndGet();
									Node node = getSession1().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
									node.setProperty(TEST_PROP, nextVal);
									// Make sure to directly invalidate and do not wait for jcr event which is async and might arrive too late
                                    String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(node.getSession(), node.getSession().getNode("/hst:hst"), false);
                                    node.getSession().save();
                                    invalidator.eventPaths(pathsToBeChanged);

									JobResultWrapperModifyMount result = new JobResultWrapperModifyMount();
									result.testPropAfterChange = nextVal;
									// ASYNC load
									final VirtualHosts asyncHosts = hstManager.getVirtualHosts(true);
									result.testPropOfAsyncLoadedHosts = asyncHosts.matchMount("localhost", "/site", "").getMount().getProperty(TEST_PROP);
									// SYNC load
									final VirtualHosts syncHosts = hstManager.getVirtualHosts();
									result.testPropOfSyncLoadedHosts = syncHosts.matchMount("localhost","/site", "").getMount().getProperty(TEST_PROP);

									return result;
								}
							}
						});
					default:
						break;
				}
			}
			final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);

			for (Future<Object> future : futures) {
				if (!future.isDone()) {
					fail("unfinished jobs");
				}
				Object o = future.get();
				if (o instanceof Boolean) {
					// nothing to check
				} else if (o instanceof JobResultWrapperModifyChannel) {
					JobResultWrapperModifyChannel jobResultWrapperModifyChannel = (JobResultWrapperModifyChannel)o;
					// since the model is loaded concurrently by multiple threads, the only thing we can guarantee is that
					// the ACTUAL testValue on the channel is AT LEAST as big as the expectedNewTestValue (since channel
					// manager is synchronously loaded the channel will have a testValue that is at least as big as the
					// testValue that was saved just before the channel manager got reloaded)
					Long valueFromChannel = (Long)jobResultWrapperModifyChannel.loadedChannels.values().iterator().next().getProperties().get(TEST_PROP);
					assertTrue(valueFromChannel.longValue() >= jobResultWrapperModifyChannel.expectedNewTestValue.longValue());
				} else if (o instanceof JobResultWrapperModifyMount) {
					JobResultWrapperModifyMount job = (JobResultWrapperModifyMount)o;
					// the sync model should always have the changed prop directly
					assertTrue(job.testPropOfSyncLoadedHosts.equals(job.testPropAfterChange));
					// the async model is hard to predict which version it gets as it is loaded concurrently by many different
					// threads. We can at least guarantee that it should not be null
					assertNotNull(job.testPropOfAsyncLoadedHosts);
				}
			}

		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			fail(e.toString());
		} finally {
			existingChannel.getProperties().remove(TEST_PROP);
            MockHstRequestContext ctx = new MockHstRequestContext();
            ctx.setSession(getSession1());
            final VirtualHost dummyHost = hstManager.getVirtualHosts().getMountsByHostGroup("dev-localhost").get(0).getVirtualHost();
            ctx.setVirtualHost(dummyHost);
            ModifiableRequestContextProvider.set(ctx);
            channelManager.save(existingChannel);
            ModifiableRequestContextProvider.clear();
			mountNode.getProperty(TEST_PROP).remove();
			mountNode.getSession().save();
			logoutSessions(sessionList);
		}
	}

	protected Collection<Future<Object>> executeAllJobs(final Collection<Callable<Object>> jobs, final int threads) throws InterruptedException {
		final ExecutorService executorService = Executors.newFixedThreadPool(threads);
		final List<Future<Object>> futures = executorService.invokeAll(jobs);
		executorService.shutdown();
		if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
			executorService.shutdownNow(); // Cancel currently executing tasks
			// Wait a while for tasks to respond to being cancelled
			if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
				fail("Pool did not terminate");
			}
		}
		return futures;
	}

	protected Session getSession1() {
		populateSessions(2);
		return sessionList.get(0);
	}

	protected Session getSession2() {
		populateSessions(2);
		return sessionList.get(1);
	}

	protected void populateSessions(int number) {
		if (sessionList != null) {
			return;
		}
		sessionList = new ArrayList<>(number);
		for (int i = 0; i < number; i++) {
			try {
				Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
				Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
				sessionList.add(session);
			} catch (LoginException e) {
				e.printStackTrace();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
	}

	protected void logoutSessions(List<Session> sessionList) {
		if (sessionList == null) {
			return;
		}
		for (Session session : sessionList) {
			session.logout();
		}
	}
	private class JobResultWrapperModifyMount {
		private String testPropAfterChange;
		private String testPropOfAsyncLoadedHosts;
		private String testPropOfSyncLoadedHosts;
	}

	private class JobResultWrapperModifyChannel {
		private Map<String, Channel> loadedChannels;
		private Long expectedNewTestValue;
	}

	public static interface TestChannelInfo extends ChannelInfo {

		@Parameter(name = TEST_PROP, defaultValue = "0")
		Long getTitle();

	}

}
