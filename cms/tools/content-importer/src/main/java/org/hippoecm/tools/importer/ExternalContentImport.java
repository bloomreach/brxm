/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.tools.importer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.Context;
import org.hippoecm.tools.importer.api.Converter;
import org.hippoecm.tools.importer.api.ImportException;
import org.hippoecm.tools.importer.api.Mapper;
import org.hippoecm.tools.importer.api.Mapping;
import org.hippoecm.tools.importer.content.FolderContent;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class ExternalContentImport {

    final static String SVN_ID = "$Id$";

    private static final String JCR_RMI_URL = "repository.rmiurl";
    private static final String JCR_USER = "repository.username";
    private static final String JCR_PASS = "repository.password";
    private static final String JCR_PATH = "repository.path";

    private static final String FILE_PATH = "filesystem.path";

    private static final String MAPPER_PREFIX = "mapper";
    private static final String CONVERTER_PREFIX = "converter";

    private final String rmiurl;
    private final String username;
    private final String password;
    private String repopath;
    private String filepath;
    private boolean overwrite;

    private final Session session;
    private final Node baseNode;

    private Mapper mapper;
    private Map<String, Converter> converters;

    private static Logger log = LoggerFactory.getLogger(ExternalContentImport.class);

    public ExternalContentImport(Configuration config) throws ImportException {
        rmiurl = config.getString(JCR_RMI_URL);
        username = config.getString(JCR_USER);
        password = config.getString(JCR_PASS);
        repopath = config.getString(JCR_PATH);
        filepath = config.getString(FILE_PATH);

        overwrite = config.getBoolean("overwrite", true);
        log.info("Overwriting existing content: " + overwrite);

        try {
            // document mapper
            String mapperClassName = config.getString(MAPPER_PREFIX);
            if (mapperClassName == null || "".equals(mapperClassName)) {
                throw new ImportException("No mapper class specified");
            }
            mapper = (Mapper) Class.forName(mapperClassName).newInstance();
            mapper.setup(config.subset(MAPPER_PREFIX));

            converters = new TreeMap<String, Converter>();
            Iterator<String> handlerIter = config.getKeys(CONVERTER_PREFIX);
            while (handlerIter.hasNext()) {
                String key = handlerIter.next();
                String[] elements = StringUtils.split(key, '.');
                if (elements.length == 2) {

                    // document converter
                    String importerClassName = config.getString(key);
                    if (importerClassName == null || "".equals(importerClassName)) {
                        throw new ImportException("No importer class specified");
                    }
                    Converter importer = (Converter) Class.forName(importerClassName).newInstance();
                    importer.setup(config.subset(key));

                    for (String nodeType : importer.getNodeTypes()) {
                        converters.put(nodeType, importer);
                    }
                }
            }

        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("ContentImporter class not found: " + e.getMessage());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("ContentImporter class not instantiated: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("ContentImporter class access exception: " + e.getMessage());
        }

        try {
            // add trailing slash
            if (!repopath.endsWith("/")) {
                repopath = repopath + "/";
            }

            File file = new File(filepath);
            log.info("Repository : " + rmiurl);
            log.info("user       : " + username);
            log.info("File path  : " + file.getCanonicalPath());
            log.info("Repo path : " + repopath);

            // test and setup connection and login
            HippoRepository repository;
            // get the repository
            repository = HippoRepositoryFactory.getHippoRepository(rmiurl);

            // login and get session
            session = repository.login(new SimpleCredentials(username, password.toCharArray()));

            Context context = new ContextImpl(session.getRootNode(), null, overwrite);
            baseNode = context.createPath(repopath);

            Content content = new FolderContent(file, filepath);
            Iterator<Content> contents = content.getChildren();
            while (contents.hasNext()) {
                Content childContent = contents.next();
                contentImport(childContent);
            }
            session.save();

        } catch (IOException ex) {
            throw new ImportException("Import failed due to IO exception", ex);
        } catch (RepositoryException ex) {
            throw new ImportException("Import failed", ex);
        }
    }

    /**
     * Recursively import from the current file
     * @param file
     * @throws RepositoryException 
     */
    private void contentImport(Content content) throws IOException, RepositoryException, ImportException {
        Mapping mapping = mapper.map(content);
        Converter converter = converters.get(mapping.getNodeType());

        Context context = new ContextImpl(baseNode, mapping, overwrite);

        Node childNode = converter.convert(context, content);

        if (content.isFolder()) {
            Iterator<Content> contents = content.getChildren();
            while (contents.hasNext()) {
                Content childContent = contents.next();
                contentImport(childContent);
            }
            session.save();
        }
    }

}
