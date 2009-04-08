#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.hstconfiguration.components;

import org.hippoecm.hst.component.support.ocm.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdFolder;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Right extends BaseHstComponent{

    public static final Logger log = LoggerFactory.getLogger(Right.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
		HippoStdNode n = this.getContentNode(request);

		request.setAttribute("folders", ((HippoStdFolder)n).getFolders());
		request.setAttribute("curnode", n);
		
    }


}