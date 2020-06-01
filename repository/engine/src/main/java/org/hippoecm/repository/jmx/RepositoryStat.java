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

import org.apache.jackrabbit.api.stats.RepositoryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.jmx.RepositoryStat.Granularity.NANOSEC;
import static org.hippoecm.repository.jmx.RepositoryStat.Per.HOUR;
import static org.hippoecm.repository.jmx.RepositoryStat.Per.MINUTE;
import static org.hippoecm.repository.jmx.RepositoryStat.Per.SECOND;

/**
 * Note
 * <ul>
 * <li>RepositoryStatistics.Type.BUNDLE_CACHE_ACCESS_COUNTER is useless as it is complete unclear what it measures, at
 * least not cache hits / misses</li>
 * <li>RepositoryStatistics.Type.OBSERVATION_EVENT_COUNTER is not kept track of by Jackrabbit</li>
 * </ul>
 */
public class RepositoryStat implements RepositoryStatMXBean {
    private static final Logger log = LoggerFactory.getLogger(RepositoryStat.class);

    enum Per {
        SECOND, MINUTE, HOUR
    }

    enum Granularity {
        NANOSEC, MILLIS
    }

    private RepositoryStatistics repositoryStatistics;

    public RepositoryStat(final RepositoryStatistics repositoryStatistics) {
        this.repositoryStatistics = repositoryStatistics;
    }

    @Override
    public double getDatabaseBundleGetDurationMillisAverageLastMinute() {
        // note we cannot use RepositoryStatistics.Type.BUNDLE_CACHE_MISS_AVERAGE since we there do not know *how* much time was
        // spend PER node bundle get
        return averageDurationMillis(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_DURATION, RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, SECOND, NANOSEC);
    }

    @Override
    public double getDatabaseBundleGetDurationMillisAverageLastHour() {
        return averageDurationMillis(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_DURATION, RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, MINUTE, NANOSEC);
    }


    @Override
    public double getDatabaseBundleGetDurationMillisAverageLastWeek() {
        return averageDurationMillis(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_DURATION, RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, HOUR, NANOSEC);
    }

    @Override
    public double getDatabaseBundleGetAverageLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, SECOND);
    }

    @Override
    public double getDatabaseBundleGetAverageLastHourPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getDatabaseBundleGetAverageLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, HOUR) / 3600;
    }

    @Override
    public double getDatabaseBundleWriteDurationMillisAverageLastMinute() {
        // note we cannot use RepositoryStatistics.Type.BUNDLE_WRITE_AVERAGE since we there do not know *how* much time was
        // spend PER node bundle write
        return averageDurationMillis(RepositoryStatistics.Type.BUNDLE_WRITE_DURATION, RepositoryStatistics.Type.BUNDLE_WRITE_COUNTER, SECOND, NANOSEC);
    }

    @Override
    public double getDatabaseBundleWriteDurationMillisAverageLastHour() {
        return averageDurationMillis(RepositoryStatistics.Type.BUNDLE_WRITE_DURATION, RepositoryStatistics.Type.BUNDLE_WRITE_COUNTER, MINUTE, NANOSEC);
    }


    @Override
    public double getDatabaseBundleWriteDurationMillisAverageLastWeek() {
        return averageDurationMillis(RepositoryStatistics.Type.BUNDLE_WRITE_DURATION, RepositoryStatistics.Type.BUNDLE_WRITE_COUNTER, HOUR, NANOSEC);
    }

    @Override
    public double getDatabaseBundleWriteAverageLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_WRITE_COUNTER, SECOND);
    }

    @Override
    public double getDatabaseBundleWriteAverageLastHourPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_WRITE_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getDatabaseBundleWriteAverageLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_WRITE_COUNTER, HOUR) / 3600;
    }


    @Override
    public double getBundleCacheMissLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, SECOND);
    }

    @Override
    public double getBundleCacheMissLastHourPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getBundleCacheMissLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_CACHE_MISS_COUNTER, HOUR) / 3600;
    }

    @Override
    public double getBundleReadLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_READ_COUNTER, SECOND);
    }

    @Override
    public double getBundleReadLastHourPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_READ_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getBundleReadLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.BUNDLE_READ_COUNTER, HOUR) / 3600;
    }

    @Override
    public double getRepositoryReadsLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.SESSION_READ_COUNTER, SECOND);
    }

    @Override
    public double getRepositoryReadsLastHourPerSecond() {
        return average(RepositoryStatistics.Type.SESSION_READ_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getRepositoryReadsLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.SESSION_READ_COUNTER, HOUR) / 3600;
    }

    @Override
    public double getRepositoryReadDurationMillisAverageLastMinute() {
        return averageDurationMillis(RepositoryStatistics.Type.SESSION_READ_DURATION, RepositoryStatistics.Type.SESSION_READ_COUNTER, SECOND, NANOSEC);
    }

    @Override
    public double getRepositoryReadDurationMillisAverageLastHour() {
        return averageDurationMillis(RepositoryStatistics.Type.SESSION_READ_DURATION, RepositoryStatistics.Type.SESSION_READ_COUNTER, MINUTE, NANOSEC);
    }

    @Override
    public double getRepositoryReadDurationMillisAverageLastWeek() {
        return averageDurationMillis(RepositoryStatistics.Type.SESSION_READ_DURATION, RepositoryStatistics.Type.SESSION_READ_COUNTER, HOUR, NANOSEC);
    }

    @Override
    public double getRepositoryWritesLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.SESSION_WRITE_COUNTER, SECOND);
    }

    @Override
    public double getRepositoryWritesLastHourPerSecond() {
        return average(RepositoryStatistics.Type.SESSION_WRITE_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getRepositoryWritesLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.SESSION_WRITE_COUNTER, HOUR) / 3600;
    }

    @Override
    public double getRepositoryWriteDurationMillisAverageLastMinute() {
        return averageDurationMillis(RepositoryStatistics.Type.SESSION_WRITE_DURATION, RepositoryStatistics.Type.SESSION_WRITE_COUNTER, SECOND, NANOSEC);
    }

    @Override
    public double getRepositoryWriteDurationMillisAverageLastHour() {
        return averageDurationMillis(RepositoryStatistics.Type.SESSION_WRITE_DURATION, RepositoryStatistics.Type.SESSION_WRITE_COUNTER, MINUTE, NANOSEC);
    }

    @Override
    public double getRepositoryWriteDurationMillisAverageLastWeek() {
        return averageDurationMillis(RepositoryStatistics.Type.SESSION_WRITE_DURATION, RepositoryStatistics.Type.SESSION_WRITE_COUNTER, HOUR, NANOSEC);
    }

    @Override
    public double getRepositoryLoginsLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.SESSION_LOGIN_COUNTER, SECOND);
    }

    @Override
    public double getRepositoryLoginsLastHourPerSecond() {
        return average(RepositoryStatistics.Type.SESSION_LOGIN_COUNTER, MINUTE) / 60;
    }

    @Override
    public double getRepositoryLoginsLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.SESSION_LOGIN_COUNTER, HOUR) / 3600;
    }


    @Override
    public double getRepositorySessionsLastMinuteAverage() {
        return average(RepositoryStatistics.Type.SESSION_COUNT, SECOND);
    }

    @Override
    public double getRepositorySessionsLastHourAverage() {
        return average(RepositoryStatistics.Type.SESSION_COUNT, MINUTE);
    }

    @Override
    public double getRepositorySessionsLastWeekAverage() {
        return average(RepositoryStatistics.Type.SESSION_COUNT, MINUTE);
    }

    @Override
    public double getQueriesLastMinutePerSecond() {
        return average(RepositoryStatistics.Type.QUERY_COUNT, SECOND);
    }

    @Override
    public double getQueriesLastHourPerSecond() {
        return average(RepositoryStatistics.Type.QUERY_COUNT, MINUTE) / 60;
    }

    @Override
    public double getQueriesLastWeekPerSecond() {
        return average(RepositoryStatistics.Type.QUERY_COUNT, HOUR) / 3600;
    }

    @Override
    public double getQueryDurationMillisAverageLastMinute() {
        return averageDurationMillis(RepositoryStatistics.Type.QUERY_DURATION, RepositoryStatistics.Type.QUERY_COUNT, SECOND, Granularity.MILLIS);
    }

    @Override
    public double getQueryDurationMillisAverageLastHour() {
        return averageDurationMillis(RepositoryStatistics.Type.QUERY_DURATION, RepositoryStatistics.Type.QUERY_COUNT, MINUTE, Granularity.MILLIS);
    }

    @Override
    public double getQueryDurationMillisAverageLastWeek() {
        return averageDurationMillis(RepositoryStatistics.Type.QUERY_DURATION, RepositoryStatistics.Type.QUERY_COUNT, HOUR, Granularity.MILLIS);
    }

    @Override
    public double getBundleCacheSizeMbLastHourAverage() {
        final double average = average(RepositoryStatistics.Type.BUNDLE_CACHE_SIZE_COUNTER, MINUTE);
        return average / (1024 * 1024);
    }

    @Override
    public double getBundleCacheHitLastMinutePerSecond() {
        return getBundleReadLastMinutePerSecond() - getBundleCacheMissLastMinutePerSecond();
    }

    @Override
    public double getBundleCacheHitLastHourPerSecond() {
        return getBundleReadLastHourPerSecond() - getBundleCacheMissLastHourPerSecond();
    }

    @Override
    public double getBundleCacheHitLastWeekPerSecond() {
        return getBundleReadLastWeekPerSecond() - getBundleCacheMissLastWeekPerSecond();
    }

    private double average(final RepositoryStatistics.Type type, Per per) {
        switch (per) {
            case SECOND:
                return average(repositoryStatistics.getTimeSeries(type).getValuePerSecond());
            case MINUTE:
                return averageIgnore0Value(repositoryStatistics.getTimeSeries(type).getValuePerMinute());
            case HOUR:
                return averageIgnore0Value(repositoryStatistics.getTimeSeries(type).getValuePerHour());
            default:
                return 0;
        }
    }

    private double average(final long[] values) {
        double total = 0;
        for (long value : values) {
            total += value;
        }
        return total / values.length;
    }

    /**
     * to avoid that for example org.apache.jackrabbit.api.stats.TimeSeries#getValuePerWeek() averages over many 0
     * values
     * (since per week returns an arrays of 156 weeks where of course only after 3 years running env all weeks have
     * data)
     * we only take non-0-values into account
     */
    private double averageIgnore0Value(final long[] values) {
        double total = 0;
        long counter = 0;
        for (long value : values) {
            if (value > 0) {
                total += value;
                counter++;
            }
        }
        if (counter == 0) {
            return 0;
        }
        return total / counter;
    }

    private double averageDurationMillis(final RepositoryStatistics.Type duration, final RepositoryStatistics.Type counter, final Per per, final Granularity measuredGranularity) {
        switch (per) {
            case SECOND:
                return average(repositoryStatistics.getTimeSeries(duration).getValuePerSecond(), repositoryStatistics.getTimeSeries(counter).getValuePerSecond(), measuredGranularity);
            case MINUTE:
                return average(repositoryStatistics.getTimeSeries(duration).getValuePerMinute(), repositoryStatistics.getTimeSeries(counter).getValuePerMinute(), measuredGranularity);
            case HOUR:
                return average(repositoryStatistics.getTimeSeries(duration).getValuePerHour(), repositoryStatistics.getTimeSeries(counter).getValuePerHour(), measuredGranularity);
            default:
                return 0;
        }
    }

    private double average(final long[] duration, final long[] counter, final Granularity measuredGranularity) {
        long totalCount = 0;
        long totalDuration = 0;
        for (int i = 0; i < counter.length; i++) {
            if (counter[i] > 0) {
                totalCount += counter[i];
                totalDuration += duration[i];
            }
        }
        // duration is in nanotime
        if (totalCount == 0) {
            return 0D;
        }
        switch (measuredGranularity) {
            case NANOSEC:
                return (totalDuration / totalCount) / 1000000D;
            case MILLIS:
                return (totalDuration / totalCount);
            default:
                return (totalDuration / totalCount);
        }
    }

}
