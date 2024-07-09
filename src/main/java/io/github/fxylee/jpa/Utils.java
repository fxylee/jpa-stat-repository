package io.github.fxylee.jpa;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class Utils {
  public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper) {
    return Collector.of(
        HashMap::new,
        (hashMap, t) -> hashMap.put(keyMapper.apply(t), valueMapper.apply(t)),
        (m1, m2) -> {
          m1.putAll(m2);
          return m1;
        }
    );
  }

  public static <T> PageImpl<T> toPage(Collection<T> collection, Class<T> clazz,
                                       Pageable pageable) {
    List<T> subList = collection.stream()
        .sorted(getComparator(clazz, pageable.getSort()))
        .skip(pageable.getOffset())
        .limit(pageable.getPageSize())
        .collect(Collectors.toList());

    return new PageImpl<T>(subList, pageable, collection.size());
  }

  /**
   * DTO对象按指定属性排序（除非明确指定为nulls first，否则默认为nulls last）
   */
  public static <T> Comparator<T> getComparator(Class<T> clazz, Sort sort) {
    return (Comparator<T> & Serializable) (c1, c2) -> {
      for (Sort.Order order : sort) {
        try {
          Field field = clazz.getDeclaredField(order.getProperty());
          field.setAccessible(true);

          Object a = field.get(c1);
          Object b = field.get(c2);

          if (Objects.isNull(a) && Objects.isNull(b)) {
            continue;
          }

          if (Objects.isNull(a)) {
            return Sort.NullHandling.NULLS_FIRST.equals(order.getNullHandling()) ? -1 : 1;
          }

          if (Objects.isNull(b)) {
            return Sort.NullHandling.NULLS_FIRST.equals(order.getNullHandling()) ? 1 : -1;
          }

          if ((a instanceof Comparable) && (b instanceof Comparable)) {
            int i = ((Comparable) a).compareTo(b);

            if (i == 0) {
              continue;
            }

            return order.isDescending() ? -i : i;
          }
        } catch (NoSuchFieldException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }

      return 0;
    };
  }
}
