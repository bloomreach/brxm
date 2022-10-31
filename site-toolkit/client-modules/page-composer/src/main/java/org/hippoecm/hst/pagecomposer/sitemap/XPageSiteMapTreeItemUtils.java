/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

import java.util.Collection;
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.security.StandardPermissionNames.HIPPO_AUTHOR;

public class XPageSiteMapTreeItemUtils {

    private final static Logger log = LoggerFactory.getLogger(XPageSiteMapTreeItemUtils.class.getName());

    public static Optional<XPageSiteMapShallowItem> getReadableItem(final XPageSiteMapTreeItem root, final Session userSession, final String pathInfo) {
        Optional<XPageSiteMapTreeItem> pathInfoItem = findPathInfoItem(root, pathInfo);
        if (!pathInfoItem.isPresent()) {
            return Optional.empty();
        }
        return getReadableXPageSiteMapShallowItem(userSession, pathInfoItem.get());
    }

    public static XPageSiteMapShallowItem[] getReadableChildren(final XPageSiteMapTreeItem root,
                                                                final Session userSession,
                                                                final String pathInfo) {

        Optional<XPageSiteMapTreeItem> pathInfoItem = findPathInfoItem(root, pathInfo);
        if (!pathInfoItem.isPresent()) {
            return new XPageSiteMapShallowItem[0];
        }

        final Collection<XPageSiteMapTreeItem> children = pathInfoItem.get().getChildren().values();

        return children.stream().map(child -> getReadableXPageSiteMapShallowItem(userSession, child))
                .filter(item -> item.isPresent())
                .map(item -> item.get())
                .toArray(XPageSiteMapShallowItem[]::new);

    }

    public static boolean hasReadableChildren(final XPageSiteMapTreeItem root,
                                              final Session userSession,
                                              final String pathInfo) {

        Optional<XPageSiteMapTreeItem> pathInfoItem = findPathInfoItem(root, pathInfo);
        if (!pathInfoItem.isPresent()) {
            return false;
        }

        final Collection<XPageSiteMapTreeItem> children = pathInfoItem.get().getChildren().values();

        return children.stream().map(child -> getReadableXPageSiteMapShallowItem(userSession, child))
                .filter(item -> item.isPresent())
                .findAny().isPresent();
    }

    private static Optional<XPageSiteMapTreeItem> findPathInfoItem(final XPageSiteMapTreeItem root, final String pathInfo) {
        XPageSiteMapTreeItem current = root;

        if (StringUtils.isNotEmpty(pathInfo) && !pathInfo.equals("/")) {
            // find the starting point for pathInfo
            final String[] segments = pathInfo.split("/");
            for (String segment : segments) {
                current = current.getChildren().get(segment);
                if (current == null) {
                    log.info("No sitemap item found for path '{}'", pathInfo);
                    return Optional.empty();
                }
            }
        }
        return Optional.of(current);
    }

    public static Optional<XPageSiteMapShallowItem> getReadableXPageSiteMapShallowItem(final Session userSession, final XPageSiteMapTreeItem item) {

        XPageSiteMapShallowItem xPageSiteMapShallowItem = null;

        try {
            final AccessControlManager accessControlManager = userSession.getAccessControlManager();
            final Privilege[] requiredPrivilege = new Privilege[]
                    {accessControlManager.privilegeFromName(HIPPO_AUTHOR)};

            final String absoluteJcrPath = item.getAbsoluteJcrPath();
            if (absoluteJcrPath != null) {
                if (accessControlManager.hasPrivileges(absoluteJcrPath, requiredPrivilege)) {
                    xPageSiteMapShallowItem = new XPageSiteMapShallowItem(item);
                } else {
                    log.info("Skipping '{}' for user '{}' as the user does not have privilege '{}' on '{}'",
                            absoluteJcrPath, userSession.getUserID(), HIPPO_AUTHOR, absoluteJcrPath);
                    return Optional.empty();
                }
            }

            boolean expandable = isExpandable(accessControlManager, requiredPrivilege, item);

            if (xPageSiteMapShallowItem == null) {
                if (expandable) {
                    // we are dealing with a 'structural' sitemap item not backed by an XPage document. We know this item
                    // must be visible as it is expandable because there is a readable descendant
                    xPageSiteMapShallowItem = new XPageSiteMapShallowItem(item.getPathInfo());
                } else {
                    // not backed by XPage doc (absoluteJcrPath is null) and not expandable (user cannot read children),
                    // thus can be ignored
                    return Optional.empty();
                }
            }

            xPageSiteMapShallowItem.setExpandable(expandable);

        } catch (RepositoryException e) {
            log.error("Exception while creating XPageSiteMapShallowItem", e);
            return Optional.empty();
        }

        return Optional.of(xPageSiteMapShallowItem);

    }

    /**
     * <p>
     * Checks whether {@code current} has at least one readable descendant document, and if found at least one, returns
     * true
     * </p>
     * <p>
     * As this is based on a privilege check on XPage docs for the descendant XPageSiteMapTreeItem's, and since
     * privileges are typically configured hierarchically on folders, it is important to NOT check for privileges
     * following a depth first traversal. Even bread first traversal is NOT optimal : if a user is not allowed to read
     * below some folder as privileges are frequently hierarchically oriented, we should avoid trying eg 10.000 xpage
     * docs which might be all unreadable : instead, we need random selected descendants to attempt a privilege access,
     * such that it is less likely some folder is fully checked while all documents do not meet the required privileges
     * </p>
     */
    private static boolean isExpandable(final AccessControlManager accessControlManager,
                                        final Privilege[] requiredPrivilege, final XPageSiteMapTreeItem check) {
        return check.getRandomOrderXPageDescendants().stream()
                .filter(descendant -> {
                    try {
                        return accessControlManager.hasPrivileges(descendant.getAbsoluteJcrPath(), requiredPrivilege);
                    } catch (RepositoryException e) {
                        log.error("Exception while checking privilege for '{}'", check.getAbsoluteJcrPath());
                        return false;
                    }
                })
                .findAny().isPresent();
    }

}
