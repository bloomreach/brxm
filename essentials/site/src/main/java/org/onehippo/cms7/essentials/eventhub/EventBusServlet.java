package org.onehippo.cms7.essentials.eventhub;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.codehaus.jackson.map.ObjectMapper;

public class EventBusServlet extends WebSocketServlet {

    private static final long serialVersionUID = 1L;
    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";

    private Map<String, Channel> channels = new HashMap<String, Channel>();

    private WatchThread watchThread;
    private WatchService watcher;

    @Override
    public void init() throws ServletException {
        try {
            final FileSystem fileSystem = FileSystems.getDefault();
            watcher = fileSystem.newWatchService();
            watchThread = new WatchThread(watcher);
            watchThread.start();
        } catch (IOException e) {
            throw new ServletException("Could not create watch service", e);
        }
    }

    @Override
    public void destroy() {
        watchThread.stopWatching();
        try {
            watcher.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    @Override
    protected StreamInbound createWebSocketInbound(final String subProtocol, HttpServletRequest request) {
        return new Connection();
    }

    synchronized void subscribe(String path, Connection connection) throws IOException {
        Channel channel = channels.get(path);
        if (channel == null) {
            channel = new Channel(path);
            channels.put(path, channel);
        }
        channel.addConnection(connection);
    }

    synchronized void unsubscribe(String path, Connection connection) {
        Channel channel = channels.get(path);
        channel.removeConnection(connection);
        if (!channel.hasListeners()) {
            channel.destroy();
            channels.remove(path);
        }
    }

    synchronized void onChange(final String path) throws IOException {
        Channel channel = channels.get(path);
        if (channel != null) {
            channel.notifyChange();
        }
    }

    private class Channel {
        private final String path;
        private final WatchKey watchKey;
        private final List<Connection> connections;

        private Channel(String path) throws IOException {
            this.path = path;

            final FileSystem fileSystem = FileSystems.getDefault();
            Path file = fileSystem.getPath(path);
            System.out.println(file.toFile().getAbsolutePath());
            this.watchKey = file.register(watcher,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
            this.connections = new LinkedList<Connection>();
        }

        private void destroy() {
            watchKey.cancel();
        }

        boolean hasListeners() {
            return connections.size() > 0;
        }

        void notifyChange() throws IOException {
            for (Connection connection : connections) {
                connection.send(this.path);
            }
        }

        void addConnection(Connection connection) {
            this.connections.add(connection);
        }

        void removeConnection(Connection connection) {
            this.connections.remove(connection);
        }
    }

    private class Connection extends MessageInbound {

        private final Set<String> paths = new HashSet<String>();
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        protected void onClose(final int status) {
            synchronized (EventBusServlet.this) {
                for (String path : paths) {
                    unsubscribe(path, this);
                }
                paths.clear();
            }
        }

        @Override
        protected void onBinaryMessage(final ByteBuffer message) throws IOException {
        }

        @Override
        protected void onTextMessage(final CharBuffer message) throws IOException {
            EventBusAction action = mapper.readValue(message.toString(), EventBusAction.class);
            if ("subscribe".equals(action.getType())) {
                subscribe(action.getChannel(), this);
                paths.add(action.getChannel());
            } else {
                paths.remove(action.getChannel());
                unsubscribe(action.getChannel(), this);
            }
        }

        void send(String path) throws IOException {
            ChangeEvent event = new ChangeEvent();
            event.setPath(path);
            CharBuffer response = CharBuffer.wrap(mapper.writeValueAsString(event));
            getWsOutbound().writeTextMessage(response);
        }
    }

    private class WatchThread extends Thread {

        private final WatchService watcher;
        private volatile boolean stopped = false;

        public WatchThread(final WatchService watcher) {
            super("WatchThread");
            this.watcher = watcher;
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    final WatchKey take = watcher.take();
                    take.pollEvents();

                    final Path watchable = (Path) take.watchable();
                    final File file = watchable.toFile();

                    final String path = file.getPath();
                    onChange(path);
                    take.reset();
                } catch (InterruptedException e) {
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }

        public void stopWatching() {
            stopped = true;
            try {
                join();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private String getBaseDir() {
        if (System.getProperty(PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            return System.getProperty(PROJECT_BASEDIR_PROPERTY);
        } else {
            return null;
        }
    }
}
