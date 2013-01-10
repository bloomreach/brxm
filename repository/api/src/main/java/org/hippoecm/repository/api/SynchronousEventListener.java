/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * A marker interface for synchronous event listener.
 * <p>
 * If a event listener implements this marker interface, the implementation
 * will decorate the listener with Jacrabbit's 
 * <code>org.apache.jackrabbit.core.observation.SynchronousEventListener</code>,
 * which is another marker interface in Jackrabbit for {@link javax.jcr.observation.EventListener}
 * implementations that wish a synchronous notification of changes to the
 * workspace. That is, a <code>SynchronousEventListener</code> is called before
 * the call to {@link javax.jcr.Item#save()} returns. In contrast, a regular
 * {@link javax.jcr.observation.EventListener} might be called after
 * <code>save()</code> returns.
 * </p>
 * <p/>
 * <b>Important note</b>: an implementation of {@link SynchronousEventListener}
 * <b>must not</b> modify content with the thread that calls {@link
 * #onEvent(EventIterator)} otherwise inconsistencies may occur.
 */
public interface SynchronousEventListener extends EventListener {
}
