package io.forest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "action" })
public class WalletActionModel {

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public static enum WalletAction {
		topup
	};

	WalletAction action;

	@JsonCreator
	public WalletActionModel(@JsonProperty("action") WalletAction action) {
		this.action = action;
	}

	public WalletAction getAction() {
		return this.action;
	}
}
