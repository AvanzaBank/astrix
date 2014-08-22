=== service-suite-provider-context === 
This is the runtime-module for service-providers. 

For instance: From the pu-application we wan't to expose a set of services. Therefore we use this module
to create a "serice-provider-runtime" which allows us to export different services over the service-bus.

Note that this is NOT the module used by service-consumers to bind to a service-provider.


TODO: is this more like a service-suite-GS-provider-context??? I.E the context used by applications running as a pu exposing different services?
Does a standalone REST-application have enough in common with a pu to motivate the existence of a commmon provider-context module? 