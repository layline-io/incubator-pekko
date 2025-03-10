# mergeSorted

Merge multiple sources.

@ref[Fan-in operators](../index.md#fan-in-operators)

## Signature

@apidoc[Source.mergeSorted](Source) { scala="#mergeSorted[U&gt;:Out,M](that:org.apache.pekko.stream.Graph[org.apache.pekko.stream.SourceShape[U],M])(implicitord:Ordering[U]):FlowOps.this.Repr[U]" java="#mergeSorted(org.apache.pekko.stream.Graph,java.util.Comparator)" }
@apidoc[Flow.mergeSorted](Flow) { scala="#mergeSorted[U&gt;:Out,M](that:org.apache.pekko.stream.Graph[org.apache.pekko.stream.SourceShape[U],M])(implicitord:Ordering[U]):FlowOps.this.Repr[U]" java="#mergeSorted(org.apache.pekko.stream.Graph,java.util.Comparator)" }


## Description

Merge multiple sources. Waits for one element to be ready from each input stream and emits the
smallest element.

## Example
Scala
:   @@snip [FlowMergeSpec.scala](/akka-stream-tests/src/test/scala/org/apache/pekko/stream/scaladsl/FlowMergeSpec.scala) { #merge-sorted }

Java
:   @@snip [SourceOrFlow.java](/docs/src/test/java/jdocs/stream/operators/SourceOrFlow.java) { #merge-sorted }

## Reactive Streams semantics

@@@div { .callout }

**emits** when all of the inputs have an element available

**backpressures** when downstream backpressures

**completes** when all upstreams complete

@@@
