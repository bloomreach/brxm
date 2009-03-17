#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.documenttypes;

import org.hippoecm.hst.service.ServiceNamespace;
import org.hippoecm.hst.service.UnderlyingServiceAware;


@ServiceNamespace(prefix = "hippostd")
public interface StandardArticle extends UnderlyingServiceAware{
    String getStateSummary();
    String getState();
}
