# Spring Data JPA #

The primary goal of the [Spring Data](http://www.springsource.org/spring-data) project is to make it easier to build Spring-powered applications that use data access technologies. This module deals with enhanced support for JPA based data access layers.

## Features ##

* Implementation of CRUD methods for JPA Entities
* Dynamic query generation from query method names
* Transparent triggering of JPA NamedQueries by query methods
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace

## Getting Help ##

This README as well as the [reference documentation](http://static.springsource.org/spring-data/data-jpa/snapshot-site/reference/html) are the best places to start learning about Spring Data JPA.  There are also [two sample applications](https://github.com/SpringSource/spring-data-jpa-examples) available to look at.

The main project [website](http://www.springsource.org/spring-data) contains links to basic project information such as source code, JavaDocs, Issue tracking, etc.

For more detailed questions, use the [forum](http://forum.springsource.org/forumdisplay.php?f=27). If you are new to Spring as well as to Spring Data, look for information about [Spring projects](http://www.springsource.org/projects). You should also have a look at our new Spring Guide
[Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/) that leverages the simplified configuration provided by [Spring Boot](http://projects.spring.io/spring-boot/).


## Quick Start ##

Download the jar though Maven:

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-jpa</artifactId>
  <version>1.4.1.RELEASE</version>
</dependency>
```

Also include your JPA persistence provider of choice (Hibernate, EclipseLink, OpenJpa). Setup basic Spring JPA configuration as well as Spring Data JPA repository support.

The simple Spring Data JPA configuration with Java-Config looks like this: 
```java
@Configuration
@EnableJpaRepositories("com.acme.repositories")
class AppConfig {

  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
  }

  @Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }

  @Bean
  public JpaVendorAdapter jpaVendorAdapter() {
    HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
    jpaVendorAdapter.setDatabase(Database.H2);
    jpaVendorAdapter.setGenerateDdl(true);
    return jpaVendorAdapter;
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean lemfb = new LocalContainerEntityManagerFactoryBean();
    lemfb.setDataSource(dataSource());
    lemfb.setJpaVendorAdapter(jpaVendorAdapter());
    lemfb.setPackagesToScan("com.acme");
    return lemfb;
  }
}
```

Create an entity:

```java
@Entity
public class User {

  @Id
  @GeneratedValue
  private Integer id;
  private String firstname;
  private String lastname;
       
  // Getters and setters
  // (Firstname, Lastname)-constructor and noargs-constructor
  // equals / hashcode
}
```

Create a repository interface in `com.acme.repositories`:

```java
public interface UserRepository extends CrudRepository<User, Long> {
  List<User> findByLastname(String lastname);
}
```

Write a test client

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UserRepositoryIntegrationTest {
     
  @Autowired UserRepository repository;
     
  @Test
  public void sampleTestCase() {
    User dave = new User("Dave", "Matthews");
    dave = repository.save(user);
         
    User carter = new User("Carter", "Beauford");
    carter = repository.save(carter);
         
    List<User> result = repository.findByLastname("Matthews");
    assertThat(result.size(), is(1));
    assertThat(result, hasItem(dave));
  }
}
```

## Contributing to Spring Data JPA##

Here are some ways for you to get involved in the community:

* Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.springsource.org/forumdisplay.php?f=27) by responding to questions and joining the debate.
* Create [JIRA](https://jira.springsource.org/browse/DATAJPA) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://www.springsource.org/node/feed) to springframework.org

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests.
