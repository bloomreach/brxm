# brXM + Angular = ♥️

Example Angular SPA using the Bloomreach Experience [Angular SDK](https://www.npmjs.com/package/@bloomreach/ng-sdk).
This project was generated with [Angular CLI](https://github.com/angular/angular-cli).

## Install and run
Run [Docker](https://hub.docker.com/r/bloomreach/xm-spa-example) container with the configured brXM instance:
```bash
docker run --net=host bloomreach/xm-spa-example
```

First, change the application URL to `http://localhost:4200` in the [channel settings](http://localhost:8080/cms/console/?path=/hst:xmspaexample/hst:configurations/xmspaexample/hst:workspace/hst:channel/hst:channelinfo).

Then, update `src/environments/environment.ts` and `src/environments/environment.prod.ts` files to specify the brXM instance to fetch the page model from:
```
export const environment = {
  endpoint: 'http://localhost:8080/site/resourceapi',
  // ...
};
```

Finally, build and run the Angular app as follows:

```bash
yarn
yarn start
```

The CMS should now be accessible at <http://localhost:8080/cms>, and it should render the Angular app in preview mode in the Experience Manager.
The SPA itself can be accessed directly via <http://localhost:4200>.

## Available scripts

In the project directory, you can run:

### Development server

Run `yarn start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

For Angular Universal application, you can use `yarn dev:ssr`.

### Code scaffolding

Run `yarn ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

### Build

Run `yarn build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

### Launch

Run `yarn start:ssr` to start Angular Universal application.

### Running unit tests

Run `yarn test` to execute the unit tests.
