package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Product {

	@Id @GeneratedValue private Long id;

	public Long getId() {
		return id;
	}
}
