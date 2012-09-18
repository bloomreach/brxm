/*
 *  Copyright 2012 Hippo.
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

import java.util.Collections;
import java.util.Set;

import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.spi.JobStore;


public abstract class AbstractJobStore implements JobStore {

    @Override
    public void setInstanceId(String id) {
    }

    @Override
    public void setInstanceName(String name) {
    }

    @Override
    public void schedulerStarted() throws SchedulerException {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

    @Override
    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 0;
    }

    @Override
    public void storeJob(SchedulingContext ctxt, JobDetail newJob, boolean replaceExisting) throws JobPersistenceException {
    }

    @Override
    public boolean removeJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        return false;
    }

    @Override
    public void storeTrigger(SchedulingContext ctxt, Trigger newTrigger, boolean replaceExisting) throws JobPersistenceException {
    }

    @Override
    public boolean removeTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        return false;
    }

    @Override
    public boolean replaceTrigger(SchedulingContext ctxt, String triggerName, String groupName, Trigger newTrigger) throws JobPersistenceException {
        return false;
    }

    @Override
    public Trigger retrieveTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        return null;
    }

    @Override
    public void storeCalendar(SchedulingContext ctxt, String name, org.quartz.Calendar calendar, boolean replaceExisting, boolean updateTriggers) throws JobPersistenceException {
    }

    @Override
    public boolean removeCalendar(SchedulingContext ctxt, String calName) throws JobPersistenceException {
        return false;
    }

    @Override
    public org.quartz.Calendar retrieveCalendar(SchedulingContext ctxt, String calName) throws JobPersistenceException {
        return null;
    }

    @Override
    public int getNumberOfJobs(SchedulingContext ctxt) throws JobPersistenceException {
        return 0;
    }

    @Override
    public int getNumberOfTriggers(SchedulingContext ctxt) throws JobPersistenceException {
        return 0;
    }

    @Override
    public int getNumberOfCalendars(SchedulingContext ctxt) throws JobPersistenceException {
        return 0;
    }

    @Override
    public String[] getJobNames(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        return new String[0];
    }

    @Override
    public String[] getTriggerNames(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        return new String[0];
    }

    @Override
    public String[] getJobGroupNames(SchedulingContext ctxt) throws JobPersistenceException {
        return new String[0];
    }

    @Override
    public String[] getTriggerGroupNames(SchedulingContext ctxt) throws JobPersistenceException {
        return new String[0];
    }

    @Override
    public String[] getCalendarNames(SchedulingContext ctxt) throws JobPersistenceException {
        return new String[0];
    }

    @Override
    public Trigger[] getTriggersForJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        return new Trigger[0];
    }

    @Override
    public int getTriggerState(SchedulingContext ctxt, String triggerName, String triggerGroup) throws JobPersistenceException {
        return 0;
    }

    @Override
    public void pauseTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
    }

    @Override
    public void pauseTriggerGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
    }

    @Override
    public void pauseJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
    }

    @Override
    public void pauseJobGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
    }

    @Override
    public void resumeTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
    }

    @Override
    public void resumeTriggerGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
    }

    @Override
    public Set getPausedTriggerGroups(SchedulingContext ctxt) throws JobPersistenceException {
        return Collections.emptySet();
    }

    @Override
    public void resumeJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
    }

    @Override
    public void resumeJobGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
    }

    @Override
    public void pauseAll(SchedulingContext ctxt) throws JobPersistenceException {
    }

    @Override
    public void resumeAll(SchedulingContext ctxt) throws JobPersistenceException {
    }

}
