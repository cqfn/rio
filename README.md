Reactive Input Output objects for Java:
 - Fine tuned: fast or memory effecient
 - [RS-TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) compatible (see tests for [publishers](https://github.com/g4s8/rio/blob/master/src/test/java/wtf/g4s8/rio/file/ReadFlowTest.java) and [subscribers](https://github.com/g4s8/rio/blob/master/src/test/java/wtf/g4s8/rio/file/WriteSubscriberTest.java))
 - Just one dependency: the only compile dependency is [JCTools](https://github.com/JCTools/JCTools) library with concurent queues

[![Maven Build](https://github.com/cqfn/rio/workflows/Maven%20Build/badge.svg)](https://github.com/cqfn/rio/actions?query=workflow%3A%22Maven+Build%22)
[![codecov](https://codecov.io/gh/cqfn/rio/branch/master/graph/badge.svg)](https://codecov.io/gh/cqfn/rio)
[![Maven Central](https://img.shields.io/maven-central/v/wtf.g4s8/rio.svg)](https://maven-badges.herokuapp.com/maven-central/wtf.g4s8/rio)

## Install

Add Maven dependency to `pom.xml`:
```xml
<dependency>
  <groupId>org.cqfn</groupId>
  <artifactId>rio</artifactId>
  <version><!-- see latest release --></version>
</dependency>
```

Or use snapshot from `central.artipie.com`:
```xml
<repositories>
  <repository>
    <name>Artipie central</name>
    <id>central.artipie.com</id>
    <url>https://central.artipie.com/cqfn/maven</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
<dependency>
  <groupId>org.cqfn</groupId>
  <artifactId>rio</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

The primary protocol for all objects is `Publisher<ByteBuffer>`, it's either accepted or provided
by various methods in main entry points.

### File API

To create new reactive file instance use constructor:
```java


var file = new File(Paths.get("/tmp/my/file.txt"));
```

## Read file

To read the file use `Publisher<ByteBuffer> content()` method of `File` object:
```java


Publisher<ByteBuffer> content = new File(Paths.get("/tmp/my/file.txt")).content();
```

The file will be read on demand with all respect to backpressure.
This default implementation uses 8 kilobytes buffer to read the file.

To tune the buffer allocation strategy use overloaded `Publisher<ByteBuffer> read(Buffers buffers)` method,
`Buffers` interface is responsible to provide new buffers for reading. Some standard implementations are
available at `Buffers.Standard` enum.

All read operations are performed in `ThreadPoolExecutor` by default, to specify executor service explicitely use
`content(ExecuorService)` or `content(Buffers, ExecutorService)` overloaded methods.

## Write file

To write reactive stream of `ByteBuffer`s to file use `CompletionStage<Void> write(Publisher<ByteBuffer> data)`
method:

```java



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

All write operations are performed in `ThreadPoolExecutor` by default, to specify executor service explicitely use
`write(Publisher<ByteBuffer> data, ExecuorService exec)` overloaded method.
