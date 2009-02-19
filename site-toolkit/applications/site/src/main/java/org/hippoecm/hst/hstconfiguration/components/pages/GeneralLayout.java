package org.hippoecm.hst.hstconfiguration.components.pages;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.hstconfiguration.components.HstComponentBase;

public class GeneralLayout extends HstComponentBase {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
      System.out.println("!!!!");
    } 
   
}
