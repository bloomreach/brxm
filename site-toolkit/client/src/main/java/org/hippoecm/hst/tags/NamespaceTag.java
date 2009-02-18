package org.hippoecm.hst.tags;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;

/**
 * This tag produces a unique value for the current HST component.
 * <p/>
 * <p/>
 * A tag handler for the <CODE>namespace</CODE> tag. writes a unique value
 * for the current HstComponent <BR>This tag has no attributes
 */
public class NamespaceTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException {
        
        String namespace = "";
        
        HttpServletRequest servletRequest = (HttpServletRequest) this.pageContext.getRequest();
        
        if (servletRequest instanceof HstRequest) {
            HstComponentWindow myWindow = ((HstRequest) servletRequest).getComponentWindow();
            
            if (myWindow != null) {
                namespace = myWindow.getReferenceNamespace();
            }
        }
        
        JspWriter writer = pageContext.getOut();
        
        try {
            writer.print(namespace);
        } catch (IOException ioe) {
            throw new JspException("Unable to write namespace", ioe);
        }
        
        return SKIP_BODY;
    }
}
