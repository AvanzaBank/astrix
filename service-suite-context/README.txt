=== service-suite-context ===
This is the "API" for service-consumers. It is used by service consumers to bootstrap the service-suite within an consuming application. 

Example: From the web-application we wan't to consume a set of services (TradingService, CustomerClient etc). 
Therefore we use this module create a "service-suite-runtime" which allows us to bind to different services.

Note that this is NOT the module used by service-providers to register provided services.