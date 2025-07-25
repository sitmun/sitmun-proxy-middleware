package org.sitmun.proxy.middleware.decorator;

public interface ObjectDecorator {

  boolean accept(Object target, Context context);

  default Object apply(Object target, Context context) throws Exception {
    if (accept(target, context)) {
      return addBehavior(target, context);
    }
    return null;
  }

  Object addBehavior(Object target, Context context) throws Exception;
}
