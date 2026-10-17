package com.fis.bic.sso.helper;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fis.bic.sso.utility.BicSsoException;

public class SlapiTokenHandlerTest {

	@Test
	public void getDecryptedCredentialsFailsClosedOnBadToken() {
		SlapiTokenHandler handler = SlapiTokenHandler.getSingletonObject();
		assertThrows(BicSsoException.class,
				() -> handler.getDecryptedCredentials("not-a-slapi-token", "BO_LP_SSO"));
	}
}
