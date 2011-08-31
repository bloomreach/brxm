/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.checker;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Coroutine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static <X> Iterable<X> toIterable(Visitable<X> visitable) {
        Bridge<X> bridge = new Bridge<X>(visitable);
        Thread thread = new Thread(bridge);
        thread.start();
        return bridge;
    }

    static <X> Iterable<X> toIterable(Visitable<X> visitable, int size) {
        Bridge<X> bridge = new Bridge<X>(visitable, size);
        Thread thread = new Thread(bridge);
        thread.start();
        return bridge;
    }

    static class Bridge<X> implements Runnable, Visitor<X>, Iterable<X> {

        int size = -1;
        volatile boolean done = false;
        Visitable<X> visitable;
        BlockingQueue<X> queue = new LinkedBlockingQueue<X>(32);
        Progress progress;

        Bridge(Visitable<X> visitable) {
            this.visitable = visitable;
            progress = new Progress();
        }

        Bridge(Visitable<X> visitable, int size) {
            this.visitable = visitable;
            this.size = size;
            progress = new Progress(size);
        }

        public void run() {
            visitable.accept(this);
            synchronized (this) {
                done = true;
                progress.close();
                notify();
            }
        }

        public void visit(X x) {
            try {
                queue.put(x);
                synchronized (Bridge.this) {
                    notify();
                }
            } catch (InterruptedException ex) {
            }
        }

        public Iterator<X> iterator() {
            return new Iterator<X>() {
                private int count = 0;
                public boolean hasNext() {
                    synchronized (Bridge.this) {
                        while (!done && queue.size() == 0) {
                            try {
                                Bridge.this.wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                        return queue.size() > 0;
                    }
                }

                public X next() {
                    synchronized (Bridge.this) {
                        while (!done && queue.size() == 0) {
                            try {
                                Bridge.this.wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                    if (queue.size() > 0) {
                        try {
                            progress.setProgress(++count);
                            return queue.take();
                        } catch (InterruptedException ex) {
                            throw new NoSuchElementException();
                        } finally {
                        }
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
