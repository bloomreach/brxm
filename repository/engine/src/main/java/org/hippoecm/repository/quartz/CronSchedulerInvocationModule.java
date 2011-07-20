/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.quartz;

import java.text.ParseException;
import java.util.Date;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

public class CronSchedulerInvocationModule extends AbstractSchedulerInvocationModule {

    private String cronExpression;

    public CronSchedulerInvocationModule(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public CronSchedulerInvocationModule(CronExpression cronExpression) {
        this.cronExpression = cronExpression.getCronExpression();
    }

    protected Trigger createTrigger(String name) {
        CronTrigger trigger = null;
        try {
            trigger = new CronTrigger(name, null, cronExpression);
        } catch (ParseException impossible) {
            throw new IllegalStateException("Failed to create cron trigger with cron expression '" + cronExpression + "'", impossible);
        }
        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        return trigger;
    }

}
