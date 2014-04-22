/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.scxml;

/**
 * SCXMLException is a specialized Exception for notifying SCXML state machine instantiation and loading exceptions.
 */
public class SCXMLException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SCXMLException exception.
     */
    @SuppressWarnings("unused")
    public SCXMLException() {
        super();
    }

    /**
     * Constructs a new SCXMLException exception with the given message.
     *
     * @param   message
     *          the exception message
     */
    @SuppressWarnings("unused")
    public SCXMLException(String message) {
        super(message);
    }

    /**
     * Constructs a new SCXMLException exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public SCXMLException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new SCXMLException exception with the following:
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
    public SCXMLException(String msg, Throwable nested) {
        super(msg, nested);
    }
    
}
