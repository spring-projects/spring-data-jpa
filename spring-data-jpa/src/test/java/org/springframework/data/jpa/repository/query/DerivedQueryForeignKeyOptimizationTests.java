/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for derived query foreign key optimization.
 * Tests that queries like findByAuthorId don't generate unnecessary JOINs.
 *
 * @author Hyunjoon Kim
 * @see <a href="https://github.com/spring-projects/spring-data-jpa/issues/3349">Issue 3349</a>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Transactional
class DerivedQueryForeignKeyOptimizationTests {

	@Autowired BookRepository bookRepository;
	@Autowired AuthorRepository authorRepository;

	private Author savedAuthor;

	@BeforeEach
	void setUp() {
		Author author = new Author();
		author.setName("John Doe");
		savedAuthor = authorRepository.save(author);

		Book book1 = new Book();
		book1.setTitle("Spring in Action");
		book1.setAuthor(savedAuthor);
		bookRepository.save(book1);

		Book book2 = new Book();
		book2.setTitle("Spring Boot in Practice");
		book2.setAuthor(savedAuthor);
		bookRepository.save(book2);
	}

	@Test
	void findByAssociationId_shouldNotGenerateJoin() {
		// This test verifies that findByAuthorId doesn't create unnecessary JOIN
		List<Book> books = bookRepository.findByAuthorId(savedAuthor.getId());
		
		assertThat(books).hasSize(2);
		assertThat(books).extracting(Book::getTitle)
				.containsExactlyInAnyOrder("Spring in Action", "Spring Boot in Practice");
	}

	@Test
	void findByAssociationIdIn_shouldNotGenerateJoin() {
		// Test with IN clause
		List<Book> books = bookRepository.findByAuthorIdIn(List.of(savedAuthor.getId()));
		
		assertThat(books).hasSize(2);
	}

	@Test
	void countByAssociationId_shouldNotGenerateJoin() {
		// Test count queries
		long count = bookRepository.countByAuthorId(savedAuthor.getId());
		
		assertThat(count).isEqualTo(2);
	}

	@Test
	void existsByAssociationId_shouldNotGenerateJoin() {
		// Test exists queries
		boolean exists = bookRepository.existsByAuthorId(savedAuthor.getId());
		
		assertThat(exists).isTrue();
	}

	@Test
	void deleteByAssociationId_shouldNotGenerateJoin() {
		// Test delete queries
		long deletedCount = bookRepository.deleteByAuthorId(savedAuthor.getId());
		
		assertThat(deletedCount).isEqualTo(2);
		assertThat(bookRepository.count()).isZero();
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories = true)
	@ImportResource("classpath:infrastructure.xml")
	static class Config {
	}

	@Entity
	@Table(name = "test_author")
	static class Author {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "test_book")
	static class Book {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		private Author author;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	interface BookRepository extends JpaRepository<Book, Long> {
		List<Book> findByAuthorId(Long authorId);
		List<Book> findByAuthorIdIn(List<Long> authorIds);
		long countByAuthorId(Long authorId);
		boolean existsByAuthorId(Long authorId);
		long deleteByAuthorId(Long authorId);
	}

	interface AuthorRepository extends JpaRepository<Author, Long> {
	}
}