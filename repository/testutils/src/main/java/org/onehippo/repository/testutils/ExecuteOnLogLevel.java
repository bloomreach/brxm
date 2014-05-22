/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class contains a set of static utilities to temporarily (for example one method invocation) change the log level
 * for some logger. This is very useful in case of unit testing some behaviour with expected exceptions, for example
 * when unit testing wrongly configured bootstrap configuration. To avoid these expected exceptions being logged
 * and fill up the unit test output, you can use this utility to temporarily change the log-level
 * </p>
 *
 * <p>
 *     For example, assume that we unit test a wrong Spring bean and expect a warning to be logger
 *     when starting the component manager. Assume you know the warning is logged by componentManager.start();
 *     You can suppress this warning during that call as follows:
 * </p>
 * <pre>
 *     <code>
 *     final SpringComponentManager componentManager = ...
 *     ExecuteOnLogLevel.error(new Runnable() {
 *         public void run() {
 *           // the method to invoke with log level set to ERROR
 *           componentManager.start();
 *         }
 *         // the SpringComponentManager.class.getName() is the logger name
 *       }, SpringComponentManager.class.getName());
 *
 *       </code>
 * </pre>
 *
 * Note that all the public utility methods on purpose take a second and third argument for <code>name</code>
 * and <code>names</code>. For example
 *  <pre>
 *     <code>
 *     debug(Runnable callback, String name, String ... names)
 *     </code>
 * </pre>
 * <p>
 * If you we would use <code> debug(Runnable callback, String ... names)</code> it would be less clear when using the
 * utility method that you should include at least one logger. With only <code>String ... names</code> this would be
 * much less clear as then you could invoke debug with only a Runnable object and no other argument
 * </p>
 * <p>
 *  If the action to be invoked by the {@link Runnable} callback itself might throw (also) an exception, using
 *  {@link Runnable} becomes a bit problematic as its {@link Runnable#run()} method doesn't allow that.
 * </p>
 * <p>
 *  For these cases it is possible to use {@link Executable} instead, which allows throwing an exception in its
 *  {@link Executable#execute()} method.
 * </p>
 * <p>
 *  An exception thrown by an {@link Executable#execute()} callback will be wrapped in a {@link ExecutableException},
 *  which extends RuntimeException instead.
 * </p>
 * <p>
 *  An example of using {@link Executable} as callback could be:
 * </p>
 * <pre>
 *     <code>
 *         final MyAction action = ...
 *         try {
 *             ExecuteOnLogLevel.error(new ExecuteOnLogLevel.Executable () {
 *                public void execute() throws Exception {
 *                    // invoke the action which might throw an exception
 *                    action.start();
 *                }
 *             }, myLoggerClass);
 *         }
 *         catch (ExecuteOnLogLevel.ExecutableException ee) {
 *             if (ee.getCause() instanceof MyIgnorableException) {
 *                 // ignore
 *             }
 *             else {
 *                 throw ee.getCause();
 *             }
 *         }
 *     </code>
 * </pre>
 * <p>
 *   <em>Note: internally ExecuteOnLogLevel has been retrofitted to always use {@link Executable} using the
 *   {@link RunnableAdapter}, zo it actually is a little bit more efficient to always use {@link Executable}.</em>
 * </p>
 */
public class ExecuteOnLogLevel {

    private static final Logger log = LoggerFactory.getLogger(ExecuteOnLogLevel.class);

    /**
     * Callback interface comparable to {@link Runnable} but with support for and allowing exceptions to be thrown
     * by the {@link #execute()} method.
     */
    public static interface Executable {

        /**
         * Callback method for execution of some arbitrary action which also may throw an exception.
         * @throws Exception
         */
        public void execute() throws Exception;
    }

    /**
     * Marker RuntimeException potentially thrown by ExecuteOnLogLevel methods using {@link Executable}, which
     * can be catched to get hold of the wrapped exception thrown by {@link Executable#execute()}.
     */
    public static class ExecutableException extends RuntimeException {

        public ExecutableException(Exception cause) {
            super(cause);
        }

        @Override
        public Exception getCause() {
            return (Exception)super.getCause();
        }
    }

    /**
     * Adapter for using a {@link Runnable} as {@link Executable}
     */
    public static class RunnableAdapter implements Executable {
        Runnable callback;

        public RunnableAdapter(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void execute() {
            callback.run();
        }
    }

    private static final boolean isLog4jLog = "org.slf4j.impl.Log4jLoggerAdapter".equals(log.getClass().getName());
    private static final boolean isJDK14Log = "org.slf4j.impl.JDK14LoggerAdapter".equals(log.getClass().getName());

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to DEBUG level
     * @param clazz the class of the logger to set to DEBUG, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to DEBUG
     */
    public static void debug(Runnable callback, Class<?> clazz, Class<?> ... classes) {
        debug(new RunnableAdapter(callback), clazz, classes);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to DEBUG level
     * @param clazz the class of the logger to set to DEBUG, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to DEBUG
     */
    public static void debug(Executable callback, Class<?> clazz, Class<?> ... classes) {
        String[] strings = getNames(classes);
        debug(callback, clazz.getName(), strings);
    }

    /**
     * @param callback the {@link Runnable} class that gets callbacked after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to DEBUG level
     * @param name the name of the logger to set to DEBUG, for example javax.jcr.Repository
     * @param names the names of the loggers to set to DEBUG
     */
    public static void debug(Runnable callback, String name, String ... names) {
        setToLevel("DEBUG", new RunnableAdapter(callback), name, names);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to DEBUG level
     * @param name the name of the logger to set to DEBUG, for example javax.jcr.Repository
     * @param names the names of the loggers to set to DEBUG
     */
    public static void debug(Executable callback, String name, String ... names) {
        setToLevel("DEBUG", callback, name, names);
    }

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to INFO level
     * @param clazz the class of the logger to set to INFO, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to INFO
     */
    public static void info(Runnable callback, Class<?> clazz, Class<?> ... classes) {
        info(new RunnableAdapter(callback), clazz, classes);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to INFO level
     * @param clazz the class of the logger to set to INFO, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to INFO
     */
    public static void info(Executable callback, Class<?> clazz, Class<?> ... classes) {
        String[] strings = getNames(classes);
        info(callback, clazz.getName(), strings);
    }

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to INFO level
     * @param name the name of the logger to set to INFO, for example javax.jcr.Repository
     * @param names the names of the loggers to set to INFO
     */
    public static void info(Runnable callback, String name, String ... names) {
        setToLevel("INFO", new RunnableAdapter(callback), name, names);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to INFO level
     * @param name the name of the logger to set to INFO, for example javax.jcr.Repository
     * @param names the names of the loggers to set to INFO
     */
    public static void info(Executable callback, String name, String ... names) {
        setToLevel("INFO", callback, name, names);
    }

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to WARN level
     * @param clazz the class of the logger to set to WARN, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to WARN
     */
    public static void warn(Runnable callback, Class<?> clazz, Class<?> ... classes) {
        warn(new RunnableAdapter(callback), clazz, classes);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to WARN level
     * @param clazz the class of the logger to set to WARN, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to WARN
     */
    public static void warn(Executable callback, Class<?> clazz, Class<?> ... classes) {
        String[] strings = getNames(classes);
        warn(callback, clazz.getName(), strings);
    }

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to WARN level
     * @param name the name of the logger to set to WARN, for example javax.jcr.Repository
     * @param names the names of the loggers to set to WARN
     */
    public static void warn(Runnable callback, String name, String ... names) {
        setToLevel("WARN", new RunnableAdapter(callback), name, names);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to WARN level
     * @param name the name of the logger to set to WARN, for example javax.jcr.Repository
     * @param names the names of the loggers to set to WARN
     */
    public static void warn(Executable callback, String name, String ... names) {
        setToLevel("WARN", callback, name, names);
    }

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to ERROR level
     * @param clazz the class of the logger to set to ERROR, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to ERROR
     */
    public static void error(Runnable callback, Class<?> clazz, Class<?> ... classes) {
        error(new RunnableAdapter(callback), clazz, classes);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to ERROR level
     * @param clazz the class of the logger to set to ERROR, for example javax.jcr.Repository
     * @param classes the classes of the loggers to set to ERROR
     */
    public static void error(Executable callback, Class<?> clazz, Class<?> ... classes) {
        String[] strings = getNames(classes);
        error(callback, clazz.getName(), strings);
    }

    /**
     * @param callback the {@link Runnable} class that gets callbacked after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to ERROR level
     * @param name the name of the logger to set to ERROR, for example javax.jcr.Repository
     * @param names the names of the loggers to set to ERROR
     */
    public static void error(Runnable callback, String name, String ... names) {
        setToLevel("ERROR", new RunnableAdapter(callback), name, names);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to ERROR level
     * @param name the name of the logger to set to ERROR, for example javax.jcr.Repository
     * @param names the names of the loggers to set to ERROR
     */
    public static void error(Executable callback, String name, String ... names) {
        setToLevel("ERROR", callback, name, names);
    }

    /**
     * @param callback the {@link Runnable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to FATAL level
     * @param name the name of the logger to set to FATAL, for example javax.jcr.Repository
     * @param names the names of the loggers to set to FATAL
     */
    public static void fatal(Runnable callback, String name, String ... names) {
        setToLevel("FATAL", new RunnableAdapter(callback), name, names);
    }

    /**
     * @param callback the {@link Executable} class that gets called after the log-level for <code>name</code> and
     *                 <code>names</code> has been set to FATAL level
     * @param name the name of the logger to set to FATAL, for example javax.jcr.Repository
     * @param names the names of the loggers to set to FATAL
     */
    public static void fatal(Executable callback, String name, String ... names) {
        setToLevel("FATAL", callback, name, names);
    }

    private static void setToLevel(String level, Executable callback, String name, String ... names) {
        final String[] allNames = Arrays.copyOf(names, names.length + 1);
        allNames[allNames.length-1] = name;
        Map<String, String> oldLoggerLevelMap = new HashMap<>();

        for (String n : allNames) {
            String oldLoggerLevel = getLoggerLevel(n);
            // even when the old logger level is null, we store it in the oldLoggerLevelMap: null
            // values will later on be reset to the effective parent logger level
            oldLoggerLevelMap.put(n, oldLoggerLevel);
            // set the log levels during the callback.run(); to ERROR
            setLoggerLevel(n, level);
        }

        // do the callback with log-level set to ERROR
        try {
            callback.execute();
        }
        catch (Exception e) {
            throw new ExecutableException(e);
        } finally {
            // Reset the log-levels to the old values
            for (Map.Entry<String, String> entry : oldLoggerLevelMap.entrySet()) {
                setLoggerLevel(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @param name the name of the logger, for example javax.jcr.Repository
     * @return the level (DEBUG / INFO / ERROR / WARN etc ) of the logger for <code>name</code> or <code>null</code> when
     * the logger is not configured
     * @throws IllegalArgumentException when <code>name</code> is null or empty
     */
    private static String getLoggerLevel(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid empty name. Not settting log level");
        }
        if (isJDK14Log) {
            java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
            java.util.logging.Logger logger = logManager.getLogger(name);
            if (logger != null) {
                return logger.getLevel().getName();
            } else {
                log.debug("Logger not found : " + name);
            }
        } else if (isLog4jLog) {
            try {
                log.debug("Getting logger " + name + " level ");

                // basic log4j reflection
                Class<?> loggerClass = Class.forName("org.apache.log4j.Logger");
                Class<?> logManagerClass = Class.forName("org.apache.log4j.LogManager");
                Method getLevel = loggerClass.getMethod("getLevel");

                // get the logger
                Object logger = logManagerClass.getMethod("getLogger", String.class).invoke(null, name);

                Object o = getLevel.invoke(logger);
                if (o == null) {
                    return null;
                }
                Method toString = o.getClass().getMethod("toString");
                return (String) toString.invoke(o);

            } catch (Exception e) {
                log.warn("Unable to get logger " + name + " level ", e);
            }
        } else {
            log.debug("Unable to determine logger");
        }
        return null;
    }

    /**
     * @param name the name of the logger, for example javax.jcr.Repository
     * @param level the level to set, can be <code>null</code>. In that case the log level will be set
     *              to undefined
     * @throws IllegalArgumentException when <code>name</code> is null or empty
     */
    private static void setLoggerLevel(String name, String level) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid empty name. Not settting log level");
        }
        if (isJDK14Log) {
            java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
            java.util.logging.Logger logger = logManager.getLogger(name);
            if (logger != null) {
                logger.setLevel(Level.parse(level));
            } else {
                log.debug("Logger not found : " + name);
            }
        } else if (isLog4jLog) {
            try {
                log.debug("Setting logger " + name + " to level " + level);

                // basic log4j reflection
                Class<?> loggerClass = Class.forName("org.apache.log4j.Logger");
                Class<?> levelClass = Class.forName("org.apache.log4j.Level");
                Class<?> logManagerClass = Class.forName("org.apache.log4j.LogManager");
                Method setLevel = loggerClass.getMethod("setLevel", levelClass);

                // get the logger
                Object logger = logManagerClass.getMethod("getLogger", String.class).invoke(null, name);

                // get the static level object field, e.g. Level.INFO
                if (level == null) {
                    // from the Logger, try to get the EffectiveLevel of the first parent logger: If the effective level
                    // is null or there is no parent, we won't reset the log-level to the old value. Otherwise,
                    // we reset it to the value of the parent
                    Method getParent =  loggerClass.getMethod("getParent");
                    Object parentLogger = getParent.invoke(logger);
                    if (parentLogger == null) {
                        return;
                    }
                    Method effectiveLevelMethod = parentLogger.getClass().getMethod("getEffectiveLevel");
                    Object levelObj = effectiveLevelMethod.invoke(parentLogger);
                    if (levelObj == null) {
                        return;
                    }
                    // reset the level to the effective parent level
                    setLevel.invoke(logger, levelObj);
                } else {
                    Field levelField;
                    levelField = levelClass.getField(level);
                    Object levelObj = levelField.get(null);
                    // set the level
                    setLevel.invoke(logger, levelObj);
                }
            } catch (NoSuchFieldException e) {
                log.warn("Unable to find Level." + level + " , not adjusting logger " + name);
            } catch (Exception e) {
                log.error("Unable to set logger " + name + " + to level " + level, e);
            }
        } else {
            log.warn("Unable to determine logger");
        }
    }


    private static String[] getNames(final Class<?>[] classes) {
        if (classes == null) {
            return null;
        }
        List<String> strings = new ArrayList<>(classes.length);
        for (Class<?> clazz : classes) {
            strings.add(clazz.getName());
        }
        return strings.toArray(new String[strings.size()]);
    }

}
