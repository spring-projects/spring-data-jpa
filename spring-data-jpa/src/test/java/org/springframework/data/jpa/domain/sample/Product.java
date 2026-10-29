package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Product {

	@Id @GeneratedValue private Long id;

	@Column(unique = true, nullable = false)
	private String code;

	@Column(unique = true)
	private String secondaryCode;

	public Long getId() {
		return id;
	}
}
