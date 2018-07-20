/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.usagestatistics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.HippoHeaderItem;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsageStatisticsHeaderItem extends HippoHeaderItem {

    private static final String USAGE_STATISTICS_JS = "usage-statistics.js";

    private static final Logger log = LoggerFactory.getLogger(UsageStatisticsHeaderItem.class);

    private static final UsageStatisticsHeaderItem INSTANCE = new UsageStatisticsHeaderItem();

    public static UsageStatisticsHeaderItem get() {
        return INSTANCE;
    }

    private UsageStatisticsHeaderItem() {
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("usage-statistics-header-item");
    }

    @Override
    public List<HeaderItem> getDependencies() {
        return Collections.singletonList(CmsHeaderItem.get());
    }

    @Override
    public void render(final Response response) {
        if (UsageStatisticsSettings.get().isEnabled()) {
            createUsageStatisticsReporter().render(response);
        }
        createLoginEventScript().render(response);
    }

    private HeaderItem createUsageStatisticsReporter() {
        final Map<String, String> scriptParams = new TreeMap<>();
        final String url = UsageStatisticsExternalUrl.get();
        scriptParams.put("externalScriptUrl", url);

        scriptParams.put("language", UsageStatisticsUtils.getLanguage());

        log.info("Including external script for reporting usage statistics: {}", url);

        final PackageTextTemplate usageStatistics = new PackageTextTemplate(UsageStatisticsHeaderItem.class, USAGE_STATISTICS_JS);
        final String javaScript = usageStatistics.asString(scriptParams);
        return OnLoadHeaderItem.forScript(javaScript);
    }

    private HeaderItem createLoginEventScript() {
        final UsageEvent loginEvent = new UsageEvent("login");

        final String releaseVersion = new SystemInfoDataProvider().getReleaseVersion();
        loginEvent.setParameter("releaseVersion", releaseVersion);

        return OnLoadHeaderItem.forScript(loginEvent.getJavaScript());
    }


}
