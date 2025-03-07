# RetryFlow.withBackoff

Wrap the given @apidoc[Flow] and retry individual elements in that stream with an exponential backoff. A decider function tests every emitted element and can return a new element to be sent to the wrapped flow for another try.

@ref[Error handling](../index.md#error-handling)

## Signature

@apidoc[RetryFlow.withBackoff](RetryFlow$) { scala="#withBackoff[In,Out,Mat](minBackoff:scala.concurrent.duration.FiniteDuration,maxBackoff:scala.concurrent.duration.FiniteDuration,randomFactor:Double,maxRetries:Int,flow:org.apache.pekko.stream.scaladsl.Flow[In,Out,Mat])(decideRetry:(In,Out)=&gt;Option[In]):org.apache.pekko.stream.scaladsl.Flow[In,Out,Mat]" java="#withBackoff(java.time.Duration,java.time.Duration,double,int,org.apache.pekko.stream.javadsl.Flow,org.apache.pekko.japi.function.Function2)" }

## Description

When an element is emitted by the wrapped `flow` it is passed to the `decideRetry` function, which may return an element to retry in the `flow`. 

The retry backoff is controlled by the `minBackoff`, `maxBackoff` and `randomFactor` parameters.
At most `maxRetries` will be made after the initial try.

The wrapped `flow` must have **one-in one-out semantics**. It may not filter, nor duplicate elements. The `RetryFlow` will fail if two elements are emitted from the `flow`, it will be stuck "forever" if nothing is emitted. Just one element will be emitted into the `flow` at any time. The `flow` needs to emit an element before the next will be emitted to it. 

Elements are retried as long as `maxRetries` is not reached and the `decideRetry` function returns a new element to be sent to `flow`. The `decideRetry` function gets passed in the original element sent to the `flow` and the element emitted by it.
When `decideRetry` returns @scala[`None`]@java[`Optional.empty`], no retries will be issued, and the response will be emitted downstream.

@@@ note

This API was added in Akka 2.6.0 and @ref:[may be changed](../../../common/may-change.md) in further patch releases.

@@@

This example wraps a `flow` handling @scala[`Int`s]@java[`Integer`s], and retries elements unless the result is 0 or negative, or `maxRetries` is hit.

Scala
:   @@snip [RetryFlowSpec.scala](/akka-stream-tests/src/test/scala/org/apache/pekko/stream/scaladsl/RetryFlowSpec.scala) { #withBackoff-demo }

Java
:   @@snip [RetryFlowTest.java](/akka-stream-tests/src/test/java/org/apache/pekko/stream/javadsl/RetryFlowTest.java) { #withBackoff-demo }

## Reactive Streams semantics

@@@div { .callout }

**emits** when the wrapped flow emits, and either `maxRetries` is reached or `decideRetry` returns @scala[`None`]@java[`Optional.empty`]

**backpressures** during backoff, when the wrapped flow backpressures, or when downstream backpressures

**completes** when upstream or the wrapped flow completes

**cancels** when downstream or the wrapped flow cancels

@@@
