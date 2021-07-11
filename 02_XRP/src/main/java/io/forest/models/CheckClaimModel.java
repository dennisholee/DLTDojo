package io.forest.models;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonPropertyOrder({ "account", "amount", "checkId" })
public class CheckClaimModel {

	@NotBlank
	private String account;

	@NotBlank
	private String amount;

	@NotBlank
	private String checkId;

	@JsonCreator
	public CheckClaimModel(@JsonProperty("account") String account, @JsonProperty("amount") String amount,
			@JsonProperty("checkId") String checkId) {
		super();
		this.account = account;
		this.amount = amount;
		this.checkId = checkId;
	}

	public String getAccount() {
		return account;
	}

	public String getAmount() {
		return amount;
	}

	public String getCheckId() {
		return checkId;
	}

	@Override
	public String toString() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			// TODO: Handle exception during toString operation.
			return "";
		}
	}
}
