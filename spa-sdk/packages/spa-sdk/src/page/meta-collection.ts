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

import { inject, injectable } from 'inversify';
import { MetaFactory } from './meta-factory';
import { MetaModel, Meta, META_POSITION_BEGIN, META_POSITION_END } from './meta';
import { isMetaComment } from './meta-comment';

export const MetaCollectionModelToken = Symbol.for('MetaCollectionModelToken');

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
   * Clears all previously rendered meta-data objects.
   * @deprecated Use a callback returned by the `render` method.
   */
  clear(): void;

  /**
   * Renders meta-data objects on the page.
   * @param head The heading node of the page fragment.
   * @param tail The tailing node of the page fragment.
   * @return The callback to clear rendered meta-data objects.
   */
  render(head: Node, tail: Node): () => void;
}

@injectable()
export class MetaCollectionImpl extends Array<Meta> implements MetaCollection {
  private comments: Comment[] = [];

  constructor(
    @inject(MetaCollectionModelToken) model: MetaCollectionModel,
    @inject(MetaFactory) metaFactory: MetaFactory,
  ) {
    super(
      ...(model.beginNodeSpan || []).map(model => metaFactory.create(model, META_POSITION_BEGIN)),
      ...(model.endNodeSpan || []).map(model => metaFactory.create(model, META_POSITION_END)),
    );

    const prototype = Object.create(MetaCollectionImpl.prototype);

    prototype.constructor = Array.prototype.constructor;
    Object.setPrototypeOf(this, prototype);
    Object.freeze(this);
  }

  clear(comments = [...this.comments]) {
    comments.forEach((comment) => {
      comment.remove();

      const index = this.comments.indexOf(comment);
      if (index > -1) {
        this.comments.splice(index, 1);
      }
    });
  }

  render(head: Node, tail: Node) {
    const document = head.ownerDocument ?? tail.ownerDocument;
    const comments = document
      ? [
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
      ]
      : [];

    this.comments.push(...comments);

    return this.clear.bind(this, comments);
  }
}
