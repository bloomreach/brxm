package org.onehippo.cms7.essentials.components.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import org.onehippo.cms7.essentials.components.paging.DefaultPagination;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
@Provider
public class JaxbContextProvider implements ContextResolver<JAXBContext> {

    private static final Logger log = LoggerFactory.getLogger(JaxbContextProvider.class);
    private JAXBContext context;
    private String beansPackage;
    private List<Class<?>> classes;

    public JaxbContextProvider() throws Exception {
        log.debug("Creating custom JAXB ContextResolver");
    }

    public JAXBContext getContext(Class<?> objectType) {
        if (context == null) {
            try {
                if (Strings.isNullOrEmpty(beansPackage)) {
                    final List<Class<?>> allClasses = new ArrayList<>();
                    allClasses.add(IterablePagination.class);
                    allClasses.add(DefaultPagination.class);
                    if (classes != null) {
                        allClasses.addAll(classes);
                    }
                    final Class[] jaxbClasses = allClasses.toArray(new Class[allClasses.size()]);
                    context = JAXBContext.newInstance(jaxbClasses);
                    return context;
                }
                final List<Class<?>> allClasses = new ArrayList<>();
                allClasses.add(IterablePagination.class);
                allClasses.add(DefaultPagination.class);
                final ClassPath classPath = ClassPath.from(getClass().getClassLoader());
                final ImmutableSet<ClassPath.ClassInfo> topLevelClasses = classPath.getTopLevelClassesRecursive(beansPackage);
                for (ClassPath.ClassInfo topLevelClass : topLevelClasses) {
                    final String name = topLevelClass.getName();
                    final Class<?> clazz = Class.forName(name);
                    if (clazz.isAnnotationPresent(XmlRootElement.class)) {
                        allClasses.add(clazz);
                    }
                }
                if (classes != null) {
                    allClasses.addAll(classes);
                }
                final Class[] jaxbClasses = allClasses.toArray(new Class[allClasses.size()]);
                context = JAXBContext.newInstance(jaxbClasses);
            } catch (JAXBException | IOException | ClassNotFoundException e) {
                log.error("Error creating JAXB context:", e);
            }
        }
        return context;
    }

    public List<Class<?>> getClasses() {
        return classes;
    }

    public void setClasses(final List<Class<?>> classes) {
        this.classes = classes;
    }

    public void setBeansPackage(String beansPackage) {
        this.beansPackage = beansPackage;
    }

    public String getBeansPackage() {
        return beansPackage;
    }
}
