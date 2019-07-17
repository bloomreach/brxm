/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InitializationValve
 */
public class InitializationValve extends AbstractBaseOrderableValve {

    private static Logger log = LoggerFactory.getLogger(InitializationValve.class);

    private Pattern[] searchEngineUserAgentPatterns;

    public void setSearchEngineUserAgentPatterns(final String patternsInCSV) {
        if (StringUtils.isBlank(patternsInCSV)) {
            searchEngineUserAgentPatterns = new Pattern[0];
        } else {
            log.info("Setting search engine user agent patterns: '{}'.", patternsInCSV);
            List<Pattern> patternsList = new ArrayList<>();

            for (String regex : StringUtils.split(patternsInCSV, ",")) {
                regex = regex.trim();

                if (regex.isEmpty()) {
                    continue;
                }

                try {
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    patternsList.add(pattern);
                } catch (Exception e) {
                    log.error("Invalid search engine user agent pattern: '{}'.", regex, e);
                }
            }

            searchEngineUserAgentPatterns = patternsList.toArray(new Pattern[patternsList.size()]);
        }
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final boolean searchEngineRequest = isSearchEngineRequest(context.getServletRequest());
        setSearchEngineRequest(searchEngineRequest);

        HstMutableRequestContext requestContext = (HstMutableRequestContext)context.getRequestContext();
        // because the requestContext can already have a jcr session (for example fetched during a SiteMapItemHandler or
        // during HstLinkProcessor pre processing, we explicitly set it to null in the HstMutableRequestContext to be
        // sure it gets a new one when requestContext#getSession is called : Namely, it can result in a different
        // jcr session (for example because the SecurityValve kicks in or because of a custom ContextCredentialsProvider
        // which inspects some state being provided by some custom valve
        requestContext.setSession(null);

        // continue
        context.invokeNext();
    }

    boolean isSearchEngineRequest(final HttpServletRequest servletRequest) {
        if (searchEngineUserAgentPatterns.length != 0) {
            final String userAgent = servletRequest.getHeader("User-Agent");

            if (StringUtils.isNotBlank(userAgent)) {
                for (Pattern pattern : searchEngineUserAgentPatterns) {
                    final Matcher matcher = pattern.matcher(userAgent);

                    if (matcher.find()) {
                        log.debug("Detected a search engine request from '{}' by the pattern, '{}'.", userAgent,
                                pattern);
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
