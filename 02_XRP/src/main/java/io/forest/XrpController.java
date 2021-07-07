package io.forest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetAccountResponse;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.ImmutablePayment;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.Transaction;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;
import org.xrpl.xrpl4j.wallet.Wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

@RestController
public class XrpController {

	Logger log = LoggerFactory.getLogger(XrpController.class);

	@Autowired
	Wallet wallet;

	@Autowired
	Wallet beneWallet;

	@Autowired
	XrplClient xrplClient;

	@Autowired
	FaucetClient faucetClient;

	@GetMapping(path = "/wallets")
	@ResponseStatus(HttpStatus.OK)
	public List<Address> getWallet() throws JsonProcessingException {
		List<Address> wallets = Arrays.asList(wallet.classicAddress(), beneWallet.classicAddress());
		return wallets;
	}

	@PostMapping(path = "/wallets/{address}", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> postWallet(@PathVariable("address") String address, @RequestBody JsonNode node) {
		log.debug("Address> {}, RequestPayload> {}", address, node.toPrettyString());

		String action = node.at("/action").asText();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("action", action);

		switch (action) {
			case "top-up": {
				log.debug("TopUp> {} ", address);
				Address classicAddress = Address.of(address);
				FundAccountRequest request = FundAccountRequest.of(classicAddress);
				FaucetAccountResponse faucetAccountResponse = faucetClient.fundAccount(request);

				result.put("account", faucetAccountResponse.account().classicAddress().value());
				result.put("amount", faucetAccountResponse.amount());
				result.put("balance", faucetAccountResponse.balance());
				break;
			}
			default: {
				throw new UnknownActionException(String.format("Unknown action [%s] encountered.", action));
			}
		}
		return result;
	}

	@PostMapping(path = "/accounts", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public String postAccounts(@RequestBody JsonNode node) throws JsonRpcClientErrorException {
		log.debug("RequestPayload> {}", node.toPrettyString());
		String address = node.at("/account").asText();
		Address classicAddress = Address.builder().value(address).build();
		log.debug("classAddress> {}", classicAddress.value());

		AccountInfoRequestParams accountInfoRequestParams = AccountInfoRequestParams.builder()
				.account(wallet.classicAddress())
				.ledgerIndex(LedgerIndex.VALIDATED)
				.build();
		AccountInfoResult accountInfo = xrplClient.accountInfo(accountInfoRequestParams);
		return accountInfo.toString();
	}

	@PostMapping(path = "/payments", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> postPayments(@RequestBody PaymentModel paymentModel)
			throws JsonRpcClientErrorException, JsonProcessingException {

		log.debug("Request received> {}", paymentModel);

		Address payeeClassicAddress = Address.builder().value(paymentModel.getAccount()).build();
		log.debug("ClassicAddress> {}", payeeClassicAddress.value());

		XrpCurrencyAmount openLedgerFee = xrplClient.fee().drops().openLedgerFee();
		log.debug("OpenLedgerFee> {}", openLedgerFee.value());

		// TODO: Handle ledgerIndex exception.
		LedgerIndex ledgerIndex = xrplClient
				.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build())
				.ledgerIndex()
				.orElseThrow(() -> new RuntimeException(""));
		log.debug("LedgerIndex>  {}", ledgerIndex.value());

		UnsignedInteger sequence = xrplClient.accountInfo(AccountInfoRequestParams.of(payeeClassicAddress))
				.accountData()
				.sequence();

		UnsignedInteger lastLedgerSequence = UnsignedInteger
				.valueOf(ledgerIndex.plus(UnsignedLong.valueOf(4)).unsignedLongValue().intValue());

		ImmutablePayment payment = Payment.builder()
				.account(payeeClassicAddress)
				.amount(XrpCurrencyAmount.ofXrp(new BigDecimal(paymentModel.getAmount())))
				.destination(Address.of(paymentModel.getDestination()))
				.sequence(sequence)
				.fee(openLedgerFee)
				.signingPublicKey(wallet.publicKey())
				.lastLedgerSequence(lastLedgerSequence)
				.build();

		log.debug("Payment> {}", payment);

		SubmitResult<Transaction> submitResult = xrplClient.submit(wallet, payment);
		log.debug("SubmitResult> {}", submitResult);
		
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("accepted", submitResult.accepted());
		result.put("applied", submitResult.applied());
		result.put("result", submitResult.result());
		result.put("account", submitResult.transactionResult().transaction().account());
		result.put("fee", submitResult.transactionResult().transaction().fee());
		
		return result;
	}
}
