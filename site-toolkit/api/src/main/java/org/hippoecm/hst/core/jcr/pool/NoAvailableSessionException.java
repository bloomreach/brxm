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
package org.hippoecm.hst.core.jcr.pool;

import java.util.NoSuchElementException;

/**
 * The <CODE>NoAvailableSessionException</CODE> class defines the exception
 * that a JCR session pooling repository can throw when it is unable to return an available session
 * since its pooled session are already exhausted and it cannot create new session.
 */
public class NoAvailableSessionException extends NoSuchElementException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new NoAvailableSessionException exception.
     */
    public NoAvailableSessionException() {
        super();
    }

    /**
     * Constructs a <code>NoAvailableSessionException</code>, saving a reference
     * to the error message string <tt>s</tt> for later retrieval by the
     * <tt>getMessage</tt> method.
     *
     * @param   s   the detail message.
     */
    public NoAvailableSessionException(String s) {
        super(s);
    }
    
}
