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
import { Content } from '@bloomreach/spa-sdk';
import { BrProps } from '@bloomreach/react-sdk';

export function NewsList(props: BrProps) {
  const { pageable } = props.component.getModels<PageableModels>();

  if (!pageable) {
    return <div/>;
  }

  return (
    <div>
      { pageable.items.map((reference, key) => <NewsListItem key={key} item={props.page.getContent(reference)!} />) }
      <NewsListPagination {...pageable} />
    </div>
  );
}

interface NewsListItemProps {
  item: Content;
}

export function NewsListItem({ item }: NewsListItemProps) {
  const { author, date, introduction, title, _links } = item.getData<DocumentData>();

  // TODO: Implement link generation mechanism
  return (
    <div className="card mb-3">
      <div className="card-body">
        { title && (
          <h2 className="card-title">
            <a href={_links.site.href}>{title}</a>
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
  if (!props.showPagination) {
    return null;
  }

  return (
    <nav aria-label="News List Pagination">
      <ul className="pagination">
        <li className={`page-item ${props.previous ? '' : 'disabled'}`}>
          <a className="page-link" href={props.previous ? `?page=${props.previousPage}` : '#'} aria-label="Previous">
            <span aria-hidden="true">&laquo;</span>
            <span className="sr-only">Previous</span>
          </a>
        </li>
        { props.pageNumbersArray.map((page, key) => (
          <li key={key} className={`page-item ${page === props.currentPage ? 'active' : ''}`}>
            <a className="page-link" href={`?page=${page}`}>{page}</a>
          </li>
        )) }
        <li className={`page-item ${props.next ? '' : 'disabled'}`}>
          <a className="page-link" href={props.next ? `?page=${props.nextPage}` : '#'} aria-label="Next">
            <span aria-hidden="true">&raquo;</span>
            <span className="sr-only">Next</span>
          </a>
        </li>
      </ul>
    </nav>
  );
}
