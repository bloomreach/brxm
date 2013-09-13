package org.onehippo.cms7.essentials.site.components;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.essentials.site.beans.BaseDocument;
import org.onehippo.cms7.essentials.site.beans.NewsDocument;
import org.onehippo.cms7.essentials.site.beans.PluginDocument;
import org.onehippo.cms7.essentials.site.components.service.SearchCollection;
import org.onehippo.cms7.essentials.site.components.service.SearchService;
import org.onehippo.cms7.essentials.site.components.service.ctx.SiteSearchContext;

import com.google.common.base.Strings;


public class SearchComponent extends BaseComponent {




    private static final Class<? extends BaseDocument> NEWS_CLAZZ = NewsDocument.class;
    private static final Class<? extends BaseDocument> PLUGIN_CLAZZ = PluginDocument.class;

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {

        final String documentType = getComponentParameter("documentType");
        Class<? extends BaseDocument> clazz = PLUGIN_CLAZZ;
        if (!Strings.isNullOrEmpty(documentType) && documentType.equals("news")) {
            clazz = NEWS_CLAZZ;
        }
        final SiteSearchContext context = new SiteSearchContext(request, this, BaseDocument.class);
        context.setBeanMappingClass(clazz);
        SearchService<BaseDocument> service = new SearchService.Builder<>(context)
                .paging("page", "size")
                .orderBy("order")
                .query(getAnyParameter(request, "query"))
                .setAttributes()
                .build();
        final SearchCollection<BaseDocument> results = service.executeCollection();
        request.setAttribute("results", results);

    }

}
