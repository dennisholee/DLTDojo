package io.forest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonPropertyOrder({ "TransactionType", "Account", "Amount", "Destination" })
public class PaymentModel {

	private String transactionType;

	private String account;

	private String amount;

	private String destination;

	@JsonCreator
	public PaymentModel(@JsonProperty("transactionType") String transactionType,
			@JsonProperty("account") String account, @JsonProperty("amount") String amount,
			@JsonProperty("destination") String destination) {
		super();
		this.transactionType = transactionType;
		this.account = account;
		this.amount = amount;
		this.destination = destination;
	}

	@JsonGetter("TransactionType")
	public String getTransactionType() {
		return transactionType;
	}

	@JsonGetter("Account")
	public String getAccount() {
		return account;
	}

	@JsonGetter("Amount")
	public String getAmount() {
		return amount;
	}

	@JsonGetter("Destination")
	public String getDestination() {
		return destination;
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
