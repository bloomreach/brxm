/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import { Component, Page, Meta } from '@bloomreach/spa-sdk';

const { META_POSITION_BEGIN, META_POSITION_END } = jest.requireActual('@bloomreach/spa-sdk');

const meta = [
  new class implements Meta {
    getData = jest.fn(() => 'meta1');
    getPosition = jest.fn(() => META_POSITION_BEGIN as typeof META_POSITION_BEGIN);
  },
  new class implements Meta {
    getData = jest.fn(() => 'meta2');
    getPosition = jest.fn(() => META_POSITION_END as typeof META_POSITION_END);
  },
];
const component = new class implements Component {
  getId = jest.fn();
  getMeta = jest.fn(() => meta);
  getModels = jest.fn();
  getUrl = jest.fn();
  getName = jest.fn();
  getParameters = jest.fn();
  getChildren = jest.fn();
  getComponent = jest.fn();
  getComponentById = jest.fn();
};
const page = new class implements Page {
  getComponent = jest.fn(() => component);
  getContent = jest.fn();
  getMeta = jest.fn();
  getTitle = jest.fn();
  isPreview = jest.fn();
  sync = jest.fn();
};

module.exports = {
  ...jest.genMockFromModule('@bloomreach/spa-sdk'),
  initialize: jest.fn(async () => page),
};
