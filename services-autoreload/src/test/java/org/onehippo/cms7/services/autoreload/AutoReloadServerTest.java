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
import java.util.concurrent.Future;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;

public class AutoReloadServerTest {

    private static LoggerRecordingWrapper log;
    private AutoReloadServer server;

    @BeforeClass
    public static void recordLogger() {
        log = new LoggerRecordingWrapper(AutoReloadServer.log);
        AutoReloadServer.log = log;
    }

    @Before
    public void setUp() {
        server = AutoReloadServer.getInstance();
        log.clearLogRecords();
    }

    @After
    public void tearDown() {
        server.clearSessions();
    }

    @Test
    public void only_one_instance_exists() {
        final AutoReloadServer instance2 = AutoReloadServer.getInstance();
        assertSame("There should only be one instance", server, instance2);
        assertNoWarningsLogged();
        assertNoErrorsLogged();
    }

    @Test
    public void page_reload_is_sent_to_one_connected_session() throws InterruptedException {
        Session session = EasyMock.createMock(Session.class);
        RemoteEndpoint.Async remote = EasyMock.createMock(RemoteEndpoint.Async.class);
        Future sendFuture = EasyMock.createMock(Future.class);

        expect(session.getId()).andReturn("42");
        expect(session.getAsyncRemote()).andReturn(remote);
        expect(remote.sendText(eq("{\"command\":\"reloadPage\"}"))).andReturn(sendFuture);
        replay(session, remote, sendFuture);

        server.onOpen(session);
        server.broadcastPageReload();

        verify(session, remote, sendFuture);
        assertLoggedInfo("auto-reload connection '42' opened, #connections = 1");
        assertNoWarningsLogged();
        assertNoErrorsLogged();
    }

    @Test
    public void page_reload_is_sent_to_two_connected_sessions() throws InterruptedException {
        Session session1 = EasyMock.createMock(Session.class);
        Session session2 = EasyMock.createMock(Session.class);
        RemoteEndpoint.Async remote = EasyMock.createMock(RemoteEndpoint.Async.class);
        Future sendFuture = EasyMock.createMock(Future.class);

        expect(session1.getId()).andReturn("one");
        expect(session2.getId()).andReturn("two");
        expect(session1.getAsyncRemote()).andReturn(remote);
        expect(session2.getAsyncRemote()).andReturn(remote);
        expect(remote.sendText(eq("{\"command\":\"reloadPage\"}"))).andReturn(sendFuture).times(2);
        replay(session1, session2, remote, sendFuture);

        server.onOpen(session1);
        server.onOpen(session2);
        server.broadcastPageReload();

        verify(session1, session2, remote, sendFuture);
        assertLoggedInfo(
                "auto-reload connection 'one' opened, #connections = 1",
                "auto-reload connection 'two' opened, #connections = 2"
        );
        assertNoWarningsLogged();
        assertNoErrorsLogged();
    }

    @Test
    public void page_reload_is_not_sent_to_closed_session() throws InterruptedException {
        Session session = EasyMock.createMock(Session.class);

        expect(session.getId()).andReturn("42").anyTimes();
        replay(session);

        server.onOpen(session);
        server.onClose(session);
        server.broadcastPageReload();

        verify(session);
        assertLoggedInfo(
                "auto-reload connection '42' opened, #connections = 1",
                "auto-reload connection '42' closed, #connections = 0"
        );
        assertNoWarningsLogged();
        assertNoErrorsLogged();
    }

    @Test
    public void message_from_remote_closes_session() throws InterruptedException, IOException {
        Session session = EasyMock.createMock(Session.class);

        expect(session.getId()).andReturn("42").anyTimes();
        session.close();
        expectLastCall();
        replay(session);

        server.onOpen(session);
        server.onMessage(session, "test");

        verify(session);
        assertLoggedInfo("auto-reload connection '42' opened, #connections = 1");
        assertLoggedWarning("closing auto-reload connection '42', unexpected message: 'test'");
        assertNoErrorsLogged();
    }

    @Test
    public void message_from_remote_closes_session_quietly() throws InterruptedException, IOException {
        Session session = EasyMock.createMock(Session.class);

        expect(session.getId()).andReturn("42").anyTimes();
        session.close();
        expectLastCall().andThrow(new IOException("simulate IO exception during close"));
        replay(session);

        server.onOpen(session);
        server.onMessage(session, "test");

        verify(session);
        assertLoggedInfo("auto-reload connection '42' opened, #connections = 1");
        assertLoggedWarning("closing auto-reload connection '42', unexpected message: 'test'");
        assertNoErrorsLogged();
    }

    private void assertLoggedInfo(String... messages) {
        int count = 0;
        for (String loggedInfo : log.getInfoMessages()) {
            assertEquals("Logged INFO message", loggedInfo, messages[count]);
            count += 1;
        }
    }

    private void assertLoggedWarning(String... messages) {
        int count = 0;
        for (String loggedWarning : log.getWarnMessages()) {
            assertEquals("Logged warning", messages[count], loggedWarning);
            count += 1;
        }
    }

    private void assertNoWarningsLogged() {
        assertEquals("Number of logged warnings", 0, log.getWarnMessages().size());
    }

    private void assertNoErrorsLogged() {
        assertEquals("Number of logged errors", 0, log.getErrorMessages().size());
    }

}
