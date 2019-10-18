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

import React from 'react';
import ReactDOM from 'react-dom';
import { MetaComment, Meta, META_POSITION_BEGIN, META_POSITION_END, isMetaComment } from '@bloomreach/spa-sdk';

interface BrMetaProps {
  meta: Meta[];
}

export class BrMeta extends React.Component<BrMetaProps> {
  private comments: Comment[] = [];
  private head?: Element | Text;
  private tailRef = React.createRef<HTMLElement>();

  componentDidMount() {
    this.head = ReactDOM.findDOMNode(this)!;
    this.renderMeta();
  }

  componentDidUpdate() {
    this.head = ReactDOM.findDOMNode(this)!;
    this.removeMeta();
    this.renderMeta();
  }

  componentWillUnmount() {
    this.removeMeta();
  }

  private removeMeta() {
    this.comments.splice(0).forEach(comment => comment.remove());
  }

  private renderMeta() {
    this.props.meta.filter(meta => isMetaComment(meta) && meta.getPosition() === META_POSITION_BEGIN)
      .forEach(this.renderMetaComment.bind(this));
    this.props.meta.filter(meta => isMetaComment(meta) && meta.getPosition() === META_POSITION_END)
      .reverse()
      .forEach(this.renderMetaComment.bind(this));
  }

  private renderMetaComment(meta: MetaComment) {
    const comment = this.head!.ownerDocument!.createComment(meta.getData());
    this.comments.push(comment);

    if (meta.getPosition() === META_POSITION_BEGIN) {
      return void this.head!.parentNode!.insertBefore(comment, this.head!);
    }

    if (!this.tailRef.current!.nextSibling) {
      return void this.tailRef.current!.parentNode!.appendChild(comment);
    }

    this.tailRef.current!.parentNode!.insertBefore(comment, this.tailRef.current!.nextSibling);
  }

  render() {
    return (
      <>
        {this.props.children}
        {this.props.meta.length ? <span style={{ display: 'none' }} ref={this.tailRef} /> : null}
      </>
    );
  }
}
