Intent of Repository Data Submodules
====================================

The repository-data module contains all definitions to be bootstrapped into the repository.
It is sub-divided into four sub-modules:

  1) application
  2) development
  3) site
  4) webfiles

Repository data vital to the application (i.e. which should be bootstrapped into any environment)
should go into the *application* module, unless it is specifically related to a site delivery
implementation. In fresh projects, the auto-export mechanism is configured such that all exported
repository-data outside the HST root configuration node is added to the application submodule.

Repository data intended for development environments only (i.e. local or CI environments) should
go into the *development* module. By default, this module is available to the bootstrapping mechanism
when deploying the application locally (-Pcargo.run), but not included when building a distribution
(-Pdist). In order to include the development module in a distribution, build it with
-Pdist-with-development-data. You can add repository data into the development module by moving YAML
sources, or by configuring auto-export to export certain repository paths to the development module.

As of version 13.0.0, there is also a separate *site* module that contains only configuration related
to a site delivery implementation. This configuration is normally packaged in the site webapp and not
the platform / CMS webapp, so it must be maintained in a separate module. Again, the default auto-
export configuration will automatically place site-related config data into this module. It should be
deployed to all environments.

The *webfiles* module is intended to contribute webfiles (only) to the repository data. Like the
application module, it is intended to be included on every environment. As of version 13.0.0, this is
typically packaged with the site webapp.

If your application requires so, you can create more repository-data submodules to be deployed to the
environments of your liking, similar to above described default setup.