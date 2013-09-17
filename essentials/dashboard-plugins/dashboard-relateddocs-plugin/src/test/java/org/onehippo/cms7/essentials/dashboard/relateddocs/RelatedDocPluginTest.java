package org.onehippo.cms7.essentials.dashboard.relateddocs;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @version "$Id$"
 */
@Ignore("Use MemoryRepo")
public class RelatedDocPluginTest {

    private static Logger log = LoggerFactory.getLogger(RelatedDocPluginTest.class);
    private Session session;

    @Before
    public void setUp() throws Exception {
        try {
            final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            session = repository.login("admin", "admin".toCharArray());
        } catch (Exception e) {
            log.error("Error creating repository connection");
            assumeTrue(false);
        }


    }

    /*Todo move to API: */
    @Test
    public void testPrototypes() throws Exception {
        assertNotNull(HippoNodeUtils.getPrimaryTypes(session, "new-document"));
        assertTrue(!HippoNodeUtils.getPrimaryTypes(session, "new-document").isEmpty());

    }

    @Test
    public void testAddAndRemoveRelatedDocsPlugins() throws Exception {
        assertTrue(addRelatedDocsPluginToNodeType("hippoplugins:newsdocument", null));
        final Node newsDocument = session.getNode("/hippo:namespaces/hippoplugins/newsdocument/editor:templates/_default_");
        assertTrue(newsDocument.hasNode("relateddocs"));
        assertTrue(newsDocument.hasNode("relateddocssuggest"));

        assertTrue(removeRelatedDocsPluginToNodeType("hippoplugins:newsdocument"));

        assertTrue(addRelatedDocsPluginToNodeType("hippoplugins:plugin", null));
        Node plugin = session.getNode("/hippo:namespaces/hippoplugins/plugin/editor:templates/_default_");
        assertTrue(plugin.hasNode("relateddocs"));
        assertTrue(plugin.hasNode("relateddocssuggest"));

        assertTrue(removeRelatedDocsPluginToNodeType("hippoplugins:plugin"));

        assertFalse(plugin.hasNode("relateddocs"));
        assertFalse(plugin.hasNode("relateddocssuggest"));

        assertTrue(addRelatedDocsPluginToNodeType("hippoplugins:plugin", Prefer.RIGHT));

        assertTrue(removeRelatedDocsPluginToNodeType("hippoplugins:plugin"));
    }

    public enum Prefer {
        LEFT("left"), RIGHT("right");

        String prefer;

        private Prefer(String prefer) {
            this.prefer = prefer;
        }

        public String getPrefer() {
            return prefer;
        }
    }


    public boolean addRelatedDocsPluginToNodeType(String type, Prefer prefer) {
        final Session mySession = this.session;
        InputStream in = null;
        try {
            Node docType;
            if (type.contains(":")) {
                docType = mySession.getNode("/hippo:namespaces/" + type.replace(':', '/'));
            } else {
                docType = mySession.getNode("/hippo:namespaces/system/" + type);
            }
            if (docType.hasNode("editor:templates/_default_/root")) {
                final Node root = docType.getNode("editor:templates/_default_/root");
                if (root.hasProperty("plugin.class")) {
                    final String pluginClazz = root.getProperty("plugin.class").getString();
                    final HippoSession hippoSession = (HippoSession) mySession;
                    RelatedDocsPlugin.PluginType pluginType = RelatedDocsPlugin.PluginType.get(pluginClazz);
                    String absPath = docType.getNode("editor:templates").getPath();
                    boolean addedRelatedDocs = false;
                    switch (pluginType) {
                        case LISTVIEWPLUGIN:
                            in = getClass().getResourceAsStream("/listview.xml");
                            hippoSession.importDereferencedXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                            addedRelatedDocs = true;
                            break;
                        case TWOCOLUMN:
                            if (prefer == null) {
                                prefer = Prefer.RIGHT;
                            }
                            in = getClass().getResourceAsStream("/two_column_" + prefer.getPrefer() + ".xml");
                            hippoSession.importDereferencedXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                            addedRelatedDocs = true;
                            break;
                    }
                    hippoSession.save();
                    return addedRelatedDocs;
                }
            }
        } catch (RepositoryException | IOException e) {
            log.error("Error adding related doc nodes", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return false;
    }

    public boolean removeRelatedDocsPluginToNodeType(String type) {
        final Session mySession = this.session;
        try {
            Node docType;
            if (type.contains(":")) {
                docType = mySession.getNode("/hippo:namespaces/" + type.replace(':', '/'));
            } else {
                docType = mySession.getNode("/hippo:namespaces/system/" + type);
            }
            if (docType.hasNode("editor:templates/_default_")) {
                boolean hasRelatedDocs = false;
                final Node _default_ = docType.getNode("editor:templates/_default_");
                final NodeIterator it = _default_.getNodes();
                while (it.hasNext()) {
                    final Node node = it.nextNode();
                    if (node.hasProperty("plugin.class")) {
                        final String pluginClass = node.getProperty("plugin.class").getString();
                        if (pluginClass.equals("org.onehippo.forge.relateddocs.editor.RelatedDocsSuggestPlugin") || pluginClass.equals("org.onehippo.forge.relateddocs.editor.RelatedDocsPlugin")) {
                            hasRelatedDocs = true;
                            node.remove();
                        }
                    }
                }
                if (_default_.hasNode("translator/hippostd:translations")) {
                    final Node translations = _default_.getNode("translator/hippostd:translations");
                    final NodeIterator tit = translations.getNodes();
                    while (tit.hasNext()) {
                        final Node translation = tit.nextNode();
                        if (translation.getName().equals("relateddocs") || translation.getName().equals("relateddocssuggest")) {
                            translation.remove();
                        }
                    }
                }
                mySession.save();
                return hasRelatedDocs;
            }
        } catch (RepositoryException e) {
            log.error("Error removing related doc nodes", e);
        }
        return false;
    }

    @After
    public void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
    }
}
