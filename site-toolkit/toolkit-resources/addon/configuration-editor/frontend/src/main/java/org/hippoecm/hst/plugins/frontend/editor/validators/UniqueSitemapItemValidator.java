package org.hippoecm.hst.plugins.frontend.editor.validators;

import org.apache.wicket.validation.IValidatable;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext.HstSitemapContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;

public class UniqueSitemapItemValidator extends NodeUniqueValidator<SitemapItem> {
    private static final long serialVersionUID = 1L;

    private HstSitemapContext context;

    public UniqueSitemapItemValidator(BeanProvider<SitemapItem> provider, HstSitemapContext context) {
        super(provider);
        this.context = context;
    }

    @Override
    protected String getNewNodeName(IValidatable validatable, SitemapItem bean) {
        return context.encodeMatcher((String) validatable.getValue());
    }
}
