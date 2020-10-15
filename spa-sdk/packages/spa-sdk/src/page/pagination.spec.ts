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

import { PaginationImpl, PaginationModel, Pagination, TYPE_PAGINATION, isPagination } from './pagination';
import { PaginationItemFactory, PaginationItemModel } from './pagination-item';

let paginationItemFactory: jest.MockedFunction<PaginationItemFactory>;

const model = {
  current: {},
  enabled: true,
  first: {},
  items: [{}, {}],
  last: {},
  offset: 10,
  pages: [{}, {}],
  size: 5,
  total: 15,
  type: TYPE_PAGINATION,
} as PaginationModel;

function createPagination(paginationModel = model) {
  return new PaginationImpl(paginationModel, paginationItemFactory);
}

beforeEach(() => {
  paginationItemFactory = jest.fn(item => item) as unknown as typeof paginationItemFactory;
});

describe('PaginationImpl', () => {
  let pagination: Pagination;

  beforeEach(() => {
    pagination = createPagination();
  });

  describe('getCurrent', () => {
    it('should return the current page', () => {
      expect(pagination.getCurrent()).toBe(model.current);
      expect(paginationItemFactory).toBeCalledWith(model.current);
    });
  });

  describe('getFirst', () => {
    it('should return the first page', () => {
      expect(pagination.getFirst()).toBe(model.first);
      expect(paginationItemFactory).toBeCalledWith(model.first);
    });
  });

  describe('getItems', () => {
    it('should return the current page items', () => {
      expect(pagination.getItems()).toBe(model.items);
    });
  });

  describe('getLast', () => {
    it('should return the last page', () => {
      expect(pagination.getLast()).toBe(model.last);
      expect(paginationItemFactory).toBeCalledWith(model.last);
    });
  });

  describe('getNext', () => {
    it('should return undefined when there is no next page', () => {
      expect(pagination.getNext()).toBeUndefined();
    });

    it('should return the next page', () => {
      const next = {} as PaginationItemModel;
      const pagination = createPagination({ ...model, next });

      expect(pagination.getNext()).toBe(next);
      expect(paginationItemFactory).toBeCalledWith(next);
    });
  });

  describe('getOffset', () => {
    it('should return the number of items before the current page', () => {
      expect(pagination.getOffset()).toBe(10);
    });
  });

  describe('getPages', () => {
    it('should return currently listed pages', () => {
      expect(pagination.getPages()).toEqual(model.pages);
      expect(paginationItemFactory).toBeCalledWith(model.pages[0]);
      expect(paginationItemFactory).toBeCalledWith(model.pages[1]);
    });
  });

  describe('getPrevious', () => {
    it('should return undefined when there is no previous page', () => {
      expect(pagination.getNext()).toBeUndefined();
    });

    it('should return the previous page', () => {
      const previous = {} as PaginationItemModel;
      const pagination = createPagination({ ...model, previous });

      expect(pagination.getPrevious()).toBe(previous);
      expect(paginationItemFactory).toBeCalledWith(previous);
    });
  });

  describe('getOffset', () => {
    it('should return the number of items listed on the current page', () => {
      expect(pagination.getSize()).toBe(5);
    });
  });

  describe('getTotal', () => {
    it('should return the total number of items', () => {
      expect(pagination.getTotal()).toBe(15);
    });
  });

  describe('isEnabled', () => {
    it('should return true', () => {
      expect(pagination.isEnabled()).toBe(true);
    });

    it('should return false', () => {
      const pagination = createPagination({ ...model, enabled: false });

      expect(pagination.isEnabled()).toBe(false);
    });
  });
});

describe('isPagination', () => {
  it('should return true', () => {
    const pagination = createPagination();

    expect(isPagination(pagination)).toBe(true);
  });

  it('should return false', () => {
    expect(isPagination(undefined)).toBe(false);
    expect(isPagination({})).toBe(false);
  });
});
