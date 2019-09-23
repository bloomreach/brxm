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
import { MetaComment as SpaMetaComment } from '@bloomreach/spa-sdk';

interface MetaCommentProps {
  meta: SpaMetaComment;
}

export class MetaComment extends React.Component<MetaCommentProps> {
  private placeholder = React.createRef<HTMLElement>();
  private comment?: Comment;

  componentDidMount() {
    if (!this.placeholder.current) {
      return;
    }

    const placeholder = this.placeholder.current;
    this.comment = placeholder.ownerDocument!.createComment(this.props.meta.getData());
    placeholder.parentNode!.replaceChild(this.comment, placeholder);
  }

  shouldComponentUpdate(nextProps: MetaCommentProps) {
    return nextProps.meta.getData() !== this.props.meta.getData();
  }

  componentDidUpdate() {
    this.comment!.replaceData(0, this.comment!.data.length, this.props.meta.getData());
  }

  componentWillUnmount() {
    if (!this.comment) {
      return;
    }

    this.comment.remove();
  }

  render() {
    return <span style={{ display: 'none' }} ref={this.placeholder} />;
  }
}
