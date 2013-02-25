/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.search;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstQueryManagerFactoryImpl implements HstQueryManagerFactory {

    private static final Logger log = LoggerFactory.getLogger(HstQueryManagerFactoryImpl.class);
    private String defaultQueryDateRangeResolution;

    @Override
    public HstQueryManager createQueryManager(Session session, ObjectConverter objectConverter) {
        DateTools.Resolution resolution = fromString(defaultQueryDateRangeResolution);
        log.info("Default query date range resolution is : {}", resolution);
        HstQueryManager mngr = new HstQueryManagerImpl(session, objectConverter, resolution);
        return mngr;
    }

    public void setDefaultQueryDateRangeResolution(String defaultQueryDateRangeResolution) {
        this.defaultQueryDateRangeResolution = defaultQueryDateRangeResolution;
    }

    /**
     * @param resolution the name of the resolution, for example, year, Year,YEAR. if resolution is <code>null</code>,
     *            {@link org.hippoecm.repository.util.DateTools.Resolution#MILLISECOND} is returned.
     * @return Resolution for <code>name</code>. <code>name</code> is compared case-insensitive. If non matches,
     *         <code>{@link org.hippoecm.repository.util.DateTools.Resolution#MILLISECOND}</code> is returned
     */
    private DateTools.Resolution fromString(String resolution) {
        if (resolution == null) {
            return DateTools.Resolution.MILLISECOND;
        }
        resolution = resolution.toLowerCase().trim();
        if (resolution.equals("year")) {
            return DateTools.Resolution.YEAR;
        }
        if (resolution.equals("month")) {
            return DateTools.Resolution.MONTH;
        }
        if (resolution.equals("day")) {
            return DateTools.Resolution.DAY;
        }
        if (resolution.equals("hour")) {
            return DateTools.Resolution.HOUR;
        }
        log.warn("Unknown resolution '{}'. Return MILLISECOND resolution.", resolution);
        return DateTools.Resolution.MILLISECOND;
    }
}
