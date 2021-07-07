package io.forest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class PaymentTest {

	@Test
	void test() throws JsonProcessingException {
		PaymentModel payment = new PaymentModel("payment", "account", "amount", "destination");
		
		System.out.println(new ObjectMapper().writeValueAsString(payment));
	}

}
