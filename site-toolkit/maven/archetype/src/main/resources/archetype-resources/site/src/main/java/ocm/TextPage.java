#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
#set($hyphen = '-')
#set($empty = '')
package ${package}.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="${rootArtifactId.replace($hyphen,$empty)}:textpage")
public class TextPage extends GeneralPage{

    public HippoHtml getHtml(){
        return getHippoHtml("${rootArtifactId.replace($hyphen,$empty)}:body");    
    }
}
