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
package org.hippoecm.hst.content.beans;

/**
 * The <CODE>ContentNodeBindingException</CODE> class defines a binding exception
 * that a <CODE>ContentNodeBinder</CODE> can throw when it is unable to perform its operation
 * successfully.
 * 
 */
public class ContentNodeBindingException extends ObjectBeanPersistenceException {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ContentNodeBindingException exception.
     */
    public ContentNodeBindingException() {
        super();
    }

    /**
     * Constructs a new ContentNodeBindingException exception with the given message.
     *
     * @param   message
     *          the exception message
     */
    public ContentNodeBindingException(String message) {
        super(message);
    }

    /**
     * Constructs a new ContentNodeBindingException exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public ContentNodeBindingException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new ContentNodeBindingException exception when the container needs to do
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
    public ContentNodeBindingException(String msg, Throwable nested) {
        super(msg, nested);
    }
    
}
