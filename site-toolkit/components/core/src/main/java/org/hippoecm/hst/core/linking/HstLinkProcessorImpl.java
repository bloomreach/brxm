package org.hippoecm.hst.core.linking;

import java.util.List;

public class HstLinkProcessorImpl implements HstLinkProcessor{

    List<HstLinkProcessorChain> chain;
    
    public void setProcessorChain(List<HstLinkProcessorChain> chain) {
        this.chain = chain;
    }

    public HstLink postProcess(HstLink link) {
        if(chain == null) {
            return link;
        }
        for(HstLinkProcessorChain processor : chain) {
            link = processor.doHstLinkPostProcess(link);
        }
        return link;
     }
    
    public HstLink preProcess(HstLink link) {
        if(chain == null) {
            return link;
        }
        for(HstLinkProcessorChain processor : chain) {
            link = processor.doHstLinkPreProcess(link);
        }
        return link;
     }
}
