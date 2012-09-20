/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.repository.quartz.workflow;

import java.util.Date;

import org.hippoecm.repository.api.CronExpression;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModuleFactory;
import org.hippoecm.repository.ext.WorkflowManagerModule;
import org.hippoecm.repository.ext.WorkflowManagerRegister;

public class SchedulerWorkflowModule implements WorkflowManagerModule {

    @Override
    public void register(WorkflowManagerRegister register) {
        register.bind(Date.class, new WorkflowInvocationHandlerModuleFactory<Date>() {
            @Override
            public WorkflowInvocationHandlerModule createInvocationHandler(Date date) {
                return new DateSchedulerInvocationModule(date);
            }
        });
        register.bind(CronExpression.class, new WorkflowInvocationHandlerModuleFactory<CronExpression>() {
            @Override
            public WorkflowInvocationHandlerModule createInvocationHandler(CronExpression cronExpression) {
                return new CronSchedulerInvocationModule(cronExpression.toString());
            }
        });
    }
}
