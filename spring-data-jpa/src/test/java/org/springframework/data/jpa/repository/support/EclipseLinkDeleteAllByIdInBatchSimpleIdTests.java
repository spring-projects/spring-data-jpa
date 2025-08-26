/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:infrastructure.xml", "classpath:eclipselink-h2.xml"})
@Transactional
class EclipseLinkDeleteAllByIdInBatchSimpleIdTests {

    @PersistenceContext
    EntityManager em;

    private JpaRepository<User, Integer> users;

    @BeforeEach
    void setUp() {
        users = new JpaRepositoryFactory(em).getRepository(UserRepository.class);
    }

    @Test // GH-3990
    void deleteAllByIdInBatchShouldWorkOnEclipseLinkWithSimpleId() {
        User one = new User("one", "eins", "one@test.com");
        User two = new User("two", "zwei", "two@test.com");
        User three = new User("three", "drei", "three@test.com");

        users.saveAll(List.of(one, two, three));
        users.flush();

        users.deleteAllByIdInBatch(List.of(one.getId(), three.getId()));

        assertThat(users.findAll()).containsExactly(two);
    }

    private interface UserRepository extends JpaRepository<User, Integer> {
    }
}
