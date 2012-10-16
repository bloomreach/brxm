package org.onehippo.cms7.utilities.security;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * A security {@link ImmutableStore} which helps listing: users, groups, users of a certain group and groups of a certain
 * user
 *
 * Read only means that the interface only provides behavior of listing users and groups. No update behavior available
 */
// TODO I want to have a Store interface which provides read/write behavior but for now the scope only needs the read
// TODO only behavior
public interface ImmutableStore {

    /**
     * Retrieve the list of users defined in the system
     *
     * @return List of users defined in the system. Empty list if there are no users defined
     */
    List<String> getUsers() throws RepositoryException;

    /**
     * Retrieve the list of groups defined in the system
     *
     * @return List of groups defined in the system. Empty list if there are no groups defined
     */
    List<String> getGroups();

    /**
     * Retrieve the list of groups a user is member of
     *
     * @param userName User name of which a list of groups in which this user is member of will be retrieved
     * @return List of groups in which this user is member of. Empty list is user is not a member of any groups
     * @throws IllegalArgumentException When <code>userName</code> is empty string or null
     */
    List<String> getUserGroups(final String userName);

    /**
     * Retrieve the list of users of the given group
     *
     * @param groupName The group name of which a list of users, members of this group, will be retrieved
     * @return List of users which are members of this group
     * @throws IllegalArgumentException When <code>groupName</code> is empty string or null
     */
    List<String> getGroupUsers(final String groupName);

    /**
     * Check whether a user is a member of a certain group
     *
     * @param userName The user name
     * @param groupName The group name
     * @return <code>true</code> if user is a member of that group. <code>false</code> otherwise
     */
    boolean isMemberOf(final String userName, final String groupName);

    /**
     * This method should be called to allow the {@link ImmutableStore} implementations to release any resources which
     * might have been opened and used to retrieve the required information of both users and groups
     * <P>
     * It the responsibility of the Callers of objects of classes implementing {@link ImmutableStore} to call this method
     * to give the implementation classes a chance to do cleanup making sure not to leave any resources open which might
     * lead to memory leaks or degrading performance
     * </P>
     */
    void release();

}
