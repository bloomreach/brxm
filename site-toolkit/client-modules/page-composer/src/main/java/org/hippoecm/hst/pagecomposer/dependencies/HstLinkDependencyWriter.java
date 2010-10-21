package org.hippoecm.hst.pagecomposer.dependencies;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;

import java.io.IOException;
import java.io.Writer;

public class HstLinkDependencyWriter extends StringWriter {

    private HstRequestContext context;
    private Writer writer;
    private HstLinkCreator creator;
    private SiteMount siteMount;

    public HstLinkDependencyWriter(HstRequestContext context, Writer writer) {
        super();
        this.context = context;
        this.writer = writer;
        creator = context.getHstLinkCreator();
        siteMount = context.getResolvedSiteMount().getSiteMount();
    }

    @Override
    protected void write(String src) {
        try {
            writer.append(src);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String parse(String path) {
        return creator.create(path, siteMount, true).toUrlForm(context, false);
    }
}
