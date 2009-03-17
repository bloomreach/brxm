#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.documenttypes;

import org.hippoecm.hst.service.ServiceNamespace;

@ServiceNamespace(prefix = "hippostd")
public interface Content extends StandardArticle {
	String getContent();
}
