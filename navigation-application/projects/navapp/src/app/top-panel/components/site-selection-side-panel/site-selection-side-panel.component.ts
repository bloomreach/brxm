/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { animate, style, transition, trigger } from '@angular/animations';
import { ArrayDataSource } from '@angular/cdk/collections';
import { FlatTreeControl } from '@angular/cdk/tree';
import { Component, EventEmitter, HostBinding, Input, OnChanges, Output } from '@angular/core';
import { MatTreeFlattener } from '@angular/material';

import { Site } from '../../../models';

interface SiteFlatNode {
  id: number;
  expandable: boolean;
  name: string;
  level: number;
  isExpanded?: boolean;
}

@Component({
  selector: 'brna-site-selection-side-panel',
  templateUrl: 'site-selection-side-panel.component.html',
  styleUrls: ['site-selection-side-panel.component.scss'],
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ transform: 'translateX(100%)' }),
        animate('300ms ease-in-out', style({ transform: 'translateX(0%)' })),
      ]),
      transition(':leave', [
        animate('300ms ease-in-out', style({ transform: 'translateX(100%)' })),
      ]),
    ]),
  ],
})
export class SiteSelectionSidePanelComponent implements OnChanges {
  @Input()
  sites: Site[];

  @Input()
  selectedSite: Site;

  @Output()
  selectedSiteChange = new EventEmitter<Site>();

  @HostBinding('@slideInOut')
  animate = true;

  searchText = '';
  treeControl = new FlatTreeControl<SiteFlatNode>(node => node.level, node => node.expandable);
  treeFlattener = new MatTreeFlattener(
    this.transformer,
    node => node.level,
    node => node.expandable,
    node => node.subGroups,
  );
  dataSource: ArrayDataSource<SiteFlatNode>;

  private flatSites: SiteFlatNode[];

  get isNotFoundPanelVisible(): boolean {
    return this.flatSites && this.flatSites.length === 0 && !!this.searchText;
  }

  ngOnChanges(changes): void {
    this.updateDataSource();
  }

  hasChild(index: number, node: SiteFlatNode): boolean {
    return node.expandable;
  }

  onExpandCollapseIconClicked(node: SiteFlatNode): void {
    node.isExpanded = !node.isExpanded;
  }

  onNodeClicked(node: SiteFlatNode): void {
    this.selectedSiteChange.emit({ id: node.id, name: node.name });
  }

  isActive(node: Site): boolean {
    return this.selectedSite ? node.id === this.selectedSite.id : false;
  }

  shouldNotRender(node: SiteFlatNode): boolean {
    const path = this.buildPath(node);

    return path.some(x => !x.isExpanded);
  }

  onSearchInputKeyUp(): void {
    this.updateDataSource();
  }

  private updateDataSource(): void {
    const filteredSites = this.filterSites(this.sites, this.searchText);
    this.flatSites = this.treeFlattener.flattenNodes(filteredSites);
    this.expandActiveNode();
    this.dataSource = new ArrayDataSource(this.flatSites);
  }

  private getParentNode(node: SiteFlatNode): SiteFlatNode {
    const nodeIndex = this.flatSites.indexOf(node);

    for (let i = nodeIndex - 1; i >= 0; i--) {
      if (this.flatSites[i].level === node.level - 1) {
        return this.flatSites[i];
      }
    }

    return undefined;
  }

  private buildPath(node: SiteFlatNode): SiteFlatNode[] {
    const parents: SiteFlatNode[] = [];
    let currentNode = node;

    while (true) {
      const parent = this.getParentNode(currentNode);

      if (!parent) {
        break;
      }

      parents.push(parent);
      currentNode = parent;
    }

    return parents;
  }

  private expandActiveNode(): void {
    if (!this.selectedSite) {
      this.treeControl.collapseAll();
      return;
    }

    const node = this.flatSites.find(x => x.id === this.selectedSite.id);
    if (!node) {
      return;
    }

    let parent: SiteFlatNode = node;

    do {
      parent.isExpanded = true;
      parent = this.getParentNode(parent);
    } while (parent);
  }

  private filterSites(sites: Site[], searchText: string): Site[] {
    if (!sites || !sites.length) {
      return [];
    }

    searchText = searchText.toLowerCase();

    return sites.reduce((result, site) => {
      site = { ...site };

      if (site.name.toLowerCase().includes(searchText)) {
        result.push(site);
        return result;
      }

      site.subGroups = this.filterSites(site.subGroups, searchText);

      if (site.subGroups.length) {
        result.push(site);
      }

      return result;
    }, []);
  }

  private transformer(node: Site, level: number): SiteFlatNode {
    return {
      id: node.id,
      expandable: !!node.subGroups && node.subGroups.length > 0,
      name: node.name,
      level,
    };
  }
}
