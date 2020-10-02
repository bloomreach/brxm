/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, ElementRef, Input, OnInit } from '@angular/core';
import * as c3 from 'c3';
import * as d3 from 'd3';

import { ExperimentStatus, ExperimentStatusAtTimestamp } from '../../models/experiment-status.model';
import { ExperimentWithStatusData } from '../../models/experiment-with-status-data.model';

interface AxisDayRegion {
  axis: 'x';
  start: Date;
  end: Date;
}

@Component({
  selector: 'em-experiment-status-chart',
  template: '',
  styleUrls: ['experiment-status-chart.component.scss'],
})
export class ExperimentStatusChartComponent implements OnInit {
  @Input()
  experiment!: ExperimentWithStatusData;

  private chart!: c3.ChartAPI;

  constructor(private readonly elRef: ElementRef<HTMLElement>) {}

  get defaultChartConfiguration(): Partial<c3.ChartConfiguration> {
    return {
      bar: {width: {ratio: 0.8}},
      tooltip: {
        show: true,
        position: () => ({top: 0, left: 0}),
        format: {
          // title: d3.format('%d/%m %H:%M') as (x: c3.Primitive, index: number) => string,
          value: (value: number) => `${value} %`,
        },
      },
      axis: {
        x: {
          height: 60,
          type: 'timeseries',
          tick: {
            rotate: 45,
            format: this.formatXAxis(),
          },
        },
        y: {
          max: 100,
          padding: {
            top: 0,
          },
          show: false,
        },
      },
      padding: {
        left: 0,
      },
      legend: {
        show: true,
      },
      zoom: {enabled: true},
      size: {
        height: 200,
      },
    };
  }

  get dataTemplate(): c3.Data {
    return {
      json: [],
      keys: {value: this.variantIds, x: 'timestamp'},
      names: this.variantNames,
      type: 'area-step',
      order: 'asc',
    };
  }

  get variantIds(): string[] {
    return this.experiment.variants.map(v => v.variantId);
  }

  get variantNames(): { [key: string]: string } {
    return this.experiment.variants.reduce((names: { [key: string]: string }, v) => {
      names[v.variantId] = v.variantName;

      return names;
    }, {});
  }

  ngOnInit(): void {
    if (!this.experiment.status) {
      return;
    }

    this.chart = c3.generate({
      bindto: this.elRef.nativeElement,
      data: this.dataTemplate,
      ...this.defaultChartConfiguration,
    });

    this.updateData(this.experiment.status);
  }

  private formatXAxis(): string {
    const start = this.midnight(new Date(this.experiment.startTime));
    const lastWeek = this.midnight(this.incrementDate(new Date(), -7));
    const now = this.midnight(new Date());

    if (start >= now) {
      return '%H:%M';
    }

    if (start <= lastWeek) {
      return '%d/%m';
    }

    return '%d/%m %H:%M';
  }

  private updateData(points: ExperimentStatus): void {
    const data = this.dataTemplate;
    data.json = this.normalizeData(points);

    this.chart.load(data as any);
    this.chart.groups([this.variantIds]);
    this.chart.regions(this.dayRegions(points));
  }

  private normalizeData(points: ExperimentStatus): ExperimentStatus {
    return points.map(point => {
      const normalizedPoint: ExperimentStatusAtTimestamp = { timestamp: point.timestamp };
      const total = Object.keys(point).reduce((sum, key) => {
        return key !== 'timestamp' ? sum + point.key : sum;
      }, 0);

      if (total === 0) {
        return normalizedPoint;
      }

      let newTotal = 0;
      let maxKey: string | undefined;
      let maxValue = 0;

      for (const key of Object.keys(point)) {
        if (key === 'timestamp') {
          continue;
        }

        const normalizedValue = Math.round((100 * point.key) / total);
        normalizedPoint.key = normalizedValue;
        newTotal += normalizedValue;

        if (maxKey === undefined || normalizedPoint.key > maxValue) {
          maxKey = key;
          maxValue = normalizedPoint.key;
        }
      }

      if (newTotal > 100) {
        normalizedPoint.maxKey -= (newTotal - 100);
      }

      return normalizedPoint;
    });
  }

  /**
   * Create a list of {axis:'x', start:Date1, end:Date2} objects where Date1 and Date2 span
   * every other day of the time range covered by the data points, in such a way that the
   * date of the first data point is not covered.
   *
   * For example, if the points start at 2015-2-9 10:15 and end at 2015-2-12 8:45,
   * this function returns the spans 2015-2-10 0:00 .. 2015-2-11 0:00 and
   * 2015-2-12 0:00 .. 2015-2-13 0:00.
   *
   * This can be used to indicate day boundaries using a zebra pattern.
   * We make the regions start on the second day because that looks nicer when there's only
   * a single day.
   */
  private dayRegions(points: ExperimentStatus): AxisDayRegion[] {
    if (!points || points.length === 0) {
      return [];
    }

    const startDate = this.midnight(new Date(points[0].timestamp));
    const endDate = this.incrementDate(new Date(points[points.length - 1].timestamp), 1);
    const regions = [];

    for (let d = this.incrementDate(startDate, 1); d < endDate; d = this.incrementDate(d, 2)) {
      regions.push(
        { axis: 'x', start: d, end: this.incrementDate(d, 1) } as AxisDayRegion,
      );
    }

    return regions;
  }

  /**
   * Rounds the given datetime to whole-day granularity and adds the given number of days to it.
   */
  private incrementDate(date: Date, increment: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + increment);
  }

  private midnight(datetime: Date): Date {
    return this.incrementDate(datetime, 0);
  }
}
