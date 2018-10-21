package brave.context.rxjava2.internal.fuseable;

import brave.context.rxjava2.Internal;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.TraceContext;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import io.reactivex.internal.fuseable.ScalarCallable;

final class TraceContextScalarCallableMaybe<T> extends Maybe<T> implements ScalarCallable<T> {
  final MaybeSource<T> source;
  final CurrentTraceContext currentTraceContext;
  final TraceContext assemblyContext;

  TraceContextScalarCallableMaybe(
      MaybeSource<T> source,
      CurrentTraceContext currentTraceContext,
      TraceContext assemblyContext) {
    this.source = source;
    this.currentTraceContext = currentTraceContext;
    this.assemblyContext = assemblyContext;
  }

  @Override protected void subscribeActual(MaybeObserver<? super T> s) {
    Scope scope = currentTraceContext.maybeScope(assemblyContext);
    try { // retrolambda can't resolve this try/finally
      source.subscribe(Internal.instance.wrap(s, currentTraceContext, assemblyContext));
    } finally {
      scope.close();
    }
  }

  /**
   * A scalar value is computed at assembly time. Since call() is at runtime, we shouldn't add
   * overhead of scoping, only to return a constant!
   *
   * <p>See https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0#callable-and-scalarcallable
   */
  @Override @SuppressWarnings("unchecked") public T call() {
    return ((ScalarCallable<T>) source).call();
  }
}