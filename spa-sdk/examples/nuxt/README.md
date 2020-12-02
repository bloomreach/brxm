# brXM + Nuxt.js = ♥️
Example Vue.js SPA using the Bloomreach Experience [Vue.js SDK](https://www.npmjs.com/package/@bloomreach/vue-sdk).
This project was generated with [create-nuxt-app](https://nuxtjs.org/guide/installation/).

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
npm run dev
```

The CMS should now be accessible at <http://localhost:8080/cms>, and it should render the application in preview mode in the Experience Manager.
The SPA itself can be accessed directly via <http://localhost:3000>.

## Available scripts
In the project directory, you can run:

### Development server
```bash
npm run dev
```

### Build
```bash
npm run build
```

### Launch
```bash
npm run start
```

### Generate static project
```bash
npm run generate
```

### Run linter
```bash
npm run lint
```

For detailed explanation on how things work, check out [Nuxt.js docs](https://nuxtjs.org).
