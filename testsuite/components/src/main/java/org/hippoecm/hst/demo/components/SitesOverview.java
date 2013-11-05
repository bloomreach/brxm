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
package org.hippoecm.hst.demo.components;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SitesOverview extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(SitesOverview.class);

    private static volatile int counter = 0;


    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {


        String hostGroupName = request.getRequestContext().getResolvedMount().getMount().getVirtualHost().getHostGroupName();

        List<Mount> mountsForHostGroup = request.getRequestContext().getResolvedMount().getMount().getVirtualHost().getVirtualHosts().getMountsByHostGroup(hostGroupName);


        request.setAttribute("mounts", mountsForHostGroup);

    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {

        Session writableSession = null;
        try {
            writableSession = this.getPersistableSession(request);

            if(request.getParameter("touch") != null) {
                Node hstHostsNode = writableSession.getNode("/hst:hst/hst:hosts");
                hstHostsNode.setProperty("hst:pagenotfound", "error " + ++counter);
                writableSession.save();
            } else {
                String numberStr = request.getParameter("number");
                int numberToAdd = Integer.parseInt(numberStr);
                int tryToAdd = 1;

                String copyComponentsStr = request.getParameter("copycomponents");
                boolean copyComponents = copyComponentsStr != null ? Boolean.parseBoolean(copyComponentsStr) : true;

                while(numberToAdd > 0) {
                    //System.out.println("numberToAdd " + numberToAdd);
                    numberToAdd--;

                    // add a new host first: First check the first non-existing 'com' + integer host:
                    Node localHost = writableSession.getNode("/hst:hst/hst:hosts/dev-test-many/localhost");
                    while(localHost.hasNode("com" + tryToAdd)) {
                        tryToAdd++;
                    }

                    Node host = localHost.addNode("com" + tryToAdd, "hst:virtualhost");
                    Node mount = host.addNode("hst:root", "hst:mount");
                    mount.setProperty("hst:mountpoint", "/hst:hst/hst:sites/demosite-test-many"+tryToAdd);
                    mount.setProperty("hst:channelpath", "/hst:hst/hst:channels/demosite-test-many"+tryToAdd);
                    mount.setProperty("hst:alias", "mount"+tryToAdd);

                    // add a new hst:site
                    Node site = writableSession.getNode("/hst:hst/hst:sites").addNode("demosite-test-many"+tryToAdd, "hst:site");
                    site.setProperty("hst:content", "/content/documents/demosite");

                    // add a new hst:channel
                    Node channel = writableSession.getNode("/hst:hst/hst:channels").addNode("demosite-test-many"+tryToAdd, "hst:channel");
                    channel.setProperty("hst:channelinfoclass", "org.hippoecm.hst.demo.channel.DemoChannelInfo");
                    channel.setProperty("hst:defaultdevice", "default");
                    channel.setProperty("hst:name", "Test Many Site " + tryToAdd);
                    channel.setProperty("hst:type", "website");
                    Node channelInfo = channel.addNode("hst:channelinfo", "hst:channelinfo");
                    channelInfo.setProperty("exampleValue", "example " + tryToAdd);

                    // now copy the hst:configurations

                    Node config = writableSession.getNode("/hst:hst/hst:configurations").addNode("demosite-test-many" + tryToAdd, "hst:configuration");
                    String[] inherits = {"../democommon"};
                    config.setProperty("hst:inheritsfrom", inherits);

                    JcrUtils.copy(writableSession, "/hst:hst/hst:configurations/demosite-test-many/hst:sitemap", config.getPath() + "/hst:sitemap");
                    JcrUtils.copy(writableSession,"/hst:hst/hst:configurations/demosite-test-many/hst:sitemenus", config.getPath() + "/hst:sitemenus");

                    if (copyComponents) {
                        JcrUtils.copy(writableSession, "/hst:hst/hst:configurations/demosite-test-many/hst:pages", config.getPath() + "/hst:pages");
                        JcrUtils.copy(writableSession,"/hst:hst/hst:configurations/demosite-test-many/hst:templates",  config.getPath()  + "/hst:templates");
                    }

                    writableSession.save();

                }
            }

        } catch (RepositoryException e) {
            log.error(e.toString(),e);
        } catch (NumberFormatException e) {
            log.error(e.toString(),e);
        } finally {
            if(writableSession != null) {
                writableSession.logout();
            }
        }

    }

}
