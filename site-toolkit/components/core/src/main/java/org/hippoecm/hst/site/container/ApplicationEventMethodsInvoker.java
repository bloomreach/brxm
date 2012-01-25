/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.site.container;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.MethodInvoker;

/**
 * Utility callback method invoker bean 
 * which can be used to invoke custom callback methods 
 * on ApplicationEvent from the ApplicationContext.
 * 
 * @version $Id$
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.ApplicationEvent
 */
public class ApplicationEventMethodsInvoker implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    
    private static Logger log = LoggerFactory.getLogger(ApplicationEventMethodsInvoker.class);

    private AbstractApplicationContext ctx;
    private Map<Class<? extends ApplicationEvent>, List<MethodInvoker>> eventTypeMethodInvokersMap;
    private boolean autoPrepareInvoker = true;

    public ApplicationEventMethodsInvoker(
            Map<Class<? extends ApplicationEvent>, List<MethodInvoker>> eventTypeMethodInvokersMap) {
        this.eventTypeMethodInvokersMap = eventTypeMethodInvokersMap;
    }

    public void setAutoPrepareInvoker(boolean autoPrepareInvoker) {
        this.autoPrepareInvoker = autoPrepareInvoker;
    }

    public boolean getAutoPrepareInvoker() {
        return autoPrepareInvoker;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof AbstractApplicationContext) {
            ctx = (AbstractApplicationContext) applicationContext;
            ctx.addApplicationListener(this);
        }
    }

    /** {@inheritDoc} */
    public void onApplicationEvent(ApplicationEvent event) {
        boolean doIt = false;

        ApplicationContext ac = ctx;
        while (ac != null && !doIt) {
            if (event.getSource() == ac) {
                doIt = true;
            }
            ac = ac.getParent();
        }

        if (!doIt) {
            return;
        }

        List<MethodInvoker> invokers = findMethodInvokersByEvent(event);

        if (invokers != null && !invokers.isEmpty()) {
            for (MethodInvoker invoker : invokers) {
                try {
                    if (autoPrepareInvoker && !invoker.isPrepared()) {
                        invoker.prepare();
                    }

                    invoker.invoke();
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.error("Failed to invoke application event callback invoker. " + e, e);
                    } else {
                        log.error("Failed to invoke application event callback invoker. " + e);
                    }
                }
            }
        }
    }

    private List<MethodInvoker> findMethodInvokersByEvent(final ApplicationEvent event) {
        if (eventTypeMethodInvokersMap != null && event != null) {
            for (Map.Entry<Class<? extends ApplicationEvent>, List<MethodInvoker>> entry : eventTypeMethodInvokersMap
                    .entrySet()) {
                Class<? extends ApplicationEvent> eventType = entry.getKey();

                if (eventType.isAssignableFrom(event.getClass())) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }
}
