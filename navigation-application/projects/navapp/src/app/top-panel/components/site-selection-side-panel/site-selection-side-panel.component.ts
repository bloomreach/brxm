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
import { FlatTreeControl } from '@angular/cdk/tree';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostBinding,
  Input,
  OnChanges,
  Output,
} from '@angular/core';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material';
import { Site } from '@bloomreach/navapp-communication';

interface SiteFlatNode {
  accountId: number;
  siteId: number;
  expandable: boolean;
  name: string;
  level: number;
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
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  treeControl = new FlatTreeControl<SiteFlatNode>(
    node => node.level,
    node => node.expandable,
  );
  treeFlattener = new MatTreeFlattener(
    this.transformer,
    node => node.level,
    node => node.expandable,
    node => node.subGroups,
  );
  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  get isNotFoundPanelVisible(): boolean {
    return (
      this.dataSource.data &&
      this.dataSource.data.length === 0 &&
      !!this.searchText
    );
  }

  ngOnChanges(changes): void {
    this.updateDataSource();
  }

  hasChild(index: number, node: SiteFlatNode): boolean {
    return node.expandable;
  }

  onNodeClicked(node: SiteFlatNode): void {
    this.selectedSiteChange.emit({ accountId: node.accountId, siteId: node.siteId, name: node.name });
  }

  isActive(node: Site): boolean {
    return this.selectedSite ? (node.accountId === this.selectedSite.accountId && node.siteId === this.selectedSite.siteId) : false;
  }

  onSearchInputKeyUp(): void {
    this.updateDataSource();
  }

  private updateDataSource(): void {
    this.dataSource.data = this.filterSites(this.sites, this.searchText);
    this.expandActiveNode();
  }

  private expandActiveNode(): void {
    if (!this.selectedSite) {
      this.treeControl.collapseAll();
      return;
    }

    const selectedNode = this.treeControl.dataNodes.find(
      x => x.accountId === this.selectedSite.accountId && x.siteId === this.selectedSite.siteId,
    );

    if (!selectedNode) {
      this.treeControl.collapseAll();
      return;
    }

    const path = this.buildPath(this.treeControl.dataNodes, selectedNode);

    path.forEach(node => this.treeControl.expand(node));
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
      accountId: node.accountId,
      siteId: node.siteId,
      expandable: !!node.subGroups && node.subGroups.length > 0,
      name: node.name,
      level,
    };
  }

  private buildPath(nodes: SiteFlatNode[], node: SiteFlatNode): SiteFlatNode[] {
    const parents: SiteFlatNode[] = [];
    let currentNode = node;

    while (true) {
      const parent = this.getParentNode(nodes, currentNode);

      if (!parent) {
        break;
      }

      parents.unshift(parent);
      currentNode = parent;
    }

    return parents;
  }

  private getParentNode(
    nodes: SiteFlatNode[],
    node: SiteFlatNode,
  ): SiteFlatNode {
    const nodeIndex = nodes.indexOf(node);

    for (let i = nodeIndex - 1; i >= 0; i--) {
      if (nodes[i].level === node.level - 1) {
        return nodes[i];
      }
    }

    return undefined;
  }
}
