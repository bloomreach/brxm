package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

public class SitemapItem extends EditorBean {
    private static final long serialVersionUID = 1L;

    public SitemapItem(JcrNodeModel model) {
        super(model);
    }

    String matcher;
    String contentPath;
    String page;

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }


}
