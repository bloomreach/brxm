/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.replication;

import org.apache.jackrabbit.core.cluster.ClusterRecordProcessor;

/**
 * A RuntimeException is needed because some API's like the {@link ClusterRecordProcessor} don't
 * allow for checked exceptions.
 */
public class RetryReplicationException extends RuntimeException {
    /** @exclude */
    
    /**
     * Serial
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The root cause of this <code>ConversionException</code>.
     */
    protected Throwable cause = null;

    /**
     * Construct a new exception with the specified message.
     *
     * @param message The message describing this exception
     */
    public RetryReplicationException(String message) {
        super(message);
    }

    /**
     * Construct a new exception with the specified message and root cause.
     *
     * @param message The message describing this exception
     * @param cause The root cause of this exception
     */
    public RetryReplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new exception with the specified root cause.
     *
     * @param cause The root cause of this exception
     */
    public RetryReplicationException(Throwable cause) {
        super(cause);
    }
}
