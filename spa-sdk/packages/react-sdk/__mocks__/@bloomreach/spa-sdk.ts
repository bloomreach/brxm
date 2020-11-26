/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { Component, Page } from '@bloomreach/spa-sdk';

const component = new class implements Component {
  getId = jest.fn();
  getMeta = jest.fn();
  getModels = jest.fn();
  getUrl = jest.fn();
  getName = jest.fn();
  getParameters = jest.fn();
  getChildren = jest.fn();
  getComponent = jest.fn();
  getComponentById = jest.fn();
};
const page = new class implements Page {
  getButton = jest.fn();
  getChannelParameters = jest.fn();
  getComponent = jest.fn(() => component);
  getContent = jest.fn();
  getDocument = jest.fn();
  getMeta = jest.fn();
  getTitle = jest.fn();
  getUrl = jest.fn();
  getVersion = jest.fn();
  getVisitor = jest.fn();
  getVisit = jest.fn();
  isPreview = jest.fn();
  rewriteLinks = jest.fn();
  sync = jest.fn();
  toJSON = jest.fn();
};

module.exports = {
  ...jest.genMockFromModule('@bloomreach/spa-sdk'),
  initialize: jest.fn(() => page),
};
