package org.hippoecm.hst.hstconfiguration.components.fragments.header;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.hstconfiguration.components.HstComponentBase;

public class PageStyle extends HstComponentBase {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        super.doBeforeRender(request, response);
        
        List<String> s = new ArrayList<String>();
        s.add("foo");
        s.add("bar");
        
        request.setAttribute("test", s);
    }


    
}
