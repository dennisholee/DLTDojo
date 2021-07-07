package io.forest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;
import org.xrpl.xrpl4j.wallet.WalletFactory;

import okhttp3.HttpUrl;

@Configuration
public class XrpConfig {

	@Value("${xrp.net.host}:${xrp.net.port}")
	String netEndpoint;

	@Value("${xrp.faucet.host}:${xrp.faucet.port}")
	String facetEndpoint;

	@Bean
	public XrplClient xrplClient() {
		HttpUrl rippledUrl = HttpUrl.get(netEndpoint);
		return new XrplClient(rippledUrl);
	}

	@Profile("dev")
	@Bean
	@Scope("prototype")
	public Wallet wallet() {
		WalletFactory walletFactory = DefaultWalletFactory.getInstance();
		return walletFactory.randomWallet(true).wallet();
	}

	@Profile("dev")
	@Bean
	public FaucetClient faucetClient() {
		return FaucetClient.construct(HttpUrl.get(facetEndpoint));
	}
}
