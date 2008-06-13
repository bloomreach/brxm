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
package ${package}.example.addon;

import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.IClusterable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.Model;
import ${package}.frontend.model.JcrNodeModel;
import ${package}.frontend.sa.plugin.IPlugin;
import ${package}.frontend.sa.plugin.IPluginContext;
import ${package}.frontend.sa.plugin.config.IPluginConfig;
import ${package}.frontend.sa.plugin.impl.RenderPlugin;
import ${package}.frontend.sa.service.IMessageListener;
import ${package}.frontend.sa.service.Message;
import ${package}.frontend.sa.service.render.ModelReference;
import ${package}.frontend.sa.service.topic.TopicService;

public class MyPlugin extends RenderPlugin implements IPlugin, IMessageListener, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final transient Logger log = LoggerFactory.getLogger(MyPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;
    private TopicService topic;

    public MyPlugin() {
        add(new AjaxLink("activate", new Model("Report problem")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    JcrNodeModel model = (JcrNodeModel) MyPlugin.this.getModel();
                    try {
                        Properties props = new Properties();
                        props.put("mail.smtp.host", MyPlugin.this.context.getProperties().get("mail.smtp.host"));
                        Session session = Session.getInstance(props, null);
                        InternetAddress from = new InternetAddress(MyPlugin.this.context.getProperties().get("mail.sender").toString());
                        InternetAddress to = new InternetAddress(MyPlugin.this.context.getProperties().get("mail.recipient").toString());
                        MimeMessage message = new MimeMessage(session);
                        message.setFrom(from);
                        message.addRecipient(MimeMessage.RecipientType.TO, to);
                        message.setSubject(MyPlugin.this.context.getProperties().get("mail.subject").toString());
                        StringBuffer sb = new StringBuffer();
                        sb.append("There has been report of a problem");
                        if (model != null) {
                            sb.append(" regarding the selected node ");
                            sb.append(model.getNode().getPath());
                        }
                        sb.append(".");
                        message.setText(new String(sb));
                        Transport.send(message);
                    } catch (AddressException ex) {
                        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    } catch (MessagingException ex) {
                        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    }
                } catch (RepositoryException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        });
    }

    @Override
    public void init(IPluginContext context, IPluginConfig properties) {
        super.init(context, properties);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void start(IPluginContext context) {
        super.start(context);
        this.context = context;
        config = context.getProperties();

        if (config.get(RenderPlugin.MODEL_ID) != null) {
            topic = new TopicService(config.getString(RenderPlugin.MODEL_ID));
            topic.addListener(this);
            topic.init(context);
        } else {
            log.warn("");
        }
    }

    @Override
    public void stop() {
        if (topic != null) {
            topic.destroy();
            topic = null;
        }
    }

    public void onMessage(Message message) {
        switch (message.getType()) {
            case ModelReference.SET_MODEL:
                // setModel((JcrNodeModel) ((ModelReference.ModelMessage) message).getModel());
                break;
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        JcrNodeModel model = (JcrNodeModel) getModel();
    }
}
