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

import { Factory } from './factory';
import { MetaModel, Meta, META_POSITION_BEGIN, META_POSITION_END, MetaPosition } from './meta';
import { isMetaComment } from './meta-comment';

/**
 * @hidden
 */
export interface MetaCollectionModel {
  beginNodeSpan?: MetaModel[];
  endNodeSpan?: MetaModel[];
}

/**
 * Collection of the meta-data describing a part of the page.
 * @note The collection extends the built-in Array type for backward compatibility.
 */
export interface MetaCollection extends Array<Meta> {
  /**
   * Clears previously rendered meta-data objects.
   */
  clear(): void;

  /**
   * Renders meta-data objects on the page.
   * @param head The heading node of the page fragment.
   * @param tail The tailing node of the page fragment.
   */
  render(head: Element | Text, tail: Element | Text): void;
}

export class MetaCollectionImpl extends Array<Meta> implements MetaCollection {
  private comments: Comment[] = [];

  constructor(model: MetaCollectionModel, factory: Factory<[MetaModel, MetaPosition], Meta>) {
    super(
      ...(model.beginNodeSpan || []).map(model => factory.create(model, META_POSITION_BEGIN)),
      ...(model.endNodeSpan || []).map(model => factory.create(model, META_POSITION_END)),
    );
    Object.setPrototypeOf(this, Object.create(MetaCollectionImpl.prototype));
  }

  clear() {
    this.comments.splice(0).forEach(comment => comment.remove());
  }

  render(head: Element | Text, tail: Element | Text) {
    const document = head.ownerDocument ?? tail.ownerDocument;
    if (!document) {
      return;
    }

    this.comments.push(
      ...this.filter(isMetaComment)
        .filter(meta => meta.getPosition() === META_POSITION_BEGIN)
        .map(meta => document.createComment(meta.getData()))
        .map((comment) => {
          head.parentNode?.insertBefore(comment, head);

          return comment;
        }),

      ...this.filter(isMetaComment)
        .filter(meta => meta.getPosition() === META_POSITION_END)
        .reverse()
        .map(meta => document.createComment(meta.getData()))
        .map((comment) => {
          if (tail.nextSibling) {
            tail.parentNode?.insertBefore(comment, tail.nextSibling);
          } else {
            tail.parentNode?.appendChild(comment);
          }

          return comment;
        }),
    );
  }
}
