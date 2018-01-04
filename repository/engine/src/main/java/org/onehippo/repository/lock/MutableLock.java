/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.lock;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import org.onehippo.cms7.services.lock.Lock;

public class MutableLock extends Lock {

    private final WeakReference<Thread> thread;

    private final AtomicInteger holdCount;

    public MutableLock(final String lockKey, final String lockOwner, final String lockThread,
                       final long creationTime, final String status) {
        super(lockKey, lockOwner, lockThread, creationTime, status);
        thread = new WeakReference<>(Thread.currentThread());
        holdCount = new AtomicInteger(1);
    }

    public WeakReference<Thread> getThread() {
        return thread;
    }

    public void increment() {
        holdCount.incrementAndGet();
    }

    public void decrement() {
        holdCount.decrementAndGet();
    }

    public int getHoldCount() {
        return holdCount.get();
    }

}
