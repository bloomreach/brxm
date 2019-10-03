# Example server-side React App

Example server-side React app using the BloomReach Experience SDK for React. The app uses [Next.js](https://github.com/zeit/next.js)
as framework for creating a server-side rendered app.

## Install and run

__TODO__: Start the BRX example Docker image

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

Finally, build and run the React app as followed:

```bash
yarn
yarn run dev
```

The CMS should now be accessible at <http://localhost:8080/cms>, and it should render the server-side React app in preview
mode in the Channel Manager. The SPA itself can be accessed directly via <http://localhost:3000>.
