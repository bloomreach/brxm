/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

export class MenuItemLink {
  appId: string;
  appPath: string;

  constructor(
    public id: string,
    public caption: string,
  ) {}
}
