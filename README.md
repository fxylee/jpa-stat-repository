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
import java.util.List;
import javax.annotation.Resource;

@Ser
public class BizService {
  @Resource
  private EntityRepository entityRepository;

  public List<String> getAll() {
    entityRepository.pick(
      "keyPath", // the property of entity
      (root, query, cb) -> {
        List<Predicate> filters = new ArrayList<>();
        // some criteria query

        return query.where(filters.toArray(new Predicate[0])).getRestriction();
      }
    );
  }
}
```