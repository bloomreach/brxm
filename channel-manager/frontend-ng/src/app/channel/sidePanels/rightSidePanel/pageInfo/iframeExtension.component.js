/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import './iframeExtension.scss';
import controller from './iframeExtension.controller';
import template from './iframeExtension.html';

const iframeExtensionComponent = {
  controller,
  template,
  bindings: {
    // Trick to set the attributes 'flex' and 'layout="column"' on the iframe-extension tag so we get a scrollbar
    // inside the tab (see https://github.com/angular-ui/ui-router/issues/3385#issuecomment-333919458).
    // The values of 'flex' and 'layout' are defined in the state definition.
    flex: '@',
    layout: '@',
  },
};

export default iframeExtensionComponent;
