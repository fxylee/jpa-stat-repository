# jpa-stat-repository

Make it easier to build aggregated queries through JPA.

## Usage

Step 1.

```java
import io.github.fxylee.jpa.StatRepositoryImpl;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(repositoryBaseClass = StatRepositoryImpl.class)
public class SpringBootApplication {
}
```

Step 2.

```java
public interface EntityRepository  extends StatRepository<Entity, ID> {
}
```

Step 3.
```java
import java.util.Map;
import java.util.List;
import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

@Service
public class BizService {
  @Resource
  private EntityRepository entityRepository;

  /**
   * Note: 
   * keyPath is the property of entity
   */
  public List<String> getAll() {
    return entityRepository.pick("keyPath", getSpec());
  }
  
  public Map<String, String> getCodeNameMap() {
    return entityRepository.tuple("codeKeyPath", "nameKeyPath", getSpec());
  }

  public Long getTotal() {
    return entityRepository.sum("keyPath", getSpec());
  }

  public Long getMaxId() {
    return entityRepository.max("idPath", getSpec());
  }

  private <T> Specification<T> getSpec() {
    return (root, query, cb) -> {
      List<Predicate> filters = new ArrayList<>();
      // some criteria query

      return query.where(filters.toArray(new Predicate[0])).getRestriction();
    };
  }
}
```