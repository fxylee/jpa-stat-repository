package io.github.fxylee.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

public enum StatFunction {
  SUM,
  COUNT,
  COUNT_DISTINCT,
  AVG {
    @Override
    public <N extends Number> Expression<Double> toExp(CriteriaBuilder cb, Expression<N> x) {
      return cb.avg(x);
    }
  };

  public <N extends Number> Expression<?> toExp(CriteriaBuilder cb, Expression<N> x) {
    switch (this) {
      case SUM:
        return cb.sum(x);
      case COUNT:
        return cb.count(x);
      case COUNT_DISTINCT:
        return cb.countDistinct(x);
      case AVG:
        return cb.avg(x);
      default:
        throw new RuntimeException("");
    }
  }
}
