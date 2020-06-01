/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

/**
 * Instances of a CronExpresssion indicate a re-occurring time specification.
 * This can be used to specify that a certain work-flow call needs to be performed
 * on a repeating interval, such as daily at 1am.  Using the call to {@link
 * org.hippoecm.repository.api.WorkflowManager#getContextWorkflowManager(java.lang.Object)}
 * you can obtain a work-flow execution context that will apply for your indicated
 * interval.
 * The expressions that can be used to create this event are dependent on the
 * actual module that will pick up the events, which is a pluggable module in
 * the repository.  In practice many installations will use Quartz, and the
 * cron specification will be the same as org.quartz.CronExpression.
 * Note however that constructing a CronExpression will not throw a exception
 * when the string cannot be parsed.  Instead the call to a work-flow will fail.
 * @deprecated use {@link org.onehippo.repository.scheduling.RepositoryScheduler}.
 */
@Deprecated
public class CronExpression {
    String cron;

    /**
     * Creates a repeating time specification.
     * @param cron An implementation dependent time specification.
     */
    public CronExpression(String cron) {
        this.cron = cron;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return cron;
    }
}
