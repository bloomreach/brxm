/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.util;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class NoJavaMailSenderImpl extends JavaMailSenderImpl {

    private static final Logger log = LoggerFactory.getLogger(NoJavaMailSenderImpl.class);

    public void init() {
        // HSTTWO-3472: Test if we can access HST ComponentManager in a bean initialized by
        //              Spring Framework WebApplicationContext loaded by ContextLoaderListener
        final ComponentManager compMgr = HstServices.getComponentManager();

        if (compMgr == null) {
            log.error("!!!!!!!!!!!! HST ComponentManager is not available from Spring Framework bean! :-(");
        } else {
            log.info("!!!!!!!!!!!! HST ComponentManager is available from Spring Framework bean! :-)");
        }
    }

    public void destroy() {
    }

    @Override
    public void send(final SimpleMailMessage simpleMessage) throws MailException {
        log.info("Send mail ..... : {}", simpleMessage);
    }

    @Override
    public void send(final SimpleMailMessage... simpleMessages) throws MailException {
        log.info("Send mails ..... : {}", simpleMessages);
    }
}
