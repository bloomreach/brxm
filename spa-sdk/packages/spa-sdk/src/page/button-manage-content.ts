/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { Content } from './content09';
import { Document } from './document';
import { Meta, MetaModel, META_POSITION_BEGIN, META_POSITION_END, TYPE_META_COMMENT } from './meta';
import { MetaCollectionModel, MetaCollection } from './meta-collection';

/**
 * A manage content button.
 */
export const TYPE_MANAGE_CONTENT_BUTTON = 'MANAGE_CONTENT_LINK';

export interface ManageContentButton {
  /**
   * The content entity to open for editing.
   */
  content?: Content | Document;

  /**
   * Template query to use for creating new documents.
   */
  documentTemplateQuery?: string;

  /**
   * Template query to use in case folders specified by `path` do not yet exist and must be created.
   */
  folderTemplateQuery?: string;

  /**
   * Initial location of a new document, relative to the `root`.
   */
  path?: string;

  /**
   * Name of the component parameter in which the document path is stored.
   */
  parameter?: string;

  /**
   * Flag indicating that the picked value should be stored as a relative path.
   */
  relative?: boolean;

  /**
   * Path to the root folder of selectable document locations.
   */
  root?: string;
}

export function createManageContentButton(params: ManageContentButton): MetaCollection | MetaCollectionModel {
  const meta = params.content?.getMeta();
  const entries = [
    ['defaultPath', params.path],
    ['documentTemplateQuery', params.documentTemplateQuery],
    ['folderTemplateQuery', params.folderTemplateQuery],
    ['rootPath', params.root],
    ['parameterName', params.parameter],
    ['parameterValueIsRelativePath', params.relative ? 'true' : undefined],
  ].filter(([, value]) => !!value);

  if (!entries.length) {
    return meta ?? {};
  }

  const model = Object.fromEntries(entries);

  if (!meta) {
    return {
      beginNodeSpan: [{
        type: TYPE_META_COMMENT,
        data: JSON.stringify({ 'HST-Type': TYPE_MANAGE_CONTENT_BUTTON, ...model }),
      }],
    };
  }

  const merge = (item: Meta) => ({
    type: TYPE_META_COMMENT,
    data: JSON.stringify(Object.assign(JSON.parse(item.getData()), model)),
  } as MetaModel);

  return {
    beginNodeSpan: meta.filter(item => item.getPosition() === META_POSITION_BEGIN).map(merge),
    endNodeSpan: meta.filter(item => item.getPosition() === META_POSITION_END).map(merge),
  };
}
