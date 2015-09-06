The DynamicConfig framework provides core abstractions for configuration property lookup and dynamic update of such properties. It was first designed as a simple facade on top of archaius due to its shortcoming when it comes to unit testing. Archaius provides no way to decouple the different configuration properties required by the application from the actual configuration source, making unit testing of classes consuming dynamic configuration parameters hard. DynamicConfig can be seen as a slf4j counterpart for configuration lookup.

### Example: DynamicConfig setup and usage
```java
// Create DynamicConfig from a single configuration source 
MapConfigSource configSource = new MapConfigSource();
DynamicConfig config = DynamicConfig.create(configSource);

// Read property from configuration and provide a default-value if non present.
DynamicBoolean fooProp = config.readBooleanProperty("foo", false);

// Since "foo" is not set in configSource it will fallback to default
assertFalse(fooProp.get());

// Update "foo" property in ConfigSource  
configSource.set("foo", "true");

// The new value of foo will be pushed to fooProp 
assertTrue(fooProp.get());


// Update "foo" property with unparsable value  
configSource.set("foo", "unparsableBoolean");

// The malformed value is ignored by the configuration framework. 
assertTrue(fooProp.get());
```



### Configuration chain
DynamicConfig allows configuration to be read from many different sources. A property is resolved to the first element discovered that can successfully be parsed by the framework. 
 
### Example: Configuration property resolution order
```java
// Create DynamicConfig from two different sources
MapConfigSource source1 = new MapConfigSource();
MapConfigSource source2 = new MapConfigSource();
DynamicConfig config = DynamicConfig.create(source1, source2);

DynamicBoolean fooProp = config.getBoolean("foo", false);

// When property not present in any source it will fallback to default value
assertFalse(fooProp.get());

// When property is updated in source2 it is propagated to fooProp
source2.set("foo", "true");
assertTrue(fooProp.get());

// When property is updated in source1 which takes precedence over source 2 it is 
// propagated to fooProp
source1.set("foo", "false");
assertTrue(fooProp.get());

// Property is updated in source1 to unparsable value, the value will not change in fooProp
// Since unparsable values are ignored by the framework
source1.set("foo", "bogus");
assertTrue(fooProp.get())
```



