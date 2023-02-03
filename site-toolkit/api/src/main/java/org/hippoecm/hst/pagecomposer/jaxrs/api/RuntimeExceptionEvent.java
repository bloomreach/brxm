/*
 *  Copyright 2018-2023 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

/**
 * Synchronous event abstraction to allow a listener to be able to set a runtime exception.
 */
public interface RuntimeExceptionEvent {

    /**
     * Return the runtime exception occurred in a listener's handling.
     * @return the runtime exception occurred by a listener's handling
     */
    public RuntimeException getException();

    /**
     * Set the runtime exception occurred in a listener's handling.
     * @param runtimeException the runtime exception occurred by a listener's handling
     */
    public void setException(final RuntimeException runtimeException);

}
