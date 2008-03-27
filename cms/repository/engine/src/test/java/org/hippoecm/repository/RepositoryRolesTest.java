package org.hippoecm.repository;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.security.UserPrincipal;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.XASessionImpl;
import org.hippoecm.repository.security.principals.RolePrincipal;
import org.hippoecm.repository.decorating.SessionDecorator;

public class RepositoryRolesTest extends TestCase {

    private HippoRepository server;
    private Session serverSession;
    private Node users;
    private Node groups;
    private Node roles;

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private static final String USERS_PATH = "hippo:configuration/hippo:users";
    private static final String GROUPS_PATH = "hippo:configuration/hippo:groups";
    private static final String ROLES_PATH = "hippo:configuration/hippo:roles";

    private static final String TESTUSER_ID = "testuser";
    private static final String TESTUSER_PASS = "testpass";

    private static final String TESTGROUP_MEMBER = "group1";
    private static final String TESTGROUP_NOTMEMBER = "group2";
    

    private static final String TESTROLE_USER = "userrole";
    private static final String TESTROLE_GROUP1 = "group1role";
    private static final String TESTROLE_GROUP2 = "group2role";
    
    public void setUp() throws RepositoryException, IOException {
        server = HippoRepositoryFactory.getHippoRepository();
        serverSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        // create user config path
        Node node = serverSession.getRootNode();
        StringTokenizer tokenizer = new StringTokenizer(USERS_PATH, "/");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            node = node.addNode(token);
        }


        // create test roles
        roles = serverSession.getRootNode().getNode(ROLES_PATH);
        Node testrole;
        testrole = roles.addNode(TESTROLE_USER, HippoNodeType.NT_ROLE);
        testrole = roles.addNode(TESTROLE_GROUP1, HippoNodeType.NT_ROLE);
        testrole = roles.addNode(TESTROLE_GROUP2, HippoNodeType.NT_ROLE);
        
        // create test user
        users = serverSession.getRootNode().getNode(USERS_PATH);
        Node testuser = users.addNode(TESTUSER_ID, HippoNodeType.NT_USER);
        testuser.setProperty("hippo:password", TESTUSER_PASS);
        testuser.setProperty(HippoNodeType.HIPPO_ROLES, new String[]{TESTROLE_USER});
        
        // create test groups
        groups = serverSession.getRootNode().getNode(GROUPS_PATH);
        Node testgroup;
        testgroup = groups.addNode(TESTGROUP_MEMBER, HippoNodeType.NT_GROUP);
        testgroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[]{TESTUSER_ID});
        testgroup.setProperty(HippoNodeType.HIPPO_ROLES, new String[]{TESTROLE_GROUP1});
        
        testgroup = groups.addNode(TESTGROUP_NOTMEMBER, HippoNodeType.NT_GROUP);
        testgroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[]{SYSTEMUSER_ID});
        testgroup.setProperty(HippoNodeType.HIPPO_ROLES, new String[]{TESTROLE_GROUP2});
        
        
        // assign roles to user and groups
        

        
        
        serverSession.save();
    }
    
    public void tearDown() throws RepositoryException {
        if (users != null) {
            users.remove();
        }
        if (groups != null) {
            groups.remove();
        }
        if (serverSession != null) {
            serverSession.save();
            serverSession.logout();
        }
        if (server != null) {
            server.close();
        }
    }
    
    private Set<Principal> getPrincipals() throws RepositoryException {
        Set<Principal> principals = new HashSet<Principal>();
        XASessionImpl userSession = (XASessionImpl) SessionDecorator.unwrap(server.login(TESTUSER_ID, TESTUSER_PASS.toCharArray()));
        principals = userSession.getUserPrincipals();
        return principals;
    }
    
    public void testUserPrincipal() throws RepositoryException {
        boolean hasPrincipal = false;
        for (Principal p : getPrincipals()) {
            if (p instanceof UserPrincipal) {
                UserPrincipal up = (UserPrincipal) p;
                if (up.getName().equals(TESTUSER_ID)) {
                    hasPrincipal = true;
                }
            }
        }
        assertTrue(hasPrincipal);
    }
    
    public void testUserRole() throws RepositoryException {
        boolean hasPrincipal = false;
        for (Principal p : getPrincipals()) {
            if (p instanceof RolePrincipal) {
                RolePrincipal rp = (RolePrincipal) p;
                if (rp.getName().equals(TESTROLE_USER)) {
                    hasPrincipal = true;
                }
            }
        }
        assertTrue(hasPrincipal);
    }
    
    public void testGroupRole() throws RepositoryException {
        boolean hasPrincipal = false;
        for (Principal p : getPrincipals()) {
            if (p instanceof RolePrincipal) {
                RolePrincipal rp = (RolePrincipal) p;
                if (rp.getName().equals(TESTROLE_GROUP1)) {
                    hasPrincipal = true;
                }
            }
        }
        assertTrue(hasPrincipal);
    }

    public void testNotGroupRole() throws RepositoryException {
        boolean hasPrincipal = false;
        for (Principal p : getPrincipals()) {
            if (p instanceof RolePrincipal) {
                RolePrincipal rp = (RolePrincipal) p;
                if (rp.getName().equals(TESTROLE_GROUP2)) {
                    hasPrincipal = true;
                }
            }
        }
        assertFalse(hasPrincipal);
    }
}
