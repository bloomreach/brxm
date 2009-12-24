package org.hippoecm.repository.facetnavigation;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class FacetViewHelper {

    
    public static void traverse(Node navigation) throws RepositoryException {
        traverse(navigation, "", 0);
    }

    public static void traverse(Node navigation, String indent, int depth) throws RepositoryException {
        depth++;
        if(depth > 12) {
            return;
        }
        String countStr = "";
        if(navigation.hasProperty("hippo:count")) {
            countStr = " [" +  navigation.getProperty("hippo:count").getLong() + "]";
        }
        System.out.println(indent + navigation.getName() +  countStr);
        //if(!navigation.getName().equals("hippo:resultset")) { 
            NodeIterator it = navigation.getNodes();
            indent += "\t";
            while(it.hasNext()) {
                traverse(it.nextNode(), indent, depth);
            }
        //}
    }
}
