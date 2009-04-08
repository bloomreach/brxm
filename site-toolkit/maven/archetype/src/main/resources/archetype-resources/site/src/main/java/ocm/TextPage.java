#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
#set($hyphen = '-')
#set($empty = '')
package ${package}.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdHtml;

@Node(jcrType="${rootArtifactId.replace($hyphen,$empty)}:textpage", discriminator=false)
public class TextPage extends GeneralPage{

	private HippoStdHtml html;
	
    @Bean(jcrName="${rootArtifactId.replace($hyphen,$empty)}:body")
    public HippoStdHtml getHtml(){
        return html;
    }
    
    public void setHtml(HippoStdHtml html) {
        this.html = html;
    }

}
