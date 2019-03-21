/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jpa.support;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import org.springframework.data.repository.Repository;

/**
 * Adapter that allows usage of blocking {@link Repository repositories} to be integrated in a reactive and non-blocking
 * flow. Some modules rely solely on blocking drivers and assume an imperative programming model. This adapter offloads
 * its workload to an underlying {@link reactor.core.scheduler.Scheduler} to prevent blocking of the subscribing
 * {@link Thread}.
 * <p/>
 * <strong>This {@link BlockingRepositoryAdapter} moves blocking behavior off the subscribing {@link Thread} to a
 * dedicated {@link reactor.core.scheduler.Scheduler}. It does not solve the blocking aspect itself, it moves work to a
 * place where it hurts less.</strong>
 *
 * @author Mark Paluch
 * @since 2.1
 * @see BlockingResourceAdapter
 */
public interface BlockingRepositoryAdapter {

	/**
	 * Read one or more entities using a {@link Repository}. The returned value is emitted upon completion through the
	 * returned {@link Mono} which also allows multiple subscriptions to run the {@link Function} multiple times.
	 * {@literal null} values returned by the {@link Function} are translated to {@link Mono#empty()}.
	 *
	 * @param function must not be {@literal null}.
	 * @param <R> Repository type.
	 * @param <T> return type.
	 * @return a {@link Mono} that wraps execution of the given {@link Function}. Each subscription initiates a new
	 *         execution of the function.
	 */
	<R extends Repository<?, ?>, T> Mono<T> read(R repository, Function<R, T> function);

	/**
	 * Read zero, one or more entities using a {@link Repository}. The returned value is emitted upon completion through
	 * the returned {@link Flux} which also allows multiple subscriptions to run the {@link Function} multiple times.
	 * {@literal null} values and empty {@link Iterable} returned by the {@link Function} are translated to
	 * {@link Flux#empty()}.
	 *
	 * @param function must not be {@literal null}.
	 * @param <R> Repository type.
	 * @param <T> return type.
	 * @return a {@link Flux} that wraps execution of the given {@link Function} by emitting all elements returned by the
	 *         function. Each subscription initiates a new execution of the function.
	 */
	default <R extends Repository<?, ?>, T> Flux<T> readMany(R repository, Function<R, ? extends Iterable<T>> function) {
		return read(repository, function).flatMapIterable(it -> it);
	}
}
