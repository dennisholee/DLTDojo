package io.forest.models;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WalletModel<T> extends RepresentationModel<WalletModel<T>> {

	@NotNull
	T address;

	@Size(max = 20)
	String alias;

	public WalletModel(T address) {
		this.address = address;
	}

	public WalletModel(T address, String alias) {
		this.address = address;
		this.alias = alias;
	}

	public T getAddress() {
		return this.address;
	}

	public String getAlias() {
		return this.alias;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();

		// TODO: Handle exception.
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
