/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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


/**
 * Hierarchical Diagnostic Context
 */
public class HDC {

    public static final Task NOOP_TASK = new NOOPTaskImpl();

    private static ThreadLocal<Task> tlRootTask = new ThreadLocal<Task>();
    private static ThreadLocal<Task> tlCurrentTask = new ThreadLocal<Task>();

    private HDC() {
    }

    public static Task start(String name) {
        Task rootTask = tlRootTask.get();

        if (rootTask != null) {
            throw new IllegalStateException("The root task was already started.");
        }

        rootTask = new DefaultTaskImpl(null, name);
        tlRootTask.set(rootTask);
        return rootTask;
    }

    public static boolean isStarted() {
        return (tlRootTask.get() != null);
    }

    public static Task getRootTask() {
        Task rootTask = tlRootTask.get();
        return (rootTask != null ? rootTask : NOOP_TASK);
    }

    public static Task getCurrentTask() {
        Task current = tlCurrentTask.get();

        if (current != null) {
            return current;
        }

        return getRootTask();
    }

    public static void setCurrentTask(Task currentTask) {
        tlCurrentTask.set(currentTask);
    }

    public static void cleanUp() {
        tlCurrentTask.remove();
        tlRootTask.remove();
    }
}
