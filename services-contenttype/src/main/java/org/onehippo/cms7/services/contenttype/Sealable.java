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

package org.onehippo.cms7.services.contenttype;

/**
 * Sealable base class to support beans which should become immutable after initialization.
 */
abstract class Sealable {
    private boolean sealed;

    protected boolean isSealed() {
        return sealed;
    }

    protected void seal() {
        if (!isSealed()) {
            doSeal();
            sealed = true;
        }
    }

    protected abstract void doSeal();

    protected void checkSealed() {
        if (isSealed()) {
            throw new UnsupportedOperationException("Object has been sealed: modifications no longer allowed.");
        }
    }

    protected void checkNotSealed() {
        if (!isSealed()) {
            throw new UnsupportedOperationException("Object has not yet been sealed.");
        }
    }
}
