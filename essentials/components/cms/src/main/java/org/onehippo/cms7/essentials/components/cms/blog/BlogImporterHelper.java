/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.cms.blog;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class BlogImporterHelper {
    public static final Logger log = LoggerFactory.getLogger(BlogImporterHelper.class);

    private static final String SCHEDULER_NAME = "BlogListenerModule";
    private static final String SCHEDULER_GROUP_NAME = "essentials";
    private static final String JOB_NAME = SCHEDULER_NAME + "Job";

    private BlogImporterHelper() {}

    public static String getProjectNamespace(final Node moduleConfigNode) {
        try {
            return new BlogImporterConfiguration(moduleConfigNode).getProjectNamespace();
        } catch (RepositoryException ex) {
            log.error("Failure loading the blog importer configuration.", ex);
        }
        return null;
    }

    public static void rescheduleJob(final Node moduleConfigNode) throws RepositoryException {
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        if (scheduler.checkExists(JOB_NAME, SCHEDULER_GROUP_NAME)) {
            scheduler.deleteJob(JOB_NAME, SCHEDULER_GROUP_NAME);
        }

        final BlogImporterConfiguration config = new BlogImporterConfiguration(moduleConfigNode);
        if (!config.isActive()
                || Strings.isNullOrEmpty(config.getCronExpression())
                || Strings.isNullOrEmpty(config.getProjectNamespace())
                || config.getUrls().length == 0) {
            // No need to reschedule, were done.
            return;
        }

        final RepositoryJobInfo jobInfo = createJobInfo(JOB_NAME, config);
        final RepositoryJobTrigger trigger = new RepositoryJobCronTrigger(SCHEDULER_NAME + "Trigger", config.getCronExpression());
        scheduler.scheduleJob(jobInfo, trigger);

        if (config.isRunNow()) {
            final String name = SCHEDULER_NAME + "TriggerNow";
            final RepositoryJobInfo nowJobInfo = createJobInfo(name, config);
            final RepositoryJobTrigger nowTrigger = new RepositoryJobCronTrigger(name, config.getCronExpression());

            scheduler.scheduleJob(nowJobInfo, nowTrigger);
            scheduler.executeJob(name, nowJobInfo.getGroup());
            scheduler.deleteJob(name, nowJobInfo.getGroup());
        }
    }

    private static RepositoryJobInfo createJobInfo(final String jobName, final BlogImporterConfiguration config) {
        final RepositoryJobInfo jobInfo = new RepositoryJobInfo(jobName, SCHEDULER_GROUP_NAME, BlogImporterJob.class);
        jobInfo.setAttribute(BlogImporterJob.PROJECT_NAMESPACE, config.getProjectNamespace());
        jobInfo.setAttribute(BlogImporterJob.AUTHORS_BASE_PATH, config.getAuthorsBasePath());
        jobInfo.setAttribute(BlogImporterJob.BLOGS_BASE_PATH, config.getBlogBasePath());
        jobInfo.setAttribute(BlogImporterJob.AUTHORS, Joiner.on(BlogImporterJob.SPLITTER).join(config.getAuthors()));
        jobInfo.setAttribute(BlogImporterJob.URLS, Joiner.on(BlogImporterJob.SPLITTER).join(config.getUrls()));
        return jobInfo;
    }
}
