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

import React from 'react';
import { Link } from 'react-router-dom';
import { Document, ImageSet } from '@bloomreach/spa-sdk';
import { BrManageContentButton, BrProps } from '@bloomreach/react-sdk';

export function Banner(props: BrProps) {
  const { document: documentRef } = props.component.getModels();
  const document = documentRef && props.page.getContent(documentRef);

  if (!document) {
    return null;
  }

  const { content, image: imageRef, link: linkRef, title } = document.getData<DocumentData>();
  const image = imageRef && props.page.getContent<ImageSet>(imageRef);
  const link = linkRef && props.page.getContent<Document>(linkRef);

  return (
    <div className={`jumbotron mb-3 ${props.page.isPreview() ? 'has-edit-button' : ''}`}>
      <BrManageContentButton content={document} />
      { title && <h1>{title}</h1> }
      { image && <img className="img-fluid" src={image.getOriginal()?.getUrl()} alt={title} /> }
      { content && <div dangerouslySetInnerHTML={{ __html: props.page.rewriteLinks(content.value) }} /> }
      { link && (
        <p className="lead">
          <Link to={link.getUrl()!} className="btn btn-primary btn-lg" role="button">Learn more</Link>
        </p>
      ) }
    </div>
  );
}
