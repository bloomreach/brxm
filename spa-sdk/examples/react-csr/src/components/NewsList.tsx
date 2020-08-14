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
import { Document } from '@bloomreach/spa-sdk';
import { BrManageContentButton, BrPageContext, BrProps } from '@bloomreach/react-sdk';

export function NewsList(props: BrProps) {
  const { pageable } = props.component.getModels<PageableModels>();

  if (!pageable) {
    return null;
  }

  return (
    <div>
      { pageable.items.map((reference, key) => <NewsListItem key={key} item={props.page.getContent<Document>(reference)!} />) }
      <NewsListPagination {...pageable} />
    </div>
  );
}

interface NewsListItemProps {
  item: Document;
}

export function NewsListItem({ item }: NewsListItemProps) {
  const { author, date, introduction, title } = item.getData<DocumentData>();

  return (
    <div className="card mb-3">
      <BrManageContentButton content={item} />
      <div className="card-body">
        { title && (
          <h2 className="card-title">
            <Link to={item.getUrl()!}>{title}</Link>
          </h2>
        ) }
        { author && <div className="card-subtitle mb-3 text-muted">{author}</div> }
        { date && <div className="card-subtitle mb-3 small text-muted">{new Date(date).toDateString()}</div> }
        { introduction && <p className="card-text">{introduction}</p> }
      </div>
    </div>
  );
}

export function NewsListPagination(props: Pageable) {
  const page = React.useContext(BrPageContext);

  if (!page || !props.showPagination) {
    return null;
  }

  return (
    <nav aria-label="News List Pagination">
      <ul className="pagination">
        <li className={`page-item ${props.previous ? '' : 'disabled'}`}>
          <Link to={props.previous ? page.getUrl(`?page=${props.previousPage}`) : '#'} className="page-link" aria-label="Previous">
            <span aria-hidden="true">&laquo;</span>
            <span className="sr-only">Previous</span>
          </Link>
        </li>
        { props.pageNumbersArray.map((pageNumber, key) => (
          <li key={key} className={`page-item ${pageNumber === props.currentPage ? 'active' : ''}`}>
            <Link to={page.getUrl(`?page=${pageNumber}`)} className="page-link">{pageNumber}</Link>
          </li>
        )) }
        <li className={`page-item ${props.next ? '' : 'disabled'}`}>
          <Link to={props.next ? page.getUrl(`?page=${props.nextPage}`) : '#'} className="page-link" aria-label="Next">
            <span aria-hidden="true">&raquo;</span>
            <span className="sr-only">Next</span>
          </Link>
        </li>
      </ul>
    </nav>
  );
}
