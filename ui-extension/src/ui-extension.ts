/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Main entry point of the ui-extension library. Implements the public API defined in the
 * api module and communicates with the parent frame using the parent module.
 * @module ui-extension
 * @see module:api
 * @see module:parent
 */

/**
 * Disable TSLint for imports that start with an uppercase letter
 * @see https://github.com/Microsoft/tslint-microsoft-contrib/issues/387
 */
import Emittery from 'emittery';  // tslint:disable-line:import-name
import { UiScope } from './api';
import { connect } from './parent';
import { Ui } from './ui';

/**
 * Main entry point of the ui-extension library.
 */
export default class UiExtension {
  /**
   * Registers a UI extension with the CMS.
   *
   * @returns A promise that resolves with a [[UiScope]] when the extension has been registered successfully.
   * The promise rejects with a [[UiExtensionError]] when registration failed.
   */
  static register(): Promise<UiScope> {
    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    const eventEmitter = new Emittery();

    return connect(parentOrigin, eventEmitter)
      .then(parentConnection => new Ui(parentConnection, eventEmitter).init());
  }
}

/**
 * Enable UiExtension.register() in ui-extension.min.js
 * @hidden don't include in the generated documentation since it duplicates UiExtension.register()
 */
export const register = UiExtension.register;
