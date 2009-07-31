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
package org.hippoecm.hst.core.linking;

/**
 * Interface for <code>HstLinkPostProcessorChain</code>'s. After a link creation, all <code>HstLinkPostProcessorChain</code>'s which are configured are executed. 
 * 
 * Note that implementation most likely need to have a reversible HstLinkProcessorChain: Thus doHstLinkPostProcess(doHstLinkPreProcess(HstLink a)).getPath().equals(a.getPath())
 *
 */
public interface HstLinkProcessorChain {

   /**
    * Causes the next link post processor in the chain to be invoked, or if the calling post processor is the last post processor
    * in the chain, returns the HstLink
    * @param hstLink
    * @return the new PostProcessed HstLink
    */
   HstLink doHstLinkPostProcess(HstLink hstLink);
   
   /**
    * Causes the next link pre processor in the chain to be invoked, or if the calling pre processor is the last pre processor
    * in the chain, returns the HstLink
    * @param hstLink
    * @return the new PreProcessed HstLink
    */
   HstLink doHstLinkPreProcess(HstLink hstLink);
}
