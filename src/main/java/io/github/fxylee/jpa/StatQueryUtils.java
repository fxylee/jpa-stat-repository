package io.github.fxylee.jpa;

import java.util.List;
import javax.persistence.criteria.Expression;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.function.ParameterizedFunctionExpression;

public class StatQueryUtils {
  public static ParameterizedFunctionExpression<Long> multipleCountDistinct(
      CriteriaBuilderImpl builder,
      List<Expression<?>> expressions) {
    return new ParameterizedFunctionExpression<Long>(builder, Long.class,
        "count_distinct", expressions
    ) {
      @Override
      public String render(RenderingContext renderingContext) {
        renderingContext.getFunctionStack().push(this);

        String var3;
        try {
          StringBuilder buffer = new StringBuilder();
          buffer.append(this.getFunctionName()).append("(");

          super.renderArguments(buffer, renderingContext);
          var3 = buffer.append(')').toString();
        } finally {
          renderingContext.getFunctionStack().pop();
        }

        return var3;
      }
    };
  }
}
