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
package org.hippoecm.hst.platform.linking;

import java.util.List;

import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkProcessor;

public class HstLinkProcessorChain implements HstLinkProcessor {
    private List<HstLinkProcessor> processorsInChain;

    public void setProcessorsInChain(List<HstLinkProcessor> processorsInChain) {
        this.processorsInChain = processorsInChain;
    }

    public HstLink postProcess(final HstLink link) {
        if(processorsInChain == null) {
            return link;
        }
        HstLink linkToBeProcessed = link;
        for (HstLinkProcessor processor : processorsInChain) {
            linkToBeProcessed = processor.postProcess(linkToBeProcessed);
        }
        return linkToBeProcessed;
    }

    public HstLink preProcess(HstLink link) {
        if(processorsInChain == null) {
            return link;
        }
        HstLink linkToBeProcessed = link;
        for (HstLinkProcessor processor : processorsInChain) {
            linkToBeProcessed = processor.preProcess(linkToBeProcessed);
        }
        return linkToBeProcessed;
    }
}
