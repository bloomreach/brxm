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

const spaSdk = jest.requireActual('@bloomreach/spa-sdk');

export const META_POSITION_BEGIN = spaSdk.META_POSITION_BEGIN;
export const META_POSITION_END = spaSdk.META_POSITION_END;

export function mockMeta(data: string, position: string) {
  return {
    getData: jest.fn().mockReturnValue(data),
    getPosition: jest.fn().mockReturnValue(position),
  };
}

export function mockNoCommentMeta() {
  return mockMeta('not-a-comment', META_POSITION_BEGIN);
}

const componentMock = {
  getMeta: jest.fn().mockReturnValue([
    mockMeta('meta-begin', META_POSITION_BEGIN),
    mockMeta('meta-end', META_POSITION_END),
  ]),
};

export const pageMock = {
  getComponent: jest.fn().mockReturnValue(componentMock),
  getContent: jest.fn(),
  getTitle: jest.fn(),
  sync: jest.fn(),
};

export const initialize = jest.fn();
export const destroy = jest.fn();

export function isMetaComment(value: any) {
  return value.getData() !== 'not-a-comment';
}
