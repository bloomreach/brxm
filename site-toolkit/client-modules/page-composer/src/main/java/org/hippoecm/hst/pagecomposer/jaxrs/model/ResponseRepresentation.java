/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * This model represents that response that the frontend expects to wrap the actual data.
 */
public class ResponseRepresentation<T> {

    private String errorCode;
    private String message;
    private boolean reloadRequired;
    private boolean success;
    private T data;

    public ResponseRepresentation() {}

    public ResponseRepresentation(final boolean success, final String message, final T data, final boolean reloadRequired, final String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.reloadRequired = reloadRequired;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(final T data) {
        this.data = data;
    }

    public boolean isReloadRequired() {
        return reloadRequired;
    }

    public void setReloadRequired(final boolean reloadRequired) {
        this.reloadRequired = reloadRequired;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("success", success)
                .append("message", message)
                .append("data", data)
                .append("reloadRequired", reloadRequired)
                .append("errorCode", errorCode)
                .toString();
    }

    public static <T> ResponseRepresentation.Builder<T> builder() {
        return new ResponseRepresentation.Builder<>();
    }

    public static final class Builder<T> {

        private String errorCode;
        private String message;
        private boolean reloadRequired;
        private boolean success;
        private T data;

        private Builder() {
        }

        public Builder<T> setSuccess(final boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> setMessage(final String message) {
            this.message = message;
            return this;
        }

        public Builder<T> setData(final T data) {
            this.data = data;
            return this;
        }

        public Builder<T> setReloadRequired(final boolean reloadRequired) {
            this.reloadRequired = reloadRequired;
            return this;
        }

        public Builder<T> setErrorCode(final String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public ResponseRepresentation<T> build() {
            return new ResponseRepresentation<>(success, message, data, reloadRequired, errorCode);
        }
    }
}
