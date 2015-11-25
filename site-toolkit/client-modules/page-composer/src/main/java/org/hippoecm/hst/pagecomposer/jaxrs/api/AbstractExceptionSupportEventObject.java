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

public abstract class AbstractExceptionSupportEventObject extends EventObject {

    private transient RuntimeException exception;

    public AbstractExceptionSupportEventObject(final Object source) {
        super(source);
    }

    /**
     * @return the {@link RuntimeException} if it was set via {@link AbstractExceptionSupportEventObject#setException(RuntimeException)}
     * and <code>null</code> when no exception was set
     */
    public RuntimeException getException() {
        return exception;
    }

    /**
     * @param exception set the {@link java.lang.RuntimeException} for this {@link PageCopyEvent} unless there was already a
     *                  a {@link java.lang.RuntimeException} set. In that case, the invocation of this method does not change
     */
    public void setException(final RuntimeException exception) {
        if (this.exception != null) {
            if (getLogger() != null) {
                getLogger().debug("Skipping exception '{}' for EventObject {} because already exception {} set.", exception.toString(),
                        this, this.exception.toString());
            }
            return;
        }
        this.exception = exception;
    }

    public abstract Logger getLogger();
}
