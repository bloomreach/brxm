package org.onehippo.cms7.essentials.components.model;

import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * @version $Id$
 */
public class RepositoryMenuItem {

    private HippoBean bean;
    private List<RepositoryMenuItem> children;
    private boolean isFolder;
    private boolean isActive;

    public RepositoryMenuItem(HippoBean bean, HippoBean contentBean) {
        this.bean = bean;
        isFolder = bean.isHippoFolderBean();
        isActive = contentBean != null && bean.isSelf(contentBean);
    }

    public HippoBean getBean() {
        return bean;
    }

    public void setBean(HippoBean bean) {
        this.bean = bean;
    }

    public List<RepositoryMenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<RepositoryMenuItem> children) {
        isFolder = true;
        this.children = children;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public String getTitle() throws RepositoryException {
        if (isFolder) {
            return bean.getName();
        } else {
            String namespace = bean.getNode().getPrimaryNodeType().getName().split(":")[0];
            String title = bean.getProperty(namespace + ":title");
            if (title != null) {
                return title;
            } else {
                return bean.getName();
            }
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
