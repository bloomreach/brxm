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

import { Document } from './document';
import { MetaCollection } from './meta-collection';
import { META_POSITION_BEGIN } from './meta';
import { TYPE_MANAGE_CONTENT_BUTTON, createManageContentButton } from './button-manage-content';

describe('createManageContentButton', () => {
  let content: jest.Mocked<Document>;
  let meta: jest.Mocked<MetaCollection>;

  beforeEach(() => {
    content = { getMeta: jest.fn(() => meta) } as unknown as typeof content;
    meta = [{
      getPosition: jest.fn(() => META_POSITION_BEGIN),
      getData: jest.fn(() => JSON.stringify({ 'HST-Type': TYPE_MANAGE_CONTENT_BUTTON, uuid: 'id' })),
    }] as unknown as typeof meta;
  });

  it('should return content meta-data as-is if there are no custom parameters', () => {
    expect(createManageContentButton({ content })).toBe(meta);
  });

  it('should return an empty meta-data model if there is no meta-data', () => {
    expect(createManageContentButton({})).toEqual({});
  });

  it('should generate add content button when there are no content parameters', () => {
    expect(createManageContentButton({
      documentTemplateQuery: 'new-content-document',
      folderTemplateQuery: 'new-content-folder',
      root: 'content',
    })).toMatchSnapshot();
  });

  it('should merge custom parameters', () => {
    expect(createManageContentButton({
      content,
      documentTemplateQuery: 'new-news-document',
      folderTemplateQuery: 'new-news-folder',
      root: 'news',
      path: '2020/11',
      parameter: 'document',
    })).toMatchSnapshot();
  });

  it('should set relative parameter flag', () => {
    expect(createManageContentButton({ content, relative: true })).toMatchSnapshot();
  });

  it('should not set relative parameter flag', () => {
    expect(createManageContentButton({ content, path: 'content', relative: false })).toMatchSnapshot();
    expect(createManageContentButton({ content, path: 'content' })).toMatchSnapshot();
  });
});
