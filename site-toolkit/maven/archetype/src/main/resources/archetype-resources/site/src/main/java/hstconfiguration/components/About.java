#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.hstconfiguration.components;

import org.hippoecm.hst.component.support.ocm.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.ocm.HippoStdNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class About extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(About.class);

    
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        super.doAction(request, response);
        
        
        
               
        
        
    }



    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
        HippoStdNode  n = getContentNode(request);
        
        if(n == null) {
            return;
        }
        request.setAttribute("document",n);
        
        
    }
    

}
