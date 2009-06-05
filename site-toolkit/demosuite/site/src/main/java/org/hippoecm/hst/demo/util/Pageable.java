/*
 * Copyright 2009 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.demo.util;

/**
 * Pageable
 *
 * @author m.milicevic <me at machak.com>
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Adds pagable behavior: it calculates page size, next/previous offsets etc.
 * <p/>
 * Posible navigation can look something like:
 * <pre>
 * Previous  ...  11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 | 21 ... Next
 * </pre>
 */
public class Pageable{

	private int pageSize = 10;
	private int pageNumber = 1;
	private int visiblePages = 10;
	private int total;


	/**
	 * Constructor.
     * NOTE: you can always override <code><strong>setTotal()</strong></code>
	 * method in your own class if total number of items is not available immediately
	 *
	 * @param total total number of results query has returned
	 * @see #setTotal(int)
	 */
	public Pageable(int total){
		this.total = total;
	}


    /**
     * Returns current page number
     * @return pagenumber we are displaying
     */
    public int getCurrentPage(){
        return pageNumber;
    }

	/**
	 * Has currrent page previous pages?
	 *
	 * @return true if page is bigger than 1 false otherwise
	 */
	public boolean isPrevious(){
		return pageNumber > 1;
	}


	/**
	 * Has currrent page next pages?
	 *
	 * @return true if page is foloowed  by other pages
	 */
	public boolean isNext(){
		return getTotalPages() > pageNumber;
	}


	/**
	 * Does  pagenumber exceeds number of visible pages?
	 *
	 * @return true if so, false otherwise
	 */
	public boolean isPreviousBatch(){
		return pageNumber > visiblePages;
	}

	/**
	 * Is  pagenumber followed by next pages e.g. next 10
	 *
	 * @return true if so, false otherwise
	 */
	public boolean isNextBatch(){
		return getTotalPages() > getEndPage();
	}


	/**
	 * Returns a list of numbers (between start and end offset)
	 *
	 * @return List containing page numbers..
	 */
	public List<Integer> getPageNumbersArray(){
		int startPage = getStartPage();
		int endPage = getEndPage();
		List<Integer> pages = new ArrayList<Integer>();
		for(int i = startPage; i <= endPage; i++){
			pages.add(i);
		}
		return pages;
	}


	/**
	 * Get where result offset should start
	 * NOTE: it's zero based
	 *
	 * @return int
	 */
	public int getStartOffset(){
		int start = (pageNumber - 1) * pageSize;
		if(start >= total){
			start = 0;
		}
		return start;
	}

	/**
	 * get where result offset should end
	 *
	 * @return int
	 */
	public int getEndOffset(){
		int end = pageNumber * pageSize;
		if(end > total){
			end = total;
			if((end - getStartOffset()) > pageSize){
				end = pageSize;
			}
		}
		return end;
	}

	/**
	 * get end page of the current page set (e.g. in pages 1...10 it will return 10)
	 *
	 * @return end page nr. of page batch
	 */
	public int getEndPage(){
		final int startPage = getStartPage();
		int total_pages = getTotalPages();
		// boundary check
		if(pageNumber > total_pages){
			return total_pages;
		}
		int end = startPage + visiblePages - 1;
		return end > total_pages ? total_pages : end;
	}

	/**
	 * get start page of the offset, so, assuming visiblePages is set to 10:
	 * e.g. if pageNumber 3, it'll return 1,
	 * pagenumber 19, it'll return  11)
	 *
	 * @return int page number of visible page batch
	 */
	public int getStartPage(){

		if(pageNumber <= visiblePages || pageNumber == 1){
			return 1;
		}
		int start = pageNumber / visiblePages;
		int remainder = pageNumber % visiblePages;
		if(remainder == 0){
			return start * visiblePages - visiblePages + 1;
		}
		return start * visiblePages + 1;
	}

	/**
	 * rteurn total numer of pages (based on pagesize)
	 *
	 * @return nr. of pages
	 */
	public int getTotalPages(){
		int pages = total / pageSize;
		int remainder = total % pageSize;
		pages += remainder == 0 ? 0 : 1;
		return pages;
	}


	//=================================
	//NOTE:
	// a lot of bound checking is done pretty
	// (much monkey-proof setters)
	//================================
	public void setPageSize(int pageSize){
		this.pageSize = pageSize <= 0 ? 10 : pageSize;
	}

	public void setPageNumber(int pageNumber){
		if(pageNumber > getTotalPages()){
			this.pageNumber = 1;
		}
		this.pageNumber = pageNumber <= 0 ? 1 : pageNumber;
	}

	public void setVisiblePages(int visiblePages){
		if(visiblePages < 0){
			visiblePages = 10;
		}
		this.visiblePages = visiblePages;
	}

	public int getPageSize(){
		return pageSize;
	}

	public int getVisiblePages(){
		return visiblePages;
	}

	/**
	 * Total number of results.
	 *
	 * @return total nr. of results
	 */
	public int getTotal(){
		return total;
	}

	/**
	 * Sets total number of results.
	 *
	 * @param total number of results query returned/your collection holds
	 */
	public void setTotal(int total){
		if(total < 0){
			total = 0;
		}
		this.total = total;
	}
}


