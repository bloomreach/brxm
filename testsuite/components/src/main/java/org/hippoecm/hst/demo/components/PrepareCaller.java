/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;

import org.apache.commons.lang.RandomStringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.demo.util.SimpleTimeKeeper;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class PrepareCaller extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(PrepareCaller.class);

    private ThreadPoolTaskExecutor executor;

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig)
            throws HstComponentException {
        super.init(servletContext, componentConfig);
        executor = HstServices.getComponentManager().getComponent("taskExecutor");
    }

    @Override
    public void prepareBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        final RandomStringHolder randomStringHolder = new RandomStringHolder(40);
        final Future<RandomStringHolder> randomStringCommand = executor
                .submit(new RandomStringCommand(randomStringHolder, 500));
        request.setAttribute("randomStringCommand", randomStringCommand);
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        try {
            final Future<RandomStringHolder> randomStringCommand = (Future<RandomStringHolder>) request
                    .getAttribute("randomStringCommand");
            final RandomStringHolder randomStringHolder = randomStringCommand.get();
            request.setAttribute("randomStringHolder", randomStringHolder);
        } catch (Exception e) {
            log.error("Failed to call randomStringCommand.", e);
        }
    }

    public static class RandomStringHolder extends SimpleTimeKeeper {

        private final int characterCount;
        private String randomString;

        public RandomStringHolder(final int characterCount) {
            super();
            this.characterCount = characterCount;
        }

        public int getCharacterCount() {
            return characterCount;
        }

        public String getRandomString() {
            return randomString;
        }

        private void setRandomString(String randomString) {
            this.randomString = randomString;
        }
    }

    public static class RandomStringCommand implements Callable<RandomStringHolder> {

        private final RandomStringHolder randomStringHolder;

        private final long waitTimeout;

        public RandomStringCommand(final RandomStringHolder resultHolder, final long waitTimeout) {
            this.randomStringHolder = resultHolder;
            this.waitTimeout = waitTimeout;
        }

        @Override
        public RandomStringHolder call() throws Exception {
            randomStringHolder.begin();
            Thread.sleep(waitTimeout);
            randomStringHolder.setRandomString(RandomStringUtils.randomAlphanumeric(randomStringHolder.getCharacterCount()));
            randomStringHolder.end();
            return randomStringHolder;
        }

        public long getWaitTimeout() {
            return waitTimeout;
        }
    }
}
