# JVM Configuration Guide for IPjFIXsvc

## Overview

This document explains the JVM configuration for the IPjFIXsvc Spring Boot application that uses Apache Ignite and Lucene. The configuration is optimized for Java 21 and addresses the specific requirements of distributed systems and data-intensive workloads.

## Configuration Files

- **`.mvn/jvm.config`** - Development configuration with monitoring and debugging enabled
- **`.mvn/jvm.config.production`** - Production-optimized configuration

## Current Development Configuration (`.mvn/jvm.config`)

### Java Module System & Reflection Access
Required for Apache Ignite and Spring Boot to function correctly in Java 21:

```
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/sun.misc=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/javax.net.ssl=ALL-UNNAMED
--add-opens=java.base/java.security=ALL-UNNAMED
--add-opens=java.base/java.time=ALL-UNNAMED
--add-opens=java.sql/java.sql=ALL-UNNAMED
--add-opens=java.management/javax.management=ALL-UNNAMED
--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
--add-exports=java.base/sun.security.util=ALL-UNNAMED
--add-modules=java.se
--enable-native-access=ALL-UNNAMED
```

### Memory & Garbage Collection
Optimized for Ignite + Lucene workloads:

```
-Xms512m                    # Initial heap size
-Xmx2g                      # Maximum heap size
-XX:+UnlockExperimentalVMOptions  # Required for some G1 options
-XX:+UseG1GC                # G1 garbage collector for low-latency
-XX:MaxGCPauseMillis=200    # Target pause time
-XX:G1HeapRegionSize=16m    # Heap region size
-XX:G1NewSizePercent=20     # Young generation minimum
-XX:G1MaxNewSizePercent=40  # Young generation maximum
```

### Performance Optimizations

```
-XX:+UseCompressedOops       # Compress object pointers
-XX:+UseCompressedClassPointers  # Compress class pointers
-XX:+UseStringDeduplication  # Reduce memory for duplicate strings
-XX:+TieredCompilation       # Enable tiered compilation
-XX:TieredStopAtLevel=4      # Full optimization level
-XX:MaxDirectMemorySize=1g   # Direct memory for Ignite off-heap
```

### Monitoring & Debugging (Development Only)

```
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
-Xlog:gc*:logs/gc.log:time,tags
```

### Error Handling

```
-XX:+HeapDumpOnOutOfMemoryError  # Create heap dump on OOM
-XX:HeapDumpPath=logs/heapdump.hprof
-XX:+ExitOnOutOfMemoryError      # Exit on OOM (container-friendly)
```

### Network & Security

```
-Djava.net.preferIPv4Stack=true     # Prefer IPv4 (distributed systems)
-Dsun.net.useExclusiveBind=false
-Dnetworkaddress.cache.ttl=60       # DNS cache timeout
```

## Production Configuration (`.mvn/jvm.config.production`)

The production configuration:
- **Increases heap size**: `-Xms1g -Xmx4g` for production workloads
- **Removes development monitoring**: No JMX, no GC logging
- **Optimizes pause times**: `MaxGCPauseMillis=100` for better responsiveness
- **Increases direct memory**: `MaxDirectMemorySize=2g` for larger Ignite off-heap storage

## Memory Sizing Guidelines

### Development Environment
- **Heap**: 512MB - 2GB (current: `-Xms512m -Xmx2g`)
- **Direct Memory**: 1GB (for Ignite off-heap storage)
- **Total JVM Memory**: ~3GB

### Production Environment
- **Heap**: 1GB - 4GB+ (production config: `-Xms1g -Xmx4g`)
- **Direct Memory**: 2GB+ (for larger datasets)
- **Total JVM Memory**: 6GB+ recommended

### System Requirements
- **Development**: Minimum 8GB RAM
- **Production**: Minimum 16GB RAM (32GB+ recommended)

## Performance Considerations

### Garbage Collection
- **G1GC** is optimal for applications with:
  - Large heap sizes (> 1GB)
  - Low-latency requirements
  - Mixed workloads (like Ignite + Lucene)

### Ignite-Specific Optimizations
- **Off-heap storage**: Configured via `MaxDirectMemorySize`
- **Network optimizations**: IPv4 preference for cluster communication
- **JVM opens**: Required for Ignite's reflection-based serialization

### Lucene Optimizations
- **String deduplication**: Reduces memory for text indexing
- **Compressed OOPs**: Saves memory for object references
- **Tiered compilation**: Improves indexing performance

## Monitoring and Troubleshooting

### JMX Monitoring (Development)
- **URL**: `service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi`
- **Tools**: JConsole, JVisualVM, or any JMX client
- **Metrics**: Heap usage, GC performance, Ignite cache metrics

### GC Logging (Development)
- **Location**: `logs/gc.log`
- **Format**: Time and tag-based logging
- **Analysis**: Use GCPlot.com or similar tools

### Heap Dumps
- **Automatic**: Generated on OutOfMemoryError
- **Location**: `logs/heapdump.hprof` (dev) or `/tmp/heapdump.hprof` (prod)
- **Analysis**: Eclipse MAT, JVisualVM, or similar tools

## Switching Configurations

### For Production Deployment
```bash
cp .mvn/jvm.config.production .mvn/jvm.config
```

### For Development
```bash
# The default .mvn/jvm.config is already configured for development
```

## Common Issues and Solutions

### OutOfMemoryError: Java heap space
- **Solution**: Increase `-Xmx` value
- **Check**: Application memory usage patterns
- **Monitor**: GC logs for frequent full GC

### OutOfMemoryError: Direct buffer memory
- **Solution**: Increase `-XX:MaxDirectMemorySize`
- **Cause**: Usually Ignite off-heap storage needs more memory

### Poor GC Performance
- **Monitor**: GC pause times in logs
- **Tune**: Adjust `MaxGCPauseMillis`, heap sizing
- **Consider**: Different GC algorithm if needed

### Ignite Warnings
- **"Specify JVM heap max size"**: Already addressed in our config
- **"Set max direct memory size"**: Already configured
- **IPv4 preference**: Already configured

## Validation

The configuration has been tested and verified to:
- ✅ Compile successfully with Maven
- ✅ Start Apache Ignite without module access issues
- ✅ Initialize Lucene indexing
- ✅ Support OData endpoints
- ✅ Handle concurrent requests
- ✅ Provide proper error handling and monitoring

## References

- [Apache Ignite Memory Configuration](https://ignite.apache.org/docs/latest/memory-configuration/memory-configuration)
- [Java 21 JVM Options](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)
- [G1 Garbage Collector Tuning](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html)
- [Spring Boot JVM Configuration](https://docs.spring.io/spring-boot/how-to/properties-and-configuration.html)
