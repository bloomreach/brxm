/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XPageSiteMapTreeItem extends XPageSiteMapBaseItem {

    public static final XPageSiteMapTreeItem root = new XPageSiteMapTreeItem();

    private XPageSiteMapTreeItem parent;
    private List<XPageSiteMapTreeItem> randomOrderXPageDescendants = new ArrayList<>();

    private Map<String, XPageSiteMapTreeItem> children = new HashMap<>();

    // Make sure to *intern* Strings as the memory usage most be as low as possible and since every branch has its
    // own in memory model but has a lot of the same Strings, it is important to share String instances
    @Override
    public void setPathInfo(final String pathInfo) {
        super.setPathInfo(pathInfo.intern());
    }


    // Make sure to *intern* Strings as the memory usage most be as low as possible and since every branch has its
    // own in memory model but has a lot of the same Strings, it is important to share String instances
    @Override
    public void setAbsoluteJcrPath(final String absoluteJcrPath) {
        super.setAbsoluteJcrPath(absoluteJcrPath.intern());
    }

    public XPageSiteMapTreeItem getParent() {
        return parent;
    }

    public void setParent(final XPageSiteMapTreeItem parent) {
        this.parent = parent;
    }

    public Map<String, XPageSiteMapTreeItem> getChildren() {
        return children;
    }

    public void setChildren(final Map<String, XPageSiteMapTreeItem> children) {
        this.children = children;
    }

    public List<XPageSiteMapTreeItem> getRandomOrderXPageDescendants() {
        return randomOrderXPageDescendants;
    }

    /**
     * Make this XPageSiteMapTreeItem partly immutable and optimizes storage
     */
    void optimize() {
        if (randomOrderXPageDescendants == null || randomOrderXPageDescendants.isEmpty()) {
            randomOrderXPageDescendants = Collections.emptyList();
        } else {
            randomOrderXPageDescendants = Collections.unmodifiableList(randomOrderXPageDescendants);
        }
        if (children.isEmpty()) {
            children = Collections.emptyMap();
        } else {
            children = Collections.unmodifiableMap(children);
            children.values().stream().forEach(child -> child.optimize());
        }
    }

}
