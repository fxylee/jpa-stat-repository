package io.github.fxylee.jpa;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import lombok.SneakyThrows;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.StringUtils;

//CHECKSTYLE:OFF
@NoRepositoryBean
public class StatRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID>
    implements StatRepository<T, ID> {
  private final JpaEntityInformation<T, ID> entityInformation;
  private final EntityManager entityManager;
  private final Class<T> fromEntity;
  private final String idName;
  private final Class<ID> idClass;

  public StatRepositoryImpl(JpaEntityInformation<T, ID> entityInformation,
                            EntityManager entityManager) {
    super(entityInformation, entityManager);

    this.entityInformation = entityInformation;
    this.entityManager = entityManager;
    this.fromEntity = entityInformation.getJavaType();
    this.idName = entityInformation.getRequiredIdAttribute().getName();
    this.idClass = entityInformation.getIdType();
  }

  private <S> TypedQuery<S> applyRepositoryMethodMetadata(TypedQuery<S> query) {
    if (getRepositoryMethodMetadata() == null) {
      return query;
    }

    LockModeType type = getRepositoryMethodMetadata().getLockModeType();
    TypedQuery<S> toReturn = type == null ? query : query.setLockMode(type);

    applyQueryHints(toReturn);

    return toReturn;
  }

  private void applyQueryHints(Query query) {
    getQueryHints().withFetchGraphs(entityManager).forEach(query::setHint);
  }

  @Override
  public <R> QueryBuilder<T, R> builder(Class<R> resultPoJoClass) {
    return new QueryBuilder<>(entityManager, fromEntity, resultPoJoClass);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Override
  public <V> V sum(String attr, Specification<T> spec) {
    Field valueField = fromEntity.getDeclaredField(attr);
    Class<V> valueClass = (Class<V>) valueField.getType();

    return Optional.ofNullable(builder(Tuple.class).sum(attr).where(spec).getSingleResult())
        .map(tuple -> tuple.get(0, valueClass))
        .orElse(null);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Override
  public <K, V> Map<K, V> sum(String keyAttr, String valueAttr, Specification<T> spec) {
    Field keyField = fromEntity.getDeclaredField(keyAttr);
    Class<K> keyClass = (Class<K>) keyField.getType();
    Field valueField = fromEntity.getDeclaredField(valueAttr);
    Class<V> valueClass = (Class<V>) valueField.getType();

    return applyRepositoryMethodMetadata(
        builder(Tuple.class).sum(valueAttr).groupBy(keyAttr).where(spec).createQuery()
    )
        .getResultList()
        .stream()
        .collect(Utils.toMap(x -> x.get(0, keyClass), x -> x.get(1, valueClass)));
  }

  @Override
  public Long count(String attr, Specification<T> spec) {
    return builder(Long.class).count(attr).where(spec).getSingleResult();
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Override
  public <K> Map<K, Long> count(String keyAttr, String valueAttr, Specification<T> spec) {
    Field keyField = fromEntity.getDeclaredField(keyAttr);
    Class<K> keyClass = (Class<K>) keyField.getType();

    return applyRepositoryMethodMetadata(
        builder(Tuple.class).count(valueAttr).groupBy(keyAttr).where(spec).createQuery()
    )
        .getResultList()
        .stream()
        .collect(Utils.toMap(x -> x.get(0, keyClass), x -> x.get(1, Long.class)));
  }

  @Override
  public Long countDistinct(String attr, Specification<T> spec) {
    return builder(Long.class).countDistinct(attr).where(spec).getSingleResult();
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Override
  public <K> Map<K, Long> countDistinct(String keyAttr, String valueAttr, Specification<T> spec) {
    Field keyField = fromEntity.getDeclaredField(keyAttr);
    Class<K> keyClass = (Class<K>) keyField.getType();

    return applyRepositoryMethodMetadata(
        builder(Tuple.class).countDistinct(valueAttr).groupBy(keyAttr).where(spec).createQuery()
    )
        .getResultList()
        .stream()
        .collect(Utils.toMap(x -> x.get(0, keyClass), x -> x.get(1, Long.class)));
  }

  @Override
  public Double avg(String attr, Specification<T> spec) {
    return builder(Tuple.class).avg(attr).where(spec).getSingleResult().get(0, Double.class);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Override
  public <K> Map<K, Double> avg(String keyAttr, String valueAttr, Specification<T> spec) {
    Field keyField = fromEntity.getDeclaredField(keyAttr);
    Class<K> keyClass = (Class<K>) keyField.getType();

    return applyRepositoryMethodMetadata(
        builder(Tuple.class).avg(valueAttr).groupBy(keyAttr).where(spec).createQuery()
    )
        .getResultList()
        .stream()
        .collect(Utils.toMap(x -> x.get(0, keyClass), x -> x.get(1, Double.class)));
  }

  @Override
  public <R> List<R> stat(Class<R> rClass) {
    return stat(rClass, null);
  }

  @Override
  public <R> List<R> stat(Class<R> rClass, Specification<T> spec) {
    return stat(rClass, spec, Sort.unsorted());
  }

  @Override
  public <R> Page<R> stat(Class<R> rClass, Specification<T> spec, Pageable pageable) {
    CriteriaQuery<R> query = createQuery(rClass, spec, pageable.getSort());
    TypedQuery<R> typedQuery = entityManager.createQuery(query);

    if (pageable.isPaged()) {
      typedQuery.setFirstResult((int) pageable.getOffset());
      typedQuery.setMaxResults(pageable.getPageSize());
    }

    List<R> list = typedQuery.getResultList();
    Long total = getTotal(query);

    return new PageImpl<>(list, pageable, total);
  }

  @Override
  public <R> List<R> stat(Class<R> rClass, Specification<T> spec, Sort sort) {
    CriteriaQuery<R> query = createQuery(rClass, spec, sort);
    TypedQuery<R> typedQuery = entityManager.createQuery(query);

    return typedQuery.getResultList();
  }

  private <R> CriteriaQuery<R> createQuery(Class<R> rClass, Specification<T> spec, Sort sort) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<R> query = builder.createQuery(rClass);
    Root<T> root = query.from(fromEntity);

    Field[] fields = rClass.getDeclaredFields();
    List<Expression<?>> metrics = getMetrics(fields, builder, root);
    List<Expression<?>> dims = getDims(fields, builder, root);

    ArrayList<Expression<?>> columns = new ArrayList<>();
    columns.addAll(dims);
    columns.addAll(metrics);

    query.select(builder.construct(rClass, columns.toArray(new Selection[0])));
    query.groupBy(dims);

    Optional.ofNullable(spec).map(s -> s.toPredicate(root, query, builder)).ifPresent(query::where);

    query.orderBy(getOrders(sort, builder, root, rClass));

    return query;
  }

  private <R> Long getTotal(CriteriaQuery<R> query) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> totalQuery = builder.createQuery(Long.class);
    totalQuery.getRoots().addAll(query.getRoots());

    List<Expression<?>> groupList = query.getGroupList();
    totalQuery
        .select(StatQueryUtils.multipleCountDistinct((CriteriaBuilderImpl) builder, groupList));

    if (Objects.nonNull(query.getRestriction())) {
      totalQuery.where(query.getRestriction());
    }

    return entityManager.createQuery(totalQuery).getSingleResult();
  }

  private <R> List<Order> getOrders(Sort sort, CriteriaBuilder builder, Root<T> root,
                                    Class<R> pojo) {
    if (sort.isUnsorted()) {
      return Collections.emptyList();
    }

    return sort.stream().map(order -> {
      try {
        Field field = pojo.getDeclaredField(order.getProperty());
        return new OrderImpl(getExpression(field, builder, root), order.isAscending());
      } catch (NoSuchFieldException e) {
        e.printStackTrace();
      }

      return null;
    })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private List<Expression<?>> getMetrics(Field[] fields, CriteriaBuilder builder, Root<T> root) {
    List<Expression<?>> metrics = new ArrayList<>();

    for (Field field : fields) {
      int modifiers = field.getModifiers();
      if (Modifier.isStatic(modifiers) || !Modifier.isPrivate(modifiers)) {
        continue; // 只将非静态且private属性作为聚合字段
      }

      Stat agg = AnnotationUtils.findAnnotation(field, Stat.class);
      // 被注解标注的字段作为聚合指标，其他作为聚合维度
      if (Objects.nonNull(agg)) {
        metrics.add(getExpression(field, builder, root));
      }
    }

    return metrics;
  }

  private List<Expression<?>> getDims(Field[] fields, CriteriaBuilder builder, Root<T> root) {
    List<Expression<?>> dims = new ArrayList<>();

    for (Field field : fields) {
      int modifiers = field.getModifiers();
      if (Modifier.isStatic(modifiers) || !Modifier.isPrivate(modifiers)) {
        continue; // 只将非静态且private属性作为聚合字段
      }

      Stat.Dim dim = AnnotationUtils.findAnnotation(field, Stat.Dim.class);
      // 被注解标注的字段作为聚合指标，其他作为聚合维度
      if (Objects.nonNull(dim)) {
        dims.add(getExpression(field, builder, root));
      }
    }

    return dims;
  }

  private Expression<?> getExpression(Field field, CriteriaBuilder builder, Root<T> root) {
    Stat stat = AnnotationUtils.findAnnotation(field, Stat.class);

    if (Objects.nonNull(stat)) {
      String attribute = StringUtils.hasText(stat.name()) ? stat.name() : field.getName();
      return stat.function().toExp(builder, root.get(attribute));
    } else {
      return root.get(field.getName());
    }
  }

  @Override
  public Map<ID, T> tuple(Collection<ID> ids) {
    return tuple((root, query, cb) -> {
      CriteriaBuilder.In<ID> idIn = cb.in(root.get(idName));
      ids.forEach(idIn::value);

      return query.where(idIn).getRestriction();
    });
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Override
  public Map<ID, T> tuple(Specification<T> spec) {
    Field idField = fromEntity.getDeclaredField(idName);
    idField.setAccessible(true);

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> query = builder.createQuery(fromEntity);
    Root<T> root = query.from(fromEntity);

    query.select(root);
    Optional.ofNullable(spec).map(s -> s.toPredicate(root, query, builder)).ifPresent(query::where);
    List<T> rows = entityManager.createQuery(query).getResultList();

    return rows
        .stream()
        .collect(Utils.<T, ID, T>toMap(
            row -> {
              try {
                Object o = idField.get(row);
                return (ID) o;
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
              return null;
            },
            row -> row
        ));
  }

  @SneakyThrows(NoSuchFieldException.class)
  @SuppressWarnings("unchecked")
  @Override
  public <K, V> Map<K, V> tuple(String keyAttr, String valueAttr, Specification<T> spec) {
    Field keyField = fromEntity.getDeclaredField(keyAttr);
    Class<K> keyClass = (Class<K>) keyField.getType();

    Field valueField = fromEntity.getDeclaredField(valueAttr);
    Class<V> valueClass = (Class<V>) valueField.getType();

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> query = builder.createQuery(Tuple.class);
    Root<T> root = query.from(fromEntity);

    query.multiselect(root.get(keyAttr), root.get(valueAttr));
    Optional.ofNullable(spec).map(s -> s.toPredicate(root, query, builder)).ifPresent(query::where);

    return entityManager.createQuery(query)
        .getResultList()
        .stream()
        .collect(Utils.toMap(row -> row.get(0, keyClass), row -> row.get(1, valueClass)));
  }

  @Override
  public Optional<T> findTop(Specification<T> spec, Sort sort) {
    return getQuery(spec, sort).setMaxResults(1).getResultStream().findFirst();
  }

  @Override
  public List<T> findTop(Specification<T> spec, Sort sort, Integer topN) {
    return getQuery(spec, sort).setMaxResults(topN).getResultList();
  }

  @Override
  public List<ID> pick() {
    return pick(null);
  }

  @Override
  public List<ID> pick(Specification<T> spec) {
    return pick(idClass, idName, spec);
  }

  @SneakyThrows(NoSuchFieldException.class)
  @Override
  @SuppressWarnings("unchecked")
  public <R> List<R> pick(String attr, Specification<T> spec) {
    Field field = fromEntity.getDeclaredField(attr);
    Class<R> rClass = (Class<R>) field.getType();

    return pick(rClass, attr, spec);
  }

  private <R> List<R> pick(Class<R> rClass, String attr, Specification<T> spec) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<R> query = builder.createQuery(rClass);
    Root<T> root = query.from(fromEntity);

    query.select(root.get(attr));
    Optional.ofNullable(spec).map(s -> s.toPredicate(root, query, builder)).ifPresent(query::where);

    return applyRepositoryMethodMetadata(entityManager.createQuery(query)).getResultList();
  }

  @SneakyThrows(NoSuchFieldException.class)
  @Override
  @SuppressWarnings("unchecked")
  public <R> List<R> pick(String attr, Example<T> example) {
    Field field = fromEntity.getDeclaredField(attr);
    Class<R> rClass = (Class<R>) field.getType();

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<R> query = builder.createQuery(rClass);
    Root<T> root = query.from(fromEntity);

    query.select(root.get(attr));
    query.where(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

    return applyRepositoryMethodMetadata(entityManager.createQuery(query)).getResultList();
  }

  @Override
  public Page<T> page(Pageable pageable) {
    return page(null, pageable);
  }

  @Override
  public Page<T> page(Specification<T> spec, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> query = builder.createQuery(fromEntity);
    Root<T> root = query.from(fromEntity);
    query.select(root);
    Optional.ofNullable(spec).map(s -> s.toPredicate(root, query, builder)).ifPresent(query::where);
    Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
    query.orderBy(Utils.toOrders(sort, root, builder));

    TypedQuery<T> typedQuery = applyRepositoryMethodMetadata(entityManager.createQuery(query));
    return pageable.isUnpaged()
        ? new PageImpl<>(typedQuery.getResultList())
        : readPage(typedQuery, fromEntity, pageable, spec);
  }
}
