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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * ESICommentFragmentInfo
 * <P>
 * The ESI Comment Fragment information showing all the relevant information in the page.
 * </P>
 */
public class ESICommentFragmentInfo extends ESIFragmentInfo {

    private List<ESIFragmentInfo> fragmentInfos;

    public ESICommentFragmentInfo(ESIFragment fragment, int beginIndex, int endIndex) {
        super(fragment, beginIndex, endIndex);
    }

    public void addAllFragmentInfos(Collection<ESIFragmentInfo> fragmentInfos) {
        if (this.fragmentInfos == null) {
            this.fragmentInfos = new LinkedList<ESIFragmentInfo>();
        }

        this.fragmentInfos.addAll(fragmentInfos);
    }

    public void addFragmentInfo(ESIFragmentInfo fragmentInfo) {
        if (fragmentInfos == null) {
            fragmentInfos = new LinkedList<ESIFragmentInfo>();
        }

        fragmentInfos.add(fragmentInfo);
    }

    public void removeAllFragmentInfos() {
        if (fragmentInfos != null) {
            fragmentInfos.clear();
        }
    }

    public List<ESIFragmentInfo> getFragmentInfos() {
        if (fragmentInfos != null) {
            return Collections.unmodifiableList(fragmentInfos);
        }

        return Collections.emptyList();
    }

    public boolean hasAnyFragmentInfo() {
        return (fragmentInfos != null && !fragmentInfos.isEmpty());
    }
}
