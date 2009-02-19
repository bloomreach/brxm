package org.hippoecm.hst.tags;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;

public class HstURLTag extends BaseHstURLTag {

    private static final long serialVersionUID = 1L;
    
    protected HstURL url;

    @Override
    protected HstURL getUrl() {
        if (this.url == null) {
            HttpServletResponse servletResponse = (HttpServletResponse) this.pageContext.getResponse();
            
            if (servletResponse instanceof HstResponse) {
                this.url = ((HstResponse) servletResponse).createURL(getType());
            }
        }
        
        return this.url;
    }

    @Override
    protected void setUrl(HstURL url) {
        this.url = url;
    }

    /**
     * TagExtraInfo class for HstURLTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "java.lang.String", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}
