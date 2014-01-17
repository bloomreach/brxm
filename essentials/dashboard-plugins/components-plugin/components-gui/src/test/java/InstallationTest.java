import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.MemoryRepository;
import org.onehippo.cms7.essentials.dashboard.event.listeners.InstructionsEventListener;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * User: obourgeois
 * Date: 30-10-13
 */
@Ignore
public class InstallationTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(InstallationTest.class);
    @Inject
    private InstructionsEventListener listener;
    @Inject
    private PluginInstructionExecutor pluginInstructionExecutor;
    protected MemoryRepository repository;
    protected Session session;
    private Path projectRoot;
    private String oldProjectBaseDir;


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        oldProjectBaseDir = System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
        final URL resource = BaseTest.class.getClassLoader().getResource("project");
        log.info("Resource loader: " + BaseTest.class.getClassLoader().getResource("project"));
        log.info("Resource loader: " + BaseTest.class.getClassLoader().getResource("."));
        File root = new File(System.getProperty("java.io.tmpdir") + "hippo-testcase");
        root.mkdir();
        projectRoot = root.toPath();

        String resourceUrl = resource.toString();
        String JAR_URI_PREFIX = "jar:file:";
        if (resourceUrl.startsWith(JAR_URI_PREFIX)) {
            int bang = resourceUrl.indexOf("!");
            resourceUrl = resourceUrl.substring(JAR_URI_PREFIX.length(), bang);
            JarFile jarFile = new JarFile(resourceUrl);
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("project") && entry.isDirectory()) {
                    FileOutputStream out = new FileOutputStream(projectRoot.toFile());
                    InputStream in = jarFile.getInputStream(entry);

                    byte[] buffer = new byte[8 * 1024];
                    int s = 0;
                    while ((s = in.read(buffer)) > 0) {
                        out.write(buffer, 0, s);
                     }
                    IOUtils.closeQuietly(out);
                    IOUtils.closeQuietly(in);

                }
            }
        } else {
            FileUtils.copyDirectory(new File(resource.getFile()), projectRoot.toFile());
        }

        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, projectRoot.getRoot().toString());

        repository = new MemoryRepository();
        session = repository.getSession();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
//        try {
//            log.debug("Deleting dir : {}", projectRoot);
//            FileUtils.deleteDirectory(projectRoot.toFile());
//        } catch (IOException e) {
//            log.error("Cannot delete temporary directory");
//        }
        if (oldProjectBaseDir != null && !oldProjectBaseDir.isEmpty()) {
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, oldProjectBaseDir);
        }
    }

    @Test
    public void testExecute() throws Exception {

        final InputStream resourceAsStream = getClass().getResourceAsStream("/META-INF/instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        log.info("content {}", content);
        listener.reset();
        final Instructions instructions = InstructionParser.parseInstructions(content);
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        for (InstructionSet instructionSet : instructionSets) {
            pluginInstructionExecutor.execute(instructionSet, getContext());
        }

        assertEquals(7, listener.getNrInstructions());


    }
}
