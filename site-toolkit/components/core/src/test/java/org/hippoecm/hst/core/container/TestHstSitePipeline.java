/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import static org.junit.Assert.assertArrayEquals;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestHstSitePipeline
 */
public class TestHstSitePipeline {

    private static Logger log = LoggerFactory.getLogger(TestHstSitePipeline.class);

    private Valve initializationValve = new InitializationValve();
    private AbstractBaseOrderableValve cmsSecurityValve = new CmsSecurityValve();

    private Valve localizationValve = new LocalizationValve();
    private Valve securityValve = new SecurityValve();
    private Valve contextResolvingValve = new ContextResolvingValve();
    private Valve actionValve = new ActionValve();
    private Valve resourceServingValve = new ResourceServingValve();
    private AbstractBaseOrderableValve pageCachingValve = new PageCachingValve();
    private Valve aggregationValve = new AggregationValve();

    private Valve cleanupValve = new CleanupValve();
    private AbstractBaseOrderableValve diagnosticReportingValve = new DiagnosticReportingValve();

    @Test
    public void testBasicValveOrdering() throws Exception {
        HstSitePipeline pipeline = new HstSitePipeline();

        pipeline.setInitializationValves(new Valve[] { initializationValve });
        pipeline.setProcessingValves(new Valve [] { localizationValve, securityValve, contextResolvingValve, actionValve, resourceServingValve, aggregationValve });
        pipeline.setCleanupValves(new Valve[] { cleanupValve });

        cmsSecurityValve.setAfter(InitializationValve.class.getName());
        pipeline.addInitializationValve(cmsSecurityValve);

        pageCachingValve.setAfter(ActionValve.class.getName());
        pageCachingValve.setBefore(AggregationValve.class.getName());
        pipeline.addProcessingValve(pageCachingValve);

        diagnosticReportingValve.setAfter(CleanupValve.class.getName());
        pipeline.addCleanupValve(diagnosticReportingValve);

        Valve [] mergedProcessingValves = pipeline.mergeProcessingValves();
        log.info("merged processing valves: \n\t{}", StringUtils.join(mergedProcessingValves, "\n\t"));
        assertArrayEquals(new Valve [] {
                initializationValve,
                cmsSecurityValve,
                localizationValve,
                securityValve,
                contextResolvingValve,
                actionValve,
                resourceServingValve,
                pageCachingValve,
                aggregationValve
        }, mergedProcessingValves);

        Valve [] mergedCleanupValves = pipeline.mergeCleanupValves();
        log.info("merged cleanup valves: \n\t{}", StringUtils.join(mergedCleanupValves, "\n\t"));
        assertArrayEquals(new Valve [] {
                cleanupValve,
                diagnosticReportingValve
        }, mergedCleanupValves);
    }

}
