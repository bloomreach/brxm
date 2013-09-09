/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.hippoecm.repository.util.RepoUtils;

public class StatusServlet extends HttpServlet {

    public static final String REPOSITORY_ADDRESS_PARAM = "repository-address";
    public static final String DEFAULT_REPOSITORY_ADDRESS = "vm://";

    String repositoryLocation;

    public StatusServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        repositoryLocation = config.getInitParameter(REPOSITORY_ADDRESS_PARAM);
        if (repositoryLocation == null || repositoryLocation.equals("")) {
            repositoryLocation = config.getServletContext().getInitParameter(REPOSITORY_ADDRESS_PARAM);
        }
        if (repositoryLocation == null || repositoryLocation.equals("")) {
            repositoryLocation = DEFAULT_REPOSITORY_ADDRESS;
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        HippoRepository hippoRepository = null;

        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        writer.println("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        writer.println("<head><title>Hippo Repository Status</title>");
        writer.println("<style type=\"text/css\">");
        writer.println(" table.params {font-size:small}");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("  <h2>Hippo Repository Status</h2>");

        writer.println("  <h3>Version</h3>");
        try {
            InputStream istream = null;
            istream = getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF");
            String manifestSource = "war/ear manifest";
            if (istream == null) {
                File manifestFile = new File(getServletContext().getRealPath("/"), "META-INF/MANIFEST.MF");
                if(manifestFile.exists())
                    istream = new FileInputStream(manifestFile);
                manifestSource = "servlet manifest";
            }
            if (istream == null) {
                try {
                    istream = RepoUtils.getManifestURL(getClass()).openStream();
                    manifestSource = "jar";
                } catch (FileNotFoundException ex) {
                    manifestSource = "none";
                }
            }
            writer.println("    Manifest" + (manifestSource != null ? " from "+manifestSource : "") + ":<br/>");
            if (istream != null) {
                Manifest manifest = new Manifest(istream);
                Attributes atts = manifest.getMainAttributes();
                writer.println("    <table style=\"params\" summary=\"request parameters\">");
                writer.println("    <tr><td>Version</td><td>: " + atts.getValue("Implementation-Version") + "</td></tr>");
                writer.println("    <tr><td>Build</td><td>: " + atts.getValue("Implementation-Build") + "</td></tr>");
                writer.println("    </table>");
                if(manifestSource == null) {
                    writer.println("<!--");
                }
                writer.print("<pre>");
                ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
                manifest.write(tmpStream);
                writer.print(tmpStream.toString());
                writer.println("</pre>");
                if(manifestSource == null) {
                    writer.println("-->");
                }
            }
        } catch(Throwable ex) {
            writer.print("Error occured:<br/><pre>");
            writer.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
        }

        writer.println("  <h3>Repository descriptors</h3>");
        try {
            hippoRepository = HippoRepositoryFactory.getHippoRepository(repositoryLocation);
            if(hippoRepository != null) {
                Repository repository = hippoRepository.getRepository();
                writer.println("    <table style=\"params\" summary=\"request parameters\">");
                writer.println("    <tr><td>Repository</td><td>: " + repository.getDescriptor(Repository.REP_NAME_DESC) + "&nbsp;" + repository.getDescriptor(Repository.REP_VERSION_DESC) + "</td></tr>");
                writer.println("    <tr><td>Vendor</td><td>: " + repository.getDescriptor(Repository.REP_VENDOR_DESC) + "&nbsp;<a href=\""+repository.getDescriptor(Repository.REP_VENDOR_URL_DESC) +"\">" + repository.getDescriptor(Repository.REP_VENDOR_URL_DESC) + "</a>" + "</td></tr>");
                writer.println("    <tr><td>Specification</td><td>: " + repository.getDescriptor(Repository.SPEC_NAME_DESC) + "&nbsp;" + repository.getDescriptor(Repository.SPEC_VERSION_DESC) + "</td></tr>");
                writer.println("    </table>");
            } else {
                writer.println("No Hippo repository obtained");
            }
        } catch(Throwable ex) {
            writer.print("Error occured:<br/><pre>");
            writer.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
        }

        writer.println("  <h3>Repository basic access</h3>");
        try {
            if(hippoRepository != null) {
                try {
                    Session session = hippoRepository.login();
                    if(session.isLive()) {
                        writer.println("Repository online and accessible");
                    }
                    session.logout();
                } catch(LoginException ex) {
                    writer.println("No internal anonymous login permitted.");
                }
            } else {
                writer.println("No hippo repository present - skipped.");
            }
        } catch(Throwable ex) {
            writer.print("Error occured:<br/><pre>");
            writer.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
        }

        try {
            writer.println("  <h3>Resource usage</h3>");
            Runtime runtime = Runtime.getRuntime();
            writer.println("    <table style=\"params\" summary=\"request parameters\">");
            writer.println("    <tr><td>Memory limit</td><td align=\"right\">: " + runtime.maxMemory() + "</td><td align=\"right\">&nbsp;(&nbsp;" + ((runtime.maxMemory()+24288)/1048576) + "&nbsp;Mbyte )</td></tr>");
            writer.println("    <tr><td>Memory in use</td><td align=\"right\">: " + runtime.totalMemory() + "</td><td align=\"right\">&nbsp;(&nbsp;" + ((runtime.totalMemory()+24288)/1048576) + "&nbsp;Mbyte )</td></tr>");
            writer.println("    <tr><td>Memory available</td><td align=\"right\">: " + runtime.freeMemory() + "</td><td align=\"right\">&nbsp;(&nbsp;" + ((runtime.freeMemory()+24288)/1048576) + "&nbsp;Mbyte )</td></tr>");
            writer.println("    <tr><td>Processors</td><td align=\"right\">: " + runtime.availableProcessors() + "</td><td>&nbsp;</td></tr>");
            writer.println("    </table>");
        } catch(Throwable ex) {
            writer.print("Error occured:<br/><pre>");
            writer.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
        }

        writer.println("</body></html>");
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
