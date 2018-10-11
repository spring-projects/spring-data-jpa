[![Spring Data JPA](https://spring.io/badges/spring-data-jpa/ga.svg)](http://projects.spring.io/spring-data-jpa/#quick-start)
[![Spring Data JPA](https://spring.io/badges/spring-data-jpa/snapshot.svg)](http://projects.spring.io/spring-data-jpa/#quick-start)
[![Build Status](https://travis-ci.org/spring-projects/spring-data-jpa.svg?branch=master)](https://travis-ci.org/spring-projects/spring-data-jpa) 


# Spring Data JPA 

The primary goal of the [Spring Data](http://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use data access technologies. This module deals with enhanced support for JPA based data access layers.

## Features ##

* Implementation of CRUD methods for JPA Entities
* Dynamic query generation from query method names
* Transparent triggering of JPA NamedQueries by query methods
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace

## Getting Help ##

This README as well as the [reference documentation](http://docs.spring.io/spring-data/data-jpa/docs/current/reference/html) are the best places to start learning about Spring Data JPA.  There are also [two sample applications](https://github.com/spring-projects/spring-data-examples) available to look at.

The main project [website](http://projects.spring.io/spring-data) contains links to basic project information such as source code, JavaDocs, Issue tracking, etc.

For more detailed questions, use [stackoverflow](http://stackoverflow.com/questions/tagged/spring-data-jpa). If you are new to Spring as well as to Spring Data, look for information about [Spring projects](http://projects.spring.io). You should also have a look at our new Spring Guides
[Accessing Data with JPA](http://spring.io/guides/gs/accessing-data-jpa/) that leverages the simplified configuration provided by [Spring Boot](http://projects.spring.io/spring-boot/) as well as [Accessing JPA Data with REST](http://spring.io/guides/gs/accessing-data-rest/)..


## Quick Start ##

Download the jar through Maven:

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-jpa</artifactId>
  <version>${version}.RELEASE</version>
</dependency>
```
If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version.

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-jpa</artifactId>
  <version>${version}.BUILD-SNAPSHOT</version>
</dependency>

<repository>
  <id>spring-libs-snapshot</id>
  <name>Spring Snapshot Repository</name>
  <url>http://repo.spring.io/libs-snapshot</url>
</repository>
```

Also include your JPA persistence provider of choice (Hibernate, EclipseLink, OpenJpa). Setup basic Spring JPA configuration as well as Spring Data JPA repository support.

The simple Spring Data JPA configuration with Java-Config looks like this: 
```java
@Configuration
@EnableJpaRepositories("com.acme.repositories")
class AppConfig {

  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
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
    dave = repository.save(dave);
         
    User carter = new User("Carter", "Beauford");
    carter = repository.save(carter);
         
    List<User> result = repository.findByLastname("Matthews");
    assertThat(result.size(), is(1));
    assertThat(result, hasItem(dave));
  }
}
```

## Contributing to Spring Data JPA

Here are some ways for you to get involved in the community:

* Get involved with the Spring community by helping out on [stackoverflow](http://stackoverflow.com/questions/tagged/spring-data-jpa) by responding to questions and joining the debate.
* Create [JIRA](https://jira.spring.io/browse/DATAJPA) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://spring.io/blog) to spring.io.

Before we accept a non-trivial patch or pull request we will need you to [sign the Contributor License Agreement](https://cla.pivotal.io/sign/spring). Signing the contributor’s agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. If you forget to do so, you'll be reminded when you submit a pull request. Active contributors might be asked to join the core team, and given the ability to merge pull requests.
