package io.github.fxylee.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

//CHECKSTYLE:OFF
@NoRepositoryBean
public interface StatRepository<T, ID> {
  <R> QueryBuilder<T, R> builder(Class<R> rClass);

  <V> V min(String attr, Specification<T> spec);

  /**
   * 按 keyAttr group by 后，min valueAttr
   */
  <K, V> Map<K, V> min(String keyAttr, String valueAttr, Specification<T> spec);

  <V> V max(String attr, Specification<T> spec);

  /**
   * 按 keyAttr group by 后，max valueAttr
   */
  <K, V> Map<K, V> max(String keyAttr, String valueAttr, Specification<T> spec);

  <V> V sum(String attr, Specification<T> spec);

  /**
   * 按 keyAttr group by 后，sum valueAttr
   */
  <K, V> Map<K, V> sum(String keyAttr, String valueAttr, Specification<T> spec);

  Long count(String attr, Specification<T> spec);

  /**
   * 按 keyAttr group by 后，count valueAttr
   */
  <K> Map<K, Long> count(String keyAttr, String valueAttr, Specification<T> spec);

  Long countDistinct(String attr, Specification<T> spec);

  /**
   * 按 keyAttr group by 后，count distinct valueAttr
   */
  <K> Map<K, Long> countDistinct(String keyAttr, String valueAttr, Specification<T> spec);

  Double avg(String attr, Specification<T> spec);

  /**
   * 按 keyAttr group by 后，count distinct valueAttr
   */
  <K> Map<K, Double> avg(String keyAttr, String valueAttr, Specification<T> spec);

  <R> List<R> stat(Class<R> rClass);

  <R> List<R> stat(Class<R> rClass, Specification<T> spec);

  <R> List<R> stat(Class<R> rClass, Specification<T> spec, Sort sort);

  <R> Page<R> stat(Class<R> rClass, Specification<T> spec, Pageable pageable);

  Map<ID, T> tuple();

  Map<ID, T> tuple(Collection<ID> ids);

  Map<ID, T> tuple(Specification<T> spec);

  /**
   * 查询keyAttr 和 valueAttr 两个字段，并自动包装为Map结构
   */
  <K, V> Map<K, V> tuple(String keyAttr, String valueAttr, Specification<T> spec);

  /**
   * 查询按指定排序后，第一条数据
   */
  Optional<T> findTop(Specification<T> spec, Sort sort);

  /**
   * 查询按指定排序后，前面 N 条数据
   */
  List<T> findTop(Specification<T> spec, Sort sort, Integer topN);

  /**
   * 查询所有ID字段
   */
  List<ID> pick();

  /**
   * 查询ID字段
   */
  List<ID> pick(Specification<T> spec);

  /**
   * 查询单个字段
   */
  <R> List<R> pick(String attr, Specification<T> spec);

  /**
   * 查询单个字段
   */
  <R> List<R> pick(String attr, Example<T> example);

  Page<T> page(Pageable pageable);

  Page<T> page(@Nullable Specification<T> spec, Pageable pageable);
}
