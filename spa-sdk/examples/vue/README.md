# brXM + Vue.js = ♥️
Example Vue.js SPA using the Bloomreach Experience [Vue.js SDK](https://www.npmjs.com/package/@bloomreach/vue-sdk).
This project was generated with [Vue CLI](https://cli.vuejs.org/).

## Install and run
Run [Docker](https://hub.docker.com/r/bloomreach/xm-spa-example) container with the configured brXM instance:
```bash
docker run -p 8080:8080 bloomreach/xm-spa-example
```

Then, copy `.env.dist` file to `.env` and specify the brXM instance to fetch the page model from:
```
VUE_APP_BRXM_ENDPOINT=http://localhost:8080/site/resourceapi
```

Finally, build and run the application as follows:

```bash
npm install
npm run serve -- --port=3000
```

The CMS should now be accessible at <http://localhost:8080/cms>, and it should render the application in preview mode in the Experience Manager.
The SPA itself can be accessed directly via <http://localhost:3000>.

## Available scripts
In the project directory, you can run:

### Compiles and hot-reloads for development
```bash
npm run serve
```

### Compiles and minifies for production
```bash
npm run build
```

### Run your unit tests
```bash
npm run test:unit
```

### Lints and fixes files
```bash
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).
