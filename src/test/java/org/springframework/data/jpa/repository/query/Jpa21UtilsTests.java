/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;
import static org.springframework.data.jpa.util.IsAttributeNode.*;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class Jpa21UtilsTests {

	@Autowired EntityManager em;

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldCreateGraphWithoutSubGraphCorrectly() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(
				new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "roles", "colleagues" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles, terminatesGraph());

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues, terminatesGraph());
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldCreateGraphWithMultipleSubGraphCorrectly() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH,
				new String[] { "roles", "colleagues.roles", "colleagues.colleagues" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles, terminatesGraph());

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues, terminatesGraphWith("roles", "colleagues"));
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldCreateGraphWithDeepSubGraphCorrectly() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH,
				new String[] { "roles", "colleagues.roles", "colleagues.colleagues.roles" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles, terminatesGraph());

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues, terminatesGraphWith("roles"));
		assertThat(colleagues, hasSubgraphs("colleagues"));

		AttributeNode<?> colleaguesOfColleagues = findNode("colleagues", colleagues);
		assertThat(colleaguesOfColleagues, terminatesGraphWith("roles"));
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	public void shouldIgnoreIntermedeateSubGraphNodesThatAreNotNeeded() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "roles",
				"colleagues", "colleagues.roles", "colleagues.colleagues", "colleagues.colleagues.roles" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles, terminatesGraph());

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues, terminatesGraphWith("roles"));
		assertThat(colleagues, hasSubgraphs("colleagues"));

		AttributeNode<?> colleaguesOfColleagues = findNode("colleagues", colleagues);
		assertThat(colleaguesOfColleagues, terminatesGraphWith("roles"));
	}

	@Test // DATAJPA-1041, DATAJPA-1075
	public void orderOfSubGraphsShouldNotMatter() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		EntityGraph<User> graph = em.createEntityGraph(User.class);
		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] {
				"colleagues.colleagues.roles", "roles", "colleagues.colleagues", "colleagues", "colleagues.roles" }), graph);

		AttributeNode<?> roles = findNode("roles", graph);
		assertThat(roles, terminatesGraph());

		AttributeNode<?> colleagues = findNode("colleagues", graph);
		assertThat(colleagues, terminatesGraphWith("roles"));
		assertThat(colleagues, hasSubgraphs("colleagues"));

		AttributeNode<?> colleaguesOfColleagues = findNode("colleagues", colleagues);
		assertThat(colleaguesOfColleagues, terminatesGraphWith("roles"));
	}

	@Test(expected = Exception.class) // DATAJPA-1041, DATAJPA-1075
	public void errorsOnUnknownProperties() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		Jpa21Utils.configureFetchGraphFrom(new JpaEntityGraph("name", EntityGraphType.FETCH, new String[] { "¯\\_(ツ)_/¯" }),
				em.createEntityGraph(User.class));
	}
}
