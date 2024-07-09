package io.github.fxylee.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Stat {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Dim {
    String name() default "";
  }

  @AliasFor("function")
  StatFunction value() default StatFunction.SUM;

  @AliasFor("value")
  StatFunction function() default StatFunction.SUM;

  String name() default "";
}
