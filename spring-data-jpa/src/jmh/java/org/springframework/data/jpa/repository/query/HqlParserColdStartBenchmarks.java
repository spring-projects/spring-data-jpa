/*
 * Copyright 2026-present the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.junit.platform.commons.annotation.Testable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author Arai
 */
@Testable
@Fork(10)
@Warmup(iterations = 0)
@Measurement(iterations = 1, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Timeout(time = 5)
public class HqlParserColdStartBenchmarks {

	private static final String QUERY = """
			select new com.example.InvoiceSummary(
				i.id,
				coalesce(sum(case when line.kind = 'SALE' then line.amount else 0 end), 0),
				coalesce(sum(case when line.kind = 'REFUND' then line.amount else 0 end), 0),
				case when exists (select 1 from Payment payment where payment.invoice = i and payment.state = 'PAID')
					then true else false end,
				lower(coalesce(customer.name, customer.companyName, '')),
				function('jsonb_extract_path_text', i.metadata, 'source'))
			from Invoice i
			join i.customer customer
			left join i.lines line
			where (:tenantId is null or i.tenantId = :tenantId)
				and (:search is null
					or lower(coalesce(i.number, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(customer.name, customer.companyName, '')) like lower(concat('%', :search, '%'))
					or lower(function('jsonb_extract_path_text', i.metadata, 'reference')) like lower(concat('%', :search, '%')))
				and (:states is null or i.state in :states)
				and i.createdAt between :from and :to
				and not exists (select 1 from Cancellation cancellation where cancellation.invoice = i)
			group by i.id, customer.name, customer.companyName, i.metadata
			having coalesce(sum(case when line.kind = 'SALE' then line.amount else 0 end), 0) >= :minimum
			order by lower(coalesce(customer.name, customer.companyName, '')), i.id
			""";

	@Benchmark
	public Object parse() {
		return JpaQueryEnhancer.forHql(QUERY);
	}
}
