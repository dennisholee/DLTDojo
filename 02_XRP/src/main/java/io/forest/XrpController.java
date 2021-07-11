package io.forest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetAccountResponse;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountObjectsRequestParams;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.ledger.LedgerObject;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.CheckCash;
import org.xrpl.xrpl4j.model.transactions.CheckCreate;
import org.xrpl.xrpl4j.model.transactions.ImmutablePayment;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.Transaction;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;
import org.xrpl.xrpl4j.wallet.Wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import io.forest.models.CheckClaimModel;
import io.forest.models.PaymentModel;
import io.forest.models.WalletActionModel;
import io.forest.models.WalletActionModel.WalletAction;
import io.forest.models.WalletModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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

	@Profile({ "dev", "sit" })
	@Operation(summary = "Generate two testing wallets on the devnet")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "List of test wallet's classic address") })
	@GetMapping(path = "/wallets")
	@ResponseStatus(HttpStatus.OK)
	public CollectionModel<WalletModel<String>> getWallet()
			throws JsonProcessingException, JsonRpcClientErrorException {

		List<WalletModel<String>> wallets = Arrays.asList(wallet, beneWallet)
				.stream()
				.map(i -> new WalletModel<String>(i.classicAddress().value(), ""))
				.collect(Collectors.toList());

		return CollectionModel.of(wallets, linkTo(methodOn(XrpController.class).getWallet()).withSelfRel(),
				linkTo(methodOn(XrpController.class).postWallet(wallet.classicAddress().value(), null)).withRel(
						"topup"),
				linkTo(methodOn(XrpController.class).postAccounts(null)).withRel("accounts"));
	}

	@Profile({ "dev", "sit" })
	@Operation(summary = "Perform acction against the wallet.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns the details of the updated wallet as a result of applying the action."),
			@ApiResponse(content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = WalletActionModel.class)) }) })
	@PostMapping(path = "/wallets/{address}", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Map<String, Object>> postWallet(
			@Parameter(description = "Wallet's class address") @PathVariable("address") String address,
			@RequestBody WalletActionModel model) throws JsonProcessingException, JsonRpcClientErrorException {
		log.debug("Address> {}, RequestPayload> {}", address, model);

		WalletAction action = model.getAction();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("action", action);

		switch (action) {
			case topup: {
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

		return EntityModel.of(result, linkTo(methodOn(XrpController.class).postWallet(address, model)).withSelfRel(),
				linkTo(methodOn(XrpController.class).postAccounts(new WalletModel<String>(address))).withRel("account"),
				linkTo(methodOn(XrpController.class).postPayments(new PaymentModel(null, address, null, null)))
						.withRel("payment"));
	}

	@PostMapping(path = "/accounts", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Map<String, Object>> postAccounts(@RequestBody WalletModel<String> walletModel)
			throws JsonRpcClientErrorException {
		// log.debug("RequestPayload> {}", node.toPrettyString());
		// String address = node.at("/account").asText();
		String address = walletModel.getAddress();
		Address classicAddress = Address.builder().value(address).build();
		log.debug("classAddress> {}", classicAddress.value());

		AccountInfoRequestParams accountInfoRequestParams = AccountInfoRequestParams.builder()
				.account(wallet.classicAddress())
				.ledgerIndex(LedgerIndex.VALIDATED)
				.build();
		AccountInfoResult accountInfo = xrplClient.accountInfo(accountInfoRequestParams);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("account", accountInfo.accountData().account().value());
		result.put("balance", accountInfo.accountData().balance().value());

		return EntityModel.of(result, linkTo(methodOn(XrpController.class).postAccounts(walletModel)).withSelfRel());
	}

	@PostMapping(path = "/payments", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Map<String, Object>> postPayments(@RequestBody PaymentModel paymentModel)
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

		return EntityModel.of(result, linkTo(methodOn(XrpController.class).postPayments(paymentModel)).withSelfRel());
	}

	@PostMapping(path = "/checks", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Map<String, Object>> postChecks(@RequestBody PaymentModel paymentModel)
			throws JsonRpcClientErrorException {
		Address payeeAccount = Address.of(paymentModel.getAccount());
		Address destination = Address.of(paymentModel.getDestination());

		XrpCurrencyAmount openLedgerFee = xrplClient.fee().drops().openLedgerFee();
		log.debug("OpenLedgerFee> {}", openLedgerFee.value());

		LedgerIndex ledgerIndex = xrplClient
				.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build())
				.ledgerIndex()
				.orElseThrow(() -> new RuntimeException(""));

		UnsignedInteger sequence = xrplClient.accountInfo(AccountInfoRequestParams.of(payeeAccount))
				.accountData()
				.sequence();

		UnsignedInteger lastLedgerSequence = UnsignedInteger
				.valueOf(ledgerIndex.plus(UnsignedLong.valueOf(4)).unsignedLongValue().intValue());

		Transaction check = CheckCreate.builder()
				.account(payeeAccount)
				.destination(destination)
				.fee(openLedgerFee)
				.sequence(sequence)
				.sendMax(XrpCurrencyAmount.ofXrp(new BigDecimal(paymentModel.getAmount())))
				.signingPublicKey(wallet.publicKey())
				.lastLedgerSequence(lastLedgerSequence)
				.build();

		SubmitResult<Transaction> submitResult = xrplClient.submit(wallet, check);
		log.debug("SubmitResult> {}", submitResult);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("accepted", submitResult.accepted());
		result.put("applied", submitResult.applied());
		result.put("result", submitResult.result());
		result.put("account", submitResult.transactionResult().transaction().account());
		result.put("fee", submitResult.transactionResult().transaction().fee());

		return EntityModel.of(result, linkTo(methodOn(XrpController.class).postChecks(paymentModel)).withSelfRel());
	}

	@GetMapping("/checks")
	public CollectionModel<LedgerObject> getChecks(@RequestHeader("account") String account)
			throws JsonRpcClientErrorException {
		log.debug("Request received> {}", account);

		Address accountAddress = Address.of(account);
		List<LedgerObject> accountObjects = xrplClient.accountObjects(AccountObjectsRequestParams.of(accountAddress))
				.accountObjects();

		return CollectionModel.of(accountObjects,
				linkTo(methodOn(XrpController.class).getChecks(account)).withSelfRel());
	}

	@PostMapping("/checkClaim")
	public void postCheckClaim(@RequestBody CheckClaimModel checkClaimModel) {
		log.debug("Request received> {}", checkClaimModel);

		Address beneAccount = Address.of(checkClaimModel.getAccount());

		CheckCash.builder()
				.account(beneAccount)
				.amount(XrpCurrencyAmount.ofXrp(new BigDecimal(checkClaimModel.getAmount())))
				.checkId(null);// checkClaimModel.getCheckId());
	}
}
