package org.hippoecm.hst.tags;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstURL;

public class HstURLTag extends BaseHstURLTag {

    private static final long serialVersionUID = 1L;

    @Override
    protected HstURL getUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUrl(HstURL url) {
        // TODO Auto-generated method stub

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
