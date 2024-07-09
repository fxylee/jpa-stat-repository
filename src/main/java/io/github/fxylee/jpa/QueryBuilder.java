package io.github.fxylee.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.springframework.data.jpa.domain.Specification;

public final class QueryBuilder<T, R> {
  private final EntityManager entityManager;
  private final Class<R> resultPoJo;
  private final Class<T> fromEntity;

  private final CriteriaBuilder criteriaBuilder;
  private final CriteriaQuery<R> query;
  private final Root<T> root;

  private final List<Expression<?>> metrics = new ArrayList<>();
  private final List<Expression<?>> dims = new ArrayList<>();
  private Specification<T> spec;

  public QueryBuilder(EntityManager entityManager, Class<T> fromEntity, Class<R> resultPoJo) {
    this.entityManager = entityManager;
    this.resultPoJo = resultPoJo;
    this.fromEntity = fromEntity;

    this.criteriaBuilder = entityManager.getCriteriaBuilder();
    this.query = criteriaBuilder.createQuery(resultPoJo);
    this.root = query.from(fromEntity);
  }

  public QueryBuilder<T, R> count(String attr) {
    metrics.add(criteriaBuilder.count(root.get(attr)));
    return this;
  }

  public QueryBuilder<T, R> countDistinct(String attr) {
    metrics.add(criteriaBuilder.countDistinct(root.get(attr)));
    return this;
  }

  public QueryBuilder<T, R> sum(String attr) {
    metrics.add(criteriaBuilder.sum(root.get(attr)));
    return this;
  }

  public QueryBuilder<T, R> avg(String attr) {
    metrics.add(criteriaBuilder.avg(root.get(attr)));
    return this;
  }

  public QueryBuilder<T, R> max(String attr) {
    metrics.add(criteriaBuilder.max(root.get(attr)));
    return this;
  }

  public QueryBuilder<T, R> groupBy(String... attributes) {
    for (String attribute : attributes) {
      dims.add(root.get(attribute));
    }
    return this;
  }

  public QueryBuilder<T, R> where(Specification<T> spec) {
    this.spec = spec;
    return this;
  }

  public TypedQuery<R> createQuery() {
    ArrayList<Expression<?>> columns = new ArrayList<>();
    columns.addAll(dims);
    columns.addAll(metrics);

    query.select(criteriaBuilder.construct(resultPoJo, columns.toArray(new Selection[0])));
    query.groupBy(dims.toArray(new Expression[0]));
    Optional.ofNullable(spec)
        .map(s -> s.toPredicate(root, query, criteriaBuilder))
        .ifPresent(query::where);

    return entityManager.createQuery(query);
  }

  public List<R> getResultList() {
    return createQuery().getResultList();
  }

  public Stream<R> getResultStream() {
    return createQuery().getResultStream();
  }

  public R getSingleResult() {
    return createQuery().getSingleResult();
  }
}
