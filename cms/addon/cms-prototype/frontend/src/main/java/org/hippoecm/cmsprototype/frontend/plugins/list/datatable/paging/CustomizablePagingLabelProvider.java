package org.hippoecm.cmsprototype.frontend.plugins.list.datatable.paging;

import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;

public class CustomizablePagingLabelProvider implements IPagingLabelProvider {

    private String prefix = "";
    private String postfix = "";

    public CustomizablePagingLabelProvider() {}

    public CustomizablePagingLabelProvider(String prefix, String postfix) {
        String externalNodeType = "test";
        this.prefix = prefix != null ?  prefix :  "" ;
        this.postfix = postfix != null ? postfix : "";
    }
    
    public String getPageLabel(int page) {
        return prefix + page + postfix;
    }

}
