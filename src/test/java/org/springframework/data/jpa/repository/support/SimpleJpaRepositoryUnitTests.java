/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.sample.User;

/**
 * Unit tests for {@link SimpleJpaRepository}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleJpaRepositoryUnitTests {

	SimpleJpaRepository<User, Long> repo;

	@Mock
	EntityManager em;
	@Mock
	CriteriaBuilder builder;
	@Mock
	CriteriaQuery<User> criteriaQuery;
	@Mock
	CriteriaQuery<Long> countCriteriaQuery;
	@Mock
	TypedQuery<User> query;
	@Mock
	TypedQuery<Long> countQuery;
	@Mock
	JpaEntityInformation<User, Long> information;

	@Before
	public void setUp() {

		when(information.getJavaType()).thenReturn(User.class);
		when(em.getCriteriaBuilder()).thenReturn(builder);

		when(builder.createQuery(User.class)).thenReturn(criteriaQuery);
		when(builder.createQuery(Long.class)).thenReturn(countCriteriaQuery);

		when(em.createQuery(criteriaQuery)).thenReturn(query);
		when(em.createQuery(countCriteriaQuery)).thenReturn(countQuery);

		repo = new SimpleJpaRepository<User, Long>(information, em);
	}

	/**
	 * @see DATAJPA-124
	 */
	@Test
	public void doesNotActuallyRetrieveObjectsForPageableOutOfRange() {

		when(countQuery.getSingleResult()).thenReturn(20L);
		repo.findAll(new PageRequest(2, 10));

		verify(query, times(0)).getResultList();
	}

	/**
	 * @see DATAJPA-177
	 */
	@Test(expected = EmptyResultDataAccessException.class)
	public void throwsExceptionIfEntityToDeleteDoesNotExist() {

		repo.delete(4711L);
	}
	
	/**
	 * @see DATAJPA-412
	 */
	@Test
	public void callsEntityManagerRefreshOnEntity() {
		User userInfo = new User();
		userInfo.setId(412);
		
		repo.refresh(userInfo);
		verify(em).refresh(userInfo);
	}

    /**
     * @see DATAJPA-412
     */
    @Test
    public void callsEntityManagerRefreshOnEntityWithLockedType() {
        User userInfo = new User();
        userInfo.setId(412);
        LockMetadataProvider optimisticLock = new LockMetadataProvider() {
            @Override
            public LockModeType getLockModeType() {
                return LockModeType.OPTIMISTIC;
            }
        };
        repo.setLockMetadataProvider(optimisticLock);
        repo.refresh(userInfo);
        verify(em).refresh(userInfo, LockModeType.OPTIMISTIC);
    }
}
