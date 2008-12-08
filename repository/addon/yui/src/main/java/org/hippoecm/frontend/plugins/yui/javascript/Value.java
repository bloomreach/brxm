/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.commons.lang.builder.ToStringBuilder;

public abstract class Value<K> implements IValue<K> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    K value;
    boolean allowNull = false;
    boolean skip = false;

    public Value(K value) {
        this.value = value;
    }

    public final K get() {
        return value;
    }

    public final void set(K value) {
        this.value = value;
    }

    public boolean isValid() {
        if ((value == null && !allowNull) || skip) {
            return false;
        }
        return true;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("skip", skip).append("allowNull", allowNull)
                .toString();
    }

}
