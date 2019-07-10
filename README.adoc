image:https://spring.io/badges/spring-data-jpa/ga.svg[Spring Data JPA,link=https://projects.spring.io/spring-data-jpa/#quick-start]
image:https://spring.io/badges/spring-data-jpa/snapshot.svg[Spring Data JPA,link=https://projects.spring.io/spring-data-jpa/#quick-start]

= Spring Data JPA image:https://jenkins.spring.io/buildStatus/icon?job=spring-data-jpa%2Fmaster&subject=Build[link=https://jenkins.spring.io/view/SpringData/job/spring-data-jpa/] https://gitter.im/spring-projects/spring-data[image:https://badges.gitter.im/spring-projects/spring-data.svg[Gitter]]

Spring Data JPA, part of the larger https://projects.spring.io/spring-data[Spring Data] family, makes it easy to easily implement JPA based repositories.
This module deals with enhanced support for JPA based data access layers.
It makes it easier to build Spring-powered applications that use data access technologies.

Implementing a data access layer of an application has been cumbersome for quite a while.
Too much boilerplate code has to be written to execute simple queries as well as perform pagination, and auditing.
Spring Data JPA aims to significantly improve the implementation of data access layers by reducing the effort to the amount that’s actually needed.
As a developer you write your repository interfaces, including custom finder methods, and Spring will provide the implementation automatically.

== Features

* Implementation of CRUD methods for JPA Entities
* Dynamic query generation from query method names
* Transparent triggering of JPA NamedQueries by query methods
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace

== Code of Conduct

This project is governed by the link:CODE_OF_CONDUCT.adoc[Spring Code of Conduct]. By participating, you are expected to uphold this code of conduct. Please report unacceptable behavior to spring-code-of-conduct@pivotal.io.

== Getting Started

Here is a quick teaser of an application using Spring Data Repositories in Java:

[source,java]
----
public interface PersonRepository extends CrudRepository<Person, Long> {

  List<Person> findByLastname(String lastname);

  List<Person> findByFirstnameLike(String firstname);
}

@Service
public class MyService {

  private final PersonRepository repository;

  public MyService(PersonRepository repository) {
    this.repository = repository;
  }

  public void doWork() {

    repository.deleteAll();

    Person person = new Person();
    person.setFirstname("Oliver");
    person.setLastname("Gierke");
    repository.save(person);

    List<Person> lastNameResults = repository.findByLastname("Gierke");
    List<Person> firstNameResults = repository.findByFirstnameLike("Oli*");
 }
}

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
----

=== Maven configuration

Add the Maven dependency:

[source,xml]
----
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-jpa</artifactId>
  <version>${version}.RELEASE</version>
</dependency>
----

If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version.

[source,xml]
----
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-jpa</artifactId>
  <version>${version}.BUILD-SNAPSHOT</version>
</dependency>

<repository>
  <id>spring-libs-snapshot</id>
  <name>Spring Snapshot Repository</name>
  <url>https://repo.spring.io/libs-snapshot</url>
</repository>
----

== Getting Help

Having trouble with Spring Data? We’d love to help!

* Check the
https://docs.spring.io/spring-data/jpa/docs/current/reference/html/[reference documentation], and https://docs.spring.io/spring-data/jpa/docs/current/api/[Javadocs].
* Learn the Spring basics – Spring Data builds on Spring Framework, check the https://spring.io[spring.io] web-site for a wealth of reference documentation.
If you are just starting out with Spring, try one of the https://spring.io/guides[guides].
* If you are upgrading, check out the https://docs.spring.io/spring-data/jpa/docs/current/changelog.txt[changelog] for "`new and noteworthy`" features.
* Ask a question - we monitor https://stackoverflow.com[stackoverflow.com] for questions tagged with https://stackoverflow.com/tags/spring-data[`spring-data-jpa`].
You can also chat with the community on https://gitter.im/spring-projects/spring-data[Gitter].
* Report bugs with Spring Data JPA at https://jira.spring.io/browse/DATAJPA[jira.spring.io/browse/DATAJPA].

== Reporting Issues

Spring Data uses JIRA as issue tracking system to record bugs and feature requests. If you want to raise an issue, please follow the recommendations below:

* Before you log a bug, please search the
https://jira.spring.io/browse/DATAJPA[issue tracker] to see if someone has already reported the problem.
* If the issue doesn’t already exist, https://jira.spring.io/browse/DATAJPA[create a new issue].
* Please provide as much information as possible with the issue report, we like to know the version of Spring Data that you are using and JVM version.
* If you need to paste code, or include a stack trace use JIRA `{code}…{code}` escapes before and after your text.
* If possible try to create a test-case or project that replicates the issue. Attach a link to your code or a compressed file containing your code.

== Building from Source

You don’t need to build from source to use Spring Data (binaries in https://repo.spring.io[repo.spring.io]), but if you want to try out the latest and greatest, Spring Data can be easily built with the https://github.com/takari/maven-wrapper[maven wrapper].
You also need JDK 1.8.

[source,bash]
----
 $ ./mvnw clean install
----

If you want to build with the regular `mvn` command, you will need https://maven.apache.org/run-maven/index.html[Maven v3.5.0 or above].

_Also see link:CONTRIBUTING.adoc[CONTRIBUTING.adoc] if you wish to submit pull requests, and in particular please sign the https://cla.pivotal.io/sign/spring[Contributor’s Agreement] before your first non-trivial change._

=== Building reference documentation

Building the documentation builds also the project without running tests.

[source,bash]
----
 $ ./mvnw clean install -Pdistribute
----

The generated documentation is available from `target/site/reference/html/index.html`.

== Guides

The https://spring.io/[spring.io] site contains several guides that show how to use Spring Data step-by-step:

* https://spring.io/guides/gs/accessing-data-jpa/[Accessing Data with JPA]: Learn how to work with JPA data persistence using Spring Data JPA.
* https://spring.io/guides/gs/accessing-jpa-data-rest/[Accessing JPA Data with REST] is a guide to creating a REST web service exposing data stored with JPA through repositories.

== Examples

* https://github.com/spring-projects/spring-data-examples/[Spring Data Examples] contains example projects that explain specific features in more detail.

== License

Spring Data JPA is Open Source software released under the https://www.apache.org/licenses/LICENSE-2.0.html[Apache 2.0 license].
