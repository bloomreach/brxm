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
package org.hippoecm.hst.util;

/**
 * The <CODE>ObjectFactoryException</CODE> class defines a general exception
 * that an ObjectFactory can throw when it is unable to perform its operation
 * successfully.
 * 
 * @version $Id$
 */
public class ObjectFactoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ObjectFactory exception.
     */
    public ObjectFactoryException() {
        super();
    }

    /**
     * Constructs a new ObjectFactoryException exception with the given message. The
     * ObjectFactory implementation may use the message write it to a log.
     *
     * @param   message
     *          the exception message
     */
    public ObjectFactoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new ObjectFactory exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public ObjectFactoryException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new HstComponent exception when the ObjectFactory implementation needs to do
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
    public ObjectFactoryException(String msg, Throwable nested) {
        super(msg, nested);
    }
    
}
