Reactive Input Output objects for Java:
 - Fine tuned: fast or memory effecient
 - [RS-TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) compatible (see tests for publishers and subscribers)
 - No dependencies: the only dependency is `org.reactivestreams:reactive-streams` interfaces

TODO: build badges

## Install

Add Maven dependency to `pom.xml`:
```xml
<dependency>
  <groupId>wtf.g4s8</groupId>
  <artifactId>rio</artifactId>
  <version><!-- see latest release -->
</dependency>
```

## Usage

The primary protocol for all objects is `Publisher<ByteBuffer>`, it's either accepted or provided
by various methods in main entry points.

### File API

To create new reactive file instance use constructor:
```java
import java.nio.file.Paths;
import wtf.g4s8.rio.file.File;

var file = new File(Paths.get("/tmp/my/file.txt"));
```

## Read file

To read the file use `Publisher<ByteBuffer> content()` method of `File` object:
```java
import java.nio.file.Paths;
import org.reactivestreams.Publisher;
import wtf.g4s8.rio.file.File;

Publisher<ByteBuffer> content = new File(Paths.get("/tmp/my/file.txt")).content();
```

The file will be read on demand (`request` method of `Subscription`) with all respect to backpressue.
This default implementation uses 8 kilobytes buffer to read the file.

To tune the buffer allocation strategy use overloaded `Publisher<ByteBuffer> read(Buffers buffers)` method,
`Buffers` interface is responsible to provide new buffers for reading. Some standard implementations are
available at `Buffers.Standard` enum.

## Write file

To write reactive stream of `ByteBuffer`s to file use `CompletionStage<Void> write(Publisher<ByteBuffer> data)`
method:

```java

import java.nio.file.Paths;
import org.reactivestreams.Publisher;
import wtf.g4s8.rio.file.File;

CompletionStage<Void> result = new File(Paths.get("/tmp/my/file.txt")).write(data);
```
It returns `CompletionStage` to signal errors or complete events. Also, it supports cancellation via
`cancel()` method of `CompletableFuture`:
```java
// will stop writing after subscribe
CompletionStage<Void> result = new File(Paths.get("/tmp/my/file.txt")).write(data);
Thread.sleep(1000);
result.toCompletableFuture().cancel(); // cancel writing after one second
```

Method `write` supports `OpenOption`s from `java.nio.file` as second varargs parameter:
`file.write(data, StandardOpenOptions.WRITE, StandardOpenOptions.CREATE_NEW)`, by default it uses
`WRITE` and `CREATE` options if not provided any.

To fine tune the speed or memory usage of write, the client is able to configure the `WriteGreed` level.
It configures the amount of buffers to requests and when. By default it requests 3 buffers at the beginning and
when writing last buffer (one befire the end):
```
[subscribe] | [write] [write] [write] | [write] [write] [write] |
 request(3) |          request(3)     |          request(3)
```
to consume less memory `WriteGreed.SINGLE` can be used which requests one by one buffer at the beginning and after
write operation. These values can be configured via `write` overloaded 2-nd parameter:
```java
file.write(path, new WriteGreed.Constant(10, 2)) // request 10 buffers when read 8
```
or via system properties for default imeplementation:
```java
# request 10 buffers when read 8 (only for default write method)
java -Drio.file.write.greed.amount=10 -Drio.file.write.greed.shift=2
```
