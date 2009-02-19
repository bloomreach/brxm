package org.hippoecm.hst.tags;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;

/**
 * Abstract supporting class for Hst URL tags (action, redner and resource)
 */

public class HstContentTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    protected String path = null;
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        return EVAL_BODY_INCLUDE;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{

        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
        
        if (request instanceof HstRequest) {
            HstComponentWindow myWindow = ((HstRequest) request).getComponentWindow();
            HstComponentWindow childWindow = myWindow.getChildWindow(this.path);
            
            if (childWindow != null) {
                try {
                    this.pageContext.getOut().flush();
                    childWindow.flushContent();
                } catch (IOException e) {
                }
            }
        }
        
        return EVAL_PAGE;
    }
    
    /**
     * Returns the path property.
     * @return String
     */
    public String getPath() {
        return this.path;
    }
    
    /**
     * Sets the path property.
     * @param path The path to set
     * @return void
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstContentTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("path");
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
