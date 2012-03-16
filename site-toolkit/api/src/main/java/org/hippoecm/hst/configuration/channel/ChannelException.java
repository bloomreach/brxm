/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

/**
 * Thrown when a channel-related error occured. A channel exception has a type with a key that can be used for
 * i18n of the error messages. A channel exception can also contain a number of parameters that can be used in the
 * i18n error message. Which parameters are included should be documented in the method where the exception is thrown.
 */
public class ChannelException extends Exception {

    /**
     * Type of a channel exception. Each type contains an key that can be used as (part of) a resource bundle key.
     */
    public enum Type {

        UNKNOWN("unknown"),
        MOUNT_EXISTS("mount.exists"),
        MOUNT_NOT_FOUND("mount.not.found"),
        CANNOT_CREATE_CONTENT("cannot.create.content");

        private final String key;

        Type(String key) {
            this.key = key;
        }

        /**
         * @return a key unique for this error.
         */
        public String getKey() {
            return key;
        }

    }

    private static final long serialVersionUID = 1L;
    private static final String[] ZERO_PARAMETERS = new String[0];

    private final Type type;
    private final String[] parameters;

    public ChannelException(String message) {
        this(message, null, Type.UNKNOWN);
    }

    public ChannelException(String message, Throwable cause) {
        this(message, cause, Type.UNKNOWN);
    }

    public ChannelException(String message, Type type, String... parameters) {
        this(message, null, type, parameters);
    }

    public ChannelException(String message, Throwable cause, Type type, String... parameters) {
        super(message, cause);
        this.type = type;
        this.parameters = parameters == null ? ZERO_PARAMETERS : parameters;
    }

    public Type getType() {
        return type;
    }

    public String[] getParameters() {
        return parameters;
    }

}
