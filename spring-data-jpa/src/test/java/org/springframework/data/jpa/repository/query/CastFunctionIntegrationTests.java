/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.sample.Item;
import org.springframework.data.jpa.repository.sample.ItemRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Integration test specifically for GH-3863, testing the CAST function syntax in JPA queries.
 * 
 * @author Harikrishna Rajagopal
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SampleConfig.class})
@Transactional
public class CastFunctionIntegrationTests {
    
    @Autowired
    EntityManagerFactory emf;
    
    @Test // GH-3863
    void testCastFunctionWithAsKeyword() {
        
        EntityManager em = emf.createEntityManager();
        
        // Create a query using the CAST function with AS keyword
        // Using a simpler query with a valid type conversion
        em.createQuery("SELECT i FROM Item i WHERE CAST(i.id AS string) = '1'")
            .getResultList();
        
        // Another valid example
        em.createQuery("SELECT CAST('100' AS int) FROM Item i")
            .getResultList();
        
        // If we reach here without exceptions, the test passes
        assertThat(true).isTrue();
    }
} 