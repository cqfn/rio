Reactive Input Output objects for Java:
 - Fine tuned: fast or memory effecient
 - [RS-TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) compatible (see tests for [publishers](https://github.com/g4s8/rio/blob/master/src/test/java/wtf/g4s8/rio/file/ReadFlowTest.java) and [subscribers](https://github.com/g4s8/rio/blob/master/src/test/java/wtf/g4s8/rio/file/WriteSubscriberTest.java))
 - Just one dependency: the only compile dependency is [JCTools](https://github.com/JCTools/JCTools) library with concurent queues

[![Maven Build](https://github.com/cqfn/rio/workflows/Maven%20Build/badge.svg)](https://github.com/cqfn/rio/actions?query=workflow%3A%22Maven+Build%22)
[![codecov](https://codecov.io/gh/cqfn/rio/branch/master/graph/badge.svg)](https://codecov.io/gh/cqfn/rio)
[![Maven Central](https://img.shields.io/maven-central/v/org.cqfn/rio.svg)](https://maven-badges.herokuapp.com/maven-central/org.cqfn/rio)

## Install

Add Maven dependency to `pom.xml`:
```xml
<dependency>
  <groupId>org.cqfn</groupId>
  <artifactId>rio</artifactId>
  <version><!-- see latest release --></version>
</dependency>
```
Snapshots are available at `https://central.artipie.com/cqfn/maven` Maven repo.

## Usage

The primary protocol for all objects is `Publisher<ByteBuffer>`, it's either accepted or provided
by various methods in main entry points.

### Files

To create new reactive file instance use the constructor:
```java
var file = new File(Paths.get("/tmp/my/file.txt"));
```

`File` instance provides multiple `content()` overloaded methods for reading, all of them returns `Publisher<ByteBuffer>`.
It's possible to specify `Buffers` allocation strategy and `ExecutorService` for subscriber callbacks. By default `contet()` method
allocates `8KB` buffers for each read and performs `Subscriber` calls on the same thread as IO reader task.
For wriging, `File` has `write(Publisher<ByteBuffer>)` overloaded methods, where the user can specify file's `OpenOptions` and `WriteGreed` (see "appendix"
section for more details), by default the `WriteGreed` is a `(3,1)`. `write()` methods returns `CompletionStage` instance, that can be
used to handle completion signal, errors, and to perform cancellation.

*Examples:*

*Copy one file to another using `1KB` buffer chunks:*
```java
var destination = new File(Path.get("out.txt"));
var source = new File(Path.get("in.txt"));
destination.write(source.content(Buffers.1K));
```

*Calculate SHA256 of file reactively (with RxJava `Flowable` reducer):*
```java
var sha256 = Flowable.fromPublisher(new File(Path.get("target")).readuceWith(
  () -> MessageDigest.getInstance("SHA-256"),
  (digest, buf) -> {
    digest.update(buf);
    return digest;
  }
).map(MessageDigest::digest).blockingGet();
```

## Channels

RIO has two wrappers for channels from java.nio:
 - `WritableChannel` as reactive wrapper for `WritableByteChannel`
 - `ReadableChannel` as reactive wrapper for `ReadableByteChannel`

`WritableChannel` accepts any instance of `WritableByteChannel` insterface and exposes
`write(Publisher<ByteBuffer>)` method overloads which accepts publisher of byte buffers to write into the
channel, it returns `CompletionStage<Void>` for completions and error events, and cancellation.

`ReadableChannel` wraps `ReadableByteChannel` and exposes `read()` overloaded methods to
return `Publisher<ByteBuffer>` read from the channel.

## Streams

Reactive wrappers for old Java IO streams API are similar to channels:
 - `ReactiveInputStream` to wrap `InputStreams`s and expose `read()` methods to return `Publisher<ByteBuffer>`
 - `ReactiveOutputStream` to wrap `OutputStream`s and provide `write(Publisher<ByteBuffer>)` methods

# Configuration

## Buffers

RIO providers `Buffers` class to specify buffers allocation strategy, usually the instance of
this interface is accepted by reading methods. It provides new `ByteBuffers` when new read request is performed.
Some standard strategies could be found in `Buffers.Standard` enum, the default value for all methods is `8KB`
standard enum value. Since `Buffers` is interface, it's possible to implement custom allocation strategy in client's code.

## Greed

To fine tune the speed or memory usage of write, the client is able to configure the `WriteGreed` level.
It configures the amount of buffers to requests and when. By default it's `(3,1)` it requests 3 buffers at the beginning and
requesting 3 buffers when the last-1 buffer was received:
```
[subscribe] || [write] | [write] | [write] || [write] | [write] | [write] ||
     req(3) ||         |  req(3) |         ||         |  req(3) |         ||
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
