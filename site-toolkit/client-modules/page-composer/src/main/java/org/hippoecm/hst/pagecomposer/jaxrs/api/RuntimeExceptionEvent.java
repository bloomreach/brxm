/**
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.slf4j.Logger;

/**
 * <p>
 *     Abstract {@link EventObject} class that can be used to set exceptions on, that later on can be by the event originating
 *     code to short-circuit processing.
 * </p>
 * <p>
 *     Listeners to these events in general can best first check whether the event already has an exception, and if so,
 *     return directly. For example
 *     <pre>
 *         {@literal @}Subscribe
 *         {@literal @}AllowConcurrentEvents
 *         <code>public void onPageCopyEvent(PageCopyEvent event) {
 *           if (event.getException() != null) {
 *           return;
 *         }
 *         </code>
 *     </pre>
 * </p>
 *
 */
public abstract class RuntimeExceptionEvent extends EventObject {

    private transient RuntimeException exception;

    public RuntimeExceptionEvent(final Object source) {
        super(source);
    }

    /**
     * <p>
     *     Listeners to events in general can best first check whether the event already has an exception, and if so,
     *     return directly. For example
     *     <pre>
     *         {@literal @}Subscribe
     *         {@literal @}AllowConcurrentEvents
     *         <code>public void onPageCopyEvent(PageCopyEvent event) {
     *           if (event.getException() != null) {
     *           return;
     *         }
     *         </code>
     *     </pre>
     * </p>
     * @return the {@link RuntimeException} if it was set via {@link RuntimeExceptionEvent#setException(RuntimeException)}
     * and <code>null</code> when no exception was set
     */
    public RuntimeException getException() {
        return exception;
    }

    /**
     * @param exception sets the {@link java.lang.RuntimeException} for this {@link RuntimeExceptionEvent}.
     *                  If there is already an exception set, the exception is reset
     */
    public void setException(final RuntimeException exception) {
        if (this.exception != null) {
            if (getLogger() != null) {
                getLogger().debug("Resetting exception '{}' for EventObject {} because already exception {} set.", exception.toString(),
                        this, this.exception.toString());
            }
        }
        this.exception = exception;
    }

    public abstract Logger getLogger();
}
