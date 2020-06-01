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
package org.hippoecm.hst.core.container;

/**
 * The <CODE>ComponentsException</CODE> class defines a exception
 * while finding component(s) from the component manager.
 */
public class ComponentsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ComponentsException exception.
     */
    public ComponentsException() {
        super();
    }

    /**
     * Constructs a new ComponentsException exception with the given message.
     *
     * @param   message
     *          the exception message
     */
    public ComponentsException(String message) {
        super(message);
    }

    /**
     * Constructs a new ComponentsException exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public ComponentsException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new ComponentsException exception when the container needs to do
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
    public ComponentsException(String msg, Throwable nested) {
        super(msg, nested);
    }

}
