package org.hippoecm.hst.tag;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;

public class HstPartialRenderURLTag extends BaseHstURLTag {

    private static final long serialVersionUID = 1L;

    protected HstURL url;

    @Override
    protected HstURL getUrl() {
        if (this.url == null) {
            // if hstResponse is retrieved, then this servlet has been dispatched by hst component.
            HstResponse hstResponse = (HstResponse) pageContext.getRequest().getAttribute(ContainerConstants.HST_RESPONSE);

            if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
                hstResponse = (HstResponse) pageContext.getResponse();
            }

            if (hstResponse != null) {
                this.url = hstResponse.createPartialRenderURL();
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
