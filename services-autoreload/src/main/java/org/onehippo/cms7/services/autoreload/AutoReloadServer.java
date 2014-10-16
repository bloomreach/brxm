/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.autoreload;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/autoreload", configurator = AutoReloadServerConfigurator.class)
public class AutoReloadServer {

    private static final String RELOAD_PAGE_MESSAGE_JSON = "{\"command\":\"reloadPage\"}";

    private static final Logger log = LoggerFactory.getLogger(AutoReloadServer.class);
    private static AutoReloadServer instance = null;

    private final ConcurrentLinkedQueue<Session> sessions;

    private AutoReloadServer() {
        sessions = new ConcurrentLinkedQueue();
        HippoServiceRegistry.registerService(this, HippoEventBus.class);
        log.info("auto-reload server created");
    }

    static synchronized AutoReloadServer getInstance() {
        if (instance == null) {
            instance = new AutoReloadServer();
        }
        return instance;
    }

    @OnOpen
    public void onOpen(final Session session) {
        sessions.add(session);
        log.info("auto-reload connection '{}' opened, #connections = {}", session.getId(), sessions.size());
    }

    @OnMessage
    public void onMessage(final Session session, final String msg) {
        log.warn("closing auto-reload connection, unexpected message: '{}'", msg);
        closeQuitely(session);
    }

    private void closeQuitely(final Session session) {
        try {
            session.close();
        } catch (IOException e) {
            log.debug("Error while closing auto-reload connection:", e);
        }
    }

    @OnClose
    public void onClose(final Session session) {
        sessions.remove(session);
        log.info("auto-reload connection '{}' closed, #connections = {}, ", session.getId(), sessions.size());
    }

    @OnError
    public void onError(final Throwable t) {
        log.debug("ignoring auto-reload websocket error:", t);
    }

    void broadcastPageReload() {
        log.debug("broadcasting page reload");
        broadcast(RELOAD_PAGE_MESSAGE_JSON);
    }

    private void broadcast(final String message) {
        for (Session session : sessions) {
            session.getAsyncRemote().sendText(message);
        }
    }

}