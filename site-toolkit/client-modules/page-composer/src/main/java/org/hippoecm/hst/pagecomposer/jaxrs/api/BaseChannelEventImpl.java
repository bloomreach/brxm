/**
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.EventObject;

import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseChannelEventImpl extends EventObject implements BaseChannelEvent {

    private static final long serialVersionUID = 1L;

    private final Channel channel;
    private transient RuntimeException exception;
    private transient Logger logger;

    public BaseChannelEventImpl(final Channel channel) {
        super(channel);
        this.channel = channel;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public RuntimeException getException() {
        return exception;
    }

    @Override
    public void setException(RuntimeException runtimeException) {
        this.exception = runtimeException;
    }

    @Override
    public Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(getClass());
        }

        return logger;
    }

}
