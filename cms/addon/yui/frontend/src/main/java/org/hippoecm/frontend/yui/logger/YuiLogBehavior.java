package org.hippoecm.frontend.yui.logger;

import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.wicketstuff.yui.YuiHeaderContributor;

public class YuiLogBehavior extends AbstractHeaderContributor {
    private static final long serialVersionUID = 1L;

    @Override
    public IHeaderContributor[] getHeaderContributors() {
        IHeaderContributor[] dependencies = YuiHeaderContributor.forModule("logger").getHeaderContributors();
        IHeaderContributor[] logger = new IHeaderContributor[dependencies.length + 1];
        System.arraycopy(dependencies, 0, logger, 0, dependencies.length);
        logger[logger.length - 1] = HeaderContributor.forJavaScript(YuiLogBehavior.class, "YuiLogBehavior.js");
        return logger;
    }

}
