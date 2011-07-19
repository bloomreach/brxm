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
package org.hippoecm.hst.configuration.components;

/**
 * ParameterType used to provide a hint to the template composer about the type of the parameter. This is just a
 * convenience interface that provides some constants for the field types.
 * @deprecated use the return type plus additional annotations
 */
@Deprecated
public interface ParameterType {
    String STRING = "STRING";
    String NUMBER = "NUMBER";
    String BOOLEAN = "BOOLEAN";
    String DATE = "DATE";
    String COLOR = "COLOR";
    String DOCUMENT = "DOCUMENT";
}