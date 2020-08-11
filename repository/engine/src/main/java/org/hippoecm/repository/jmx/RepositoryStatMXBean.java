/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jmx;

public interface RepositoryStatMXBean {

    double getDatabaseBundleGetDurationMillisAverageLastMinute();

    double getDatabaseBundleGetDurationMillisAverageLastHour();

    double getDatabaseBundleGetDurationMillisAverageLastWeek();

    double getDatabaseBundleGetAverageLastMinutePerSecond();

    double getDatabaseBundleGetAverageLastHourPerSecond();

    double getDatabaseBundleGetAverageLastWeekPerSecond();

    double getDatabaseBundleWriteDurationMillisAverageLastMinute();

    double getDatabaseBundleWriteDurationMillisAverageLastHour();

    double getDatabaseBundleWriteDurationMillisAverageLastWeek();

    double getDatabaseBundleWriteAverageLastMinutePerSecond();

    double getDatabaseBundleWriteAverageLastHourPerSecond();

    double getDatabaseBundleWriteAverageLastWeekPerSecond();

    double getBundleCacheMissLastMinutePerSecond();

    double getBundleCacheMissLastHourPerSecond();

    double getBundleCacheMissLastWeekPerSecond();

    double getBundleReadLastMinutePerSecond();

    double getBundleReadLastHourPerSecond();

    double getBundleReadLastWeekPerSecond();

    double getRepositoryReadsLastMinutePerSecond();

    double getRepositoryReadsLastHourPerSecond();

    double getRepositoryReadsLastWeekPerSecond();

    double getRepositoryReadDurationMillisAverageLastMinute();

    double getRepositoryReadDurationMillisAverageLastHour();

    double getRepositoryReadDurationMillisAverageLastWeek();

    double getRepositoryWritesLastMinutePerSecond();

    double getRepositoryWritesLastHourPerSecond();

    double getRepositoryWritesLastWeekPerSecond();

    double getRepositoryWriteDurationMillisAverageLastMinute();

    double getRepositoryWriteDurationMillisAverageLastHour();

    double getRepositoryWriteDurationMillisAverageLastWeek();

    double getRepositoryLoginsLastMinutePerSecond();

    double getRepositoryLoginsLastHourPerSecond();

    double getRepositoryLoginsLastWeekPerSecond();

    double getRepositorySessionsLastMinuteAverage();

    double getRepositorySessionsLastHourAverage();

    double getRepositorySessionsLastWeekAverage();

    double getQueriesLastMinutePerSecond();

    double getQueriesLastHourPerSecond();

    double getQueriesLastWeekPerSecond();

    double getQueryDurationMillisAverageLastMinute();

    double getQueryDurationMillisAverageLastHour();

    double getQueryDurationMillisAverageLastWeek();

    double getBundleCacheSizeMbLastHourAverage();

    double getBundleCacheHitLastMinutePerSecond();

    double getBundleCacheHitLastHourPerSecond();

    double getBundleCacheHitLastWeekPerSecond();
}
