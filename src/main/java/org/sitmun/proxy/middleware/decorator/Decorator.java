package org.sitmun.proxy.middleware.decorator;

public interface Decorator {

  boolean accept(Object target, Context context);

  default void apply(Object target, Context context) {
    if (accept(target, context)) {
      addBehavior(target, context);
    }
  }

  void addBehavior(Object target, Context context);
}
