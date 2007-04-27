public class FacettedNavigationTest
{

  /*
            Session session = server.login();

            Node docs, node, root = session.getRootNode();
            docs = root.addNode("navigation");

            node = docs.addNode("bySourceTest1","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceTest2","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "section" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceTest3","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "section", "type" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceTest4","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "section", "type", "author" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceTest5","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "year", "month", "day", "author" });
            node.setProperty("hippo:docbase", "files");
            
            node = docs.addNode("bySourceTest6","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "author", "year", "month", "day" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceTest7","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "documentdate", "section" });
            node.setProperty("hippo:docbase", "files");
            

            node = docs.addNode("bySectionSource","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "section", "source", "year", "month", "author", "type" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySectionDate","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "section", "year", "month", "source", "author", "type" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceSection","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "section", "year", "month", "author", "type" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("bySourceDate","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "source", "year", "month", "section", "author", "type" });
            node.setProperty("hippo:docbase", "files");

            
            
            node = docs.addNode("byAuthorDate","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "author", "year", "month", "section", "source", "type" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("byAuthorSource","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "author", "section", "source", "year", "month", "type" });
            node.setProperty("hippo:docbase", "files");
            

            node = docs.addNode("byDateAuthor","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "year", "month", "day", "author", "section", "source", "type" });
            node.setProperty("hippo:docbase", "files");

            node = docs.addNode("byDateSection","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "year", "month", "day", "section", "source", "author", "type" });
            node.setProperty("hippo:docbase", "files");

            /*
            Node docs, node, root = session.getRootNode();
            docs = root.addNode("navigation");
            node = docs.addNode("byproduct","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "product", "brand" });
            node.setProperty("hippo:docbase", "documents");
	        // node.setProperty("hippo:search", new String[] { "has='ambilight'" });
            
            docs = root.addNode("documents");
            node = docs.addNode("42PF9831D");
            node.addMixin("mix:referenceable");
            node.setProperty("product","television");
            node.setProperty("brand","philips");
            node = docs.addNode("Bravia");
            node.addMixin("mix:referenceable");
            node.setProperty("product","television");
            node.setProperty("brand","sony");
            node = docs.addNode("DVP-FX810");
            node.addMixin("mix:referenceable");
            //node.setProperty("product",new String[] { "television", "dvdplayer" });
            node.setProperty("product","dvdplayer");
            node.setProperty("brand","sony");
            node = docs.addNode("spoon");
            node.addMixin("mix:referenceable");
            node.setProperty("product","dvdplayer");
            session.save();
            root = session.getRootNode();
            node = root.getNode("documents/42PF9831D");
            String uuid;
            if(!node.isNodeType("mix:referenceable")) {
              uuid = "";
              System.out.println("UUID NOT SUPPORTED");
            } else
              uuid = node.getUUID();
            System.out.println("\nEntire tree: "+session.getRootNode().getClass().getName());
            Utilities.dump(session.getRootNode());

            System.out.println("\nTelevisions:");
            node = root.getNode("navigation/byproduct/television/resultset");
            for(NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
              node = iter.nextNode();
              Utilities.dump(node);
            }

            System.out.println("\nTelevisions by Philips:");
            node = root.getNode("navigation/byproduct/television/philips/resultset");
            boolean found = false;
            for(NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
              node = iter.nextNode();
              try {
                if(uuid.equals(node.getUUID()))
                  found = true;
              } catch(UnsupportedRepositoryOperationException ex) {
              }
              Utilities.dump(node);
            }
            System.out.println(found ? "FOUND" : "NOT FOUND");

            if(false) {
              node = root.getNode("navigation/free/product[facet='television']/brand[facet='philips']");
              found = false;
              for(NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
                node = iter.nextNode();
                if(uuid.equals(node.getUUID()))
                  found = true;
                Utilities.dump(node);
              }
              System.out.println(found ? "FOUND" : "NOT FOUND");
            }
            

            session.save();
            session.logout();
            */
}
