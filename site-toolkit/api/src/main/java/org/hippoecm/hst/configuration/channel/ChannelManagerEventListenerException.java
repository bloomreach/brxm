/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.configuration.channel;

/**
 * Exception that you can throw in a {@link ChannelManagerEventListener} implementation : When you
 * wants to short circuit the processing of the {@link Channel} creation / update entirely, you have to use 
 * 
 */
public class ChannelManagerEventListenerException extends Exception {

    private static final long serialVersionUID = 1L;
    
    enum Status {
        STOP_CHANNEL_PROCESSING,
        LOG_AND_CONTINUE
    }
    
    private Status status;

    
    /**
     * Constructs a new {@link ChannelManagerEventListenerException} exception.
     */
    public ChannelManagerEventListenerException(Status status) {
        super();
        this.status = status;
    }

    /**
     * Constructs a new {@link ChannelManagerEventListenerException} exception with the given message.
     *
     * @param   message
     *          the exception message
     */
    public ChannelManagerEventListenerException(Status status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Constructs a new {@link ChannelManagerEventListenerException} exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public ChannelManagerEventListenerException(Status status, Throwable nested) {
        super(nested);
        this.status = status;
    }

    /**
     * Constructs a new {@link ChannelManagerEventListenerException} exception when the container needs to do
     * the following:
     * <ul>
     * <li>throw an exception 
     * <li>include the "nested" exception
     * <li>include a description message
     * </ul>
     *
     * @param   msg
     *          the exception message
     * @param   nested
     *          the nested exception
     */
    public ChannelManagerEventListenerException(Status status, String msg, Throwable nested) {
        super(msg, nested);
        this.status = status;
    }


    public Status getStatus() {
        return status;
    }

}
