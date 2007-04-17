/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.model.mock;

import java.util.Date;
import org.hippocms.repository.model.Scheduler;

public class MockScheduler implements Scheduler {
    public MockScheduler() {
        super();
    }

    public String schedule(Date time, Runnable task) {
        return "1";
    }

    public void cancel(String taskId) {
    }
}
