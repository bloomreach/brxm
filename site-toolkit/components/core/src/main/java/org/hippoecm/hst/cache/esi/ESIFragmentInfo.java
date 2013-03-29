/**
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
package org.hippoecm.hst.cache.esi;

import java.io.Serializable;

/**
 * ESIFragmentInfo abstraction
 * <P>
 * The ESI Fragment Information showing all the relevant information such as location in the page.
 * </P>
 */
public abstract class ESIFragmentInfo implements Serializable {

    /** Real ESI Fragment in the page source */
    private ESIFragment fragment;

    /** The begin index where this fragment is located in the page. */
    private int beginIndex;

    /** The end index (exclusive) where this fragment is located in the page. */
    private int endIndex;

    public ESIFragmentInfo(ESIFragment fragment, int beginIndex, int endIndex) {
        this.fragment = fragment;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public ESIFragment getFragment() {
        return fragment;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(" [").append(beginIndex).append(',').append(endIndex).append("]: ").append(fragment).toString();
    }
}
