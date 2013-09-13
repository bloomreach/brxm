package org.onehippo.cms7.essentials.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.onehippo.cms7.essentials.shared.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class MavenModule {

    static final Logger log = LoggerFactory.getLogger(MavenModule.class);

    public static final String MODEL_FILE = "pom.xml";

    private static final Text NEWLINE = new Text("\n");

    private static final String[] DEPENDENCY_PROPERTIES = new String[]{
            "groupId", "artifactId", "version", "scope"};

    private final Document document;
    private final XMLParser reader;
    private final File directory;

    public MavenModule(final File directory) throws IOException {
        this.directory = directory;
        this.reader = new XMLParser();
        this.document = reader.parse(new XMLIOSource(new FileInputStream(new File(directory, MODEL_FILE))));
    }

    public HashMap<String, MavenModule> getChildModels() throws IOException {
        HashMap<String, MavenModule> models = new HashMap<String, MavenModule>();
        List<String> modules = getModules(document.getRootElement());
        if (modules.isEmpty()) {
            Element activeProfile = getDefaultProfile();
            if (activeProfile != null) {
                modules = getModules(activeProfile);
            }
        }
        for (String module : modules) {
            MavenModule childModel = new MavenModule(new File(this.directory, module));
            models.put(module, childModel);
        }
        return models;
    }

    public Element getDefaultProfile() {
        Element root = document.getRootElement();
        final Element profilesEl = root.getChild("profiles");
        if (profilesEl != null) {
            for (Element profileEl : profilesEl.getChildren("profile")) {
                if (profileEl.getChild("id").getTrimmedText().equals("default")) {
                    return profileEl;
                }
            }
        }
        return null;
    }

    public void flush() throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(directory, MODEL_FILE)));
        writer.write(document.toString());
        writer.close();
    }

    private List<String> getModules(Element root) {
        List<String> modules = new ArrayList<String>();
        final Element modulesEl = root.getChild("modules");
        if (modulesEl != null) {
            final List<Element> moduleEls = modulesEl.getChildren("module");
            for (Element moduleEl : moduleEls) {
                modules.add(moduleEl.getTrimmedText());
            }
        }
        return modules;
    }

    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Element dependenciesEl = document.getRootElement().getChild("dependencies");
        if (dependenciesEl != null) {
            for (Element child : dependenciesEl.getChildren()) {
                if ("dependency".equals(child.getName())) {
                    Dependency dependency = new Dependency();
                    try {
                        for (String property : DEPENDENCY_PROPERTIES) {
                            Element propertyEl = child.getChild(property);
                            if (propertyEl != null) {
                                PropertyUtils.setProperty(dependency, property, propertyEl.getTrimmedText());
                            }
                        }
                    } catch (IllegalAccessException e) {
                        log.error("fail", e);
                    } catch (InvocationTargetException e) {
                        log.error("fail", e);
                    } catch (NoSuchMethodException e) {
                        log.error("fail", e);
                    }
                    dependencies.add(dependency);
                }
            }
        }
        return dependencies;
    }

    public void addDependency(final Dependency mvnDependency) {
        Element dependencies = document.getRootElement().getChild("dependencies");
        int index = dependencies.nodeCount();
        final List<Element> children = dependencies.getChildren();
        if (children.size() > 0) {
            index = dependencies.nodeIndexOf(children.get(children.size() - 1)) + 1;
        }
        final Element dependency = createDependencyElement(mvnDependency);
        dependencies.addNode(index++, NEWLINE)
                .addNode(index++, new Text("    "))
                .addNode(index++, dependency);
    }

    private Element createDependencyElement(final Dependency mvnDependency) {
        final Element dependency = new Element("dependency").addNode(NEWLINE);
        try {
            for (String property : DEPENDENCY_PROPERTIES) {
                final Object value = PropertyUtils.getProperty(mvnDependency, property);
                if (value != null) {
                    dependency.addNode(new Text("      "))
                            .addNode(new Element(property).setText(value.toString()))
                            .addNode(NEWLINE);
                }
            }
        } catch (Exception e) {
            log.error("fail", e);
        }
        dependency.addNode(new Text("    "));
        return dependency;
    }

}
