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
package org.hippoecm.repository.quartz;

public final class HippoSchedJcrConstants {

    private HippoSchedJcrConstants() {}

    public static final String HIPPOSCHED_DATA = "hipposched:data";
    public static final String HIPPOSCHED_NEXTFIRETIME = "hipposched:nextFireTime";
    public static final String HIPPOSCHED_STARTTIME = "hipposched:startTime";
    public static final String HIPPOSCHED_ENDTIME = "hipposched:endTime";
    public static final String HIPPOSCHED_REPEATCOUNT = "hipposched:repeatCount";
    public static final String HIPPOSCHED_REPEATINTERVAL = "hipposched:repeatInterval";
    public static final String HIPPOSCHED_CRONEXPRESSION = "hipposched:cronExpression";
    public static final String HIPPOSCHED_TRIGGERS = "hipposched:triggers";
    public static final String HIPPOSCHED_SIMPLE_TRIGGER = "hipposched:simpletrigger";
    public static final String HIPPOSCHED_CRON_TRIGGER = "hipposched:crontrigger";
    public static final String HIPPOSCHED_REPOSITORY_JOB = "hipposched:repositoryjob";
    public static final String HIPPOSCHED_WORKFLOW_JOB = "hipposched:workflowjob";
    public static final String HIPPOSCHED_JOBGROUP = "hipposched:jobgroup";
    public static final String HIPPOSCHED_ENABLED = "hipposched:enabled";

    // hipposched:repositoryjob properties
    public static final String HIPPOSCHED_REPOSITORY_JOB_CLASS = "hipposched:repositoryJobClass";
    public static final String HIPPOSCHED_ATTRIBUTE_NAMES = "hipposched:attributeNames";
    public static final String HIPPOSCHED_ATTRIBUTE_VALUES = "hipposched:attributeValues";

    // hipposched:workflowjob properties
    public static final String HIPPOSCHED_METHOD_NAME = "hipposched:methodName";
    public static final String HIPPOSCHED_CATEGORY = "hipposched:category";
    public static final String HIPPOSCHED_WORKFLOW_NAME = "hipposched:workflowName";
    public static final String HIPPOSCHED_SUBJECT_ID = "hipposched:subjectId";
    public static final String HIPPOSCHED_PARAMETER_TYPES = "hipposched:parameterTypes";
    public static final String HIPPOSCHED_ARGUMENTS = "hipposched:arguments";
    public static final String HIPPOSCHED_INTERACTION_ID = "hipposched:interactionId";
    public static final String HIPPOSCHED_INTERACTION = "hipposched:interaction";

}
