/**
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.diagnosis;

import org.slf4j.LoggerFactory;

/**
 * Hierarchical Diagnostic Context.
 * <P>This provides static methods to start, get and clean up diagnostic tasks.</P>
 * <P>This also allows to customize <code>HDC</code> by setting a system property,
 * <code>org.hippoecm.hst.diagnosis</code> to a specific implementation class name.
 * A custom <code>HDC</code> implementation may override the instance methods (<code>#doXXX</code>)
 * to extend the functionality.</code></P>
 * <P>
 * For example, suppose you want to extend <code>HDC</code> to report the duration of each task
 * to an external performance measuring system. Then one example implementation could look like the following:
 * </P>
 * <PRE>
 * public class CustomExternalApmIntegratedHDC extends HDC {
 *
 *    // Suppose externalAPM is your external application performance monitoring system
 *    // at your hand to integrate with.
 *    private ExternalAPM externalAPM = ...;
 *
 *    public CustomExternalApmIntegratedHDC() {
 *        super();
 *    }
 *
 *    @Override
 *    protected Task doCreateTask(String name) {
 *        // Create a custom root task implementation to override Task#stop() method
 *        return new CustomTaskImpl(null, name) {
 *            @Override
 *            protected Task createSubtask(final Task parentTask, final String name) {
 *                return new CustomTaskImpl(parentTask, name);
 *            }
 *        };
 *    }
 *
 *    //Custom extension task implementation which records a metric on stop.
 *    private class CustomTaskImpl extends DefaultTaskImpl {
 *
 *        protected CustomTaskImpl(Task parentTask, String name) {
 *            super(parentTask, name);
 *        }
 *
 *        @Override
 *        public void stop() {
 *            super.stop();
 *            // let's suppose 
 *            externalAPM.recordMetric("Custom/site_" + getName() + "/duration", getDurationTimeMillis());
 *        }
 *
 *        @Override
 *        protected Task createSubtask(final Task parentTask, final String name) {
 *            // Create a child task with this class, too.
 *            return new CustomTaskImpl(parentTask, name);
 *        }
 *    }
 * }
 * </PRE>
 */
public class HDC {

    private static HDC singleton = new HDC();

    static {
        final String clazzName = System.getProperty(HDC.class.getName());

        if (clazzName != null && !"".equals(clazzName)) {
            try {
                Class<?> clazz = Class.forName(clazzName);
                singleton = (HDC) clazz.newInstance();
                LoggerFactory.getLogger(HDC.class).info("HDC singleton: {}", singleton);
            } catch (Exception e) {
                LoggerFactory.getLogger(HDC.class).info("Invalid custom HDC class: {}", clazzName, e);
            }
        }
    }

    public static final Task NOOP_TASK = new NOOPTaskImpl();

    private static ThreadLocal<Task> tlRootTask = new ThreadLocal<Task>();
    private static ThreadLocal<Task> tlCurrentTask = new ThreadLocal<Task>();

    /**
     * Start the root task with the name.
     * @param name root task name
     * @return root task instance
     */
    public static Task start(String name) {
        return singleton.doStart(name);
    }

    /**
     * Returns true if the root task was started.
     * @return true if the root task was started
     */
    public static boolean isStarted() {
        return singleton.doIsStarted();
    }

    /**
     * Returns the root task. Null otherwise.
     * @return the root task if exists. Null, otherwise.
     */
    public static Task getRootTask() {
        return singleton.doGetRootTask();
    }

    /**
     * Returns the task in the current thread context. Null if not available.
     * @return the task in the current thread context.  Null if not available.
     */
    public static Task getCurrentTask() {
        return singleton.doGetCurrentTask();
    }

    /**
     * Sets a task in the current thread context.
     * @param currentTask current task instance
     */
    public static void setCurrentTask(Task currentTask) {
        singleton.doSetCurrentTask(currentTask);
    }

    /**
     * Cleans up the HDC tasks and its context.
     */
    public static void cleanUp() {
        singleton.doCleanUp();
    }

    /**
     * Protected constructor which might be called by a child class.
     */
    protected HDC() {
    }

    /**
     * Internally starts the root task by the name.
     * @param name root task name
     * @return the root task instance
     */
    protected Task doStart(String name) {
        Task rootTask = tlRootTask.get();

        if (rootTask != null) {
            throw new IllegalStateException("The root task was already started.");
        }

        rootTask = doCreateTask(name);
        tlRootTask.set(rootTask);
        return rootTask;
    }

    /**
     * Internally create a task instance by the name.
     * This method is invoked by {@link #doStart(String)}, so a child class may override this method
     * if it needs to override the default task implementation, {@link DefaultTaskImpl}, for instance.
     * @param name task name
     * @return internally created task instance
     */
    protected Task doCreateTask(String name) {
        return new DefaultTaskImpl(null, name);
    }

    /**
     * Internally check whether or not the root task was started.
     * @return returns if the root task was started
     */
    protected boolean doIsStarted() {
        return (tlRootTask.get() != null);
    }

    /**
     * Internally returns the root task instance if available. Null otherwise.
     * @return the root task instance if available. Null otherwise
     */
    protected Task doGetRootTask() {
        Task rootTask = tlRootTask.get();
        return (rootTask != null ? rootTask : NOOP_TASK);
    }

    /**
     * Internally returns the task instance in the current thread context if available. Null otherwise.
     * @return the task instance in the current thread context if available. Null otherwise
     */
    protected Task doGetCurrentTask() {
        Task current = tlCurrentTask.get();

        if (current != null) {
            return current;
        }

        return getRootTask();
    }

    /**
     * Internally sets the task instance in the current thread context.
     * @param currentTask current task instance
     */
    protected void doSetCurrentTask(Task currentTask) {
        tlCurrentTask.set(currentTask);
    }

    /**
     * Cleans up the HDC tasks and its context.
     */
    protected void doCleanUp() {
        tlCurrentTask.remove();
        tlRootTask.remove();
    }
}
