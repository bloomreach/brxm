# brXM + Next.js = ♥️

Example Next.js SPA using the Bloomreach Experience [React SDK](https://www.npmjs.com/package/@bloomreach/react-sdk).
The app uses unversal framework [Next.js](https://github.com/zeit/next.js) for creating isomorphic React applications.

## Install and run
Run [Docker](https://hub.docker.com/repository/docker/bloomreach/xm-spa-example) container with the configured brXM instance:
```bash
docker run --net=host bloomreach/xm-spa-example
```

Then, copy `.env.dist` file to `.env` and customize it to contain a correct `PUBLIC_URL` path, for example:
```
PUBLIC_URL=http://localhost:3000
```

In the same `.env` file, also specify the brXM instance to fetch the page model from:
```
BR_URL_LIVE=http://localhost:8080/site/spa-ssr/resourceapi
SPA_BASE_PATH_LIVE=
BR_URL_PREVIEW=http://localhost:8080/site/_cmsinternal/spa-ssr/resourceapi
SPA_BASE_PATH_PREVIEW=/site/_cmsinternal/spa-csr
```

Finally, build and run the Next.js app as followed:

```bash
yarn
yarn run dev
```

The CMS should now be accessible at <http://localhost:8080/cms>, and it should render the Next.js app in preview mode in the Experience manager.
The SPA itself can be accessed directly via <http://localhost:3000>.
