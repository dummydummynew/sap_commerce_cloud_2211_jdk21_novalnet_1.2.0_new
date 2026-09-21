/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.facades.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.store.BaseStoreModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.service.payment.NovalnetWebhookService;



@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetWebhookConfigFacadeTest
{
	private static final String PRODUCT_KEY = "productKey123";
	private static final String PAYMENT_KEY = "paymentKey456";
	private static final String WEBHOOK_URL = "https://example.com/novalnet/webhook";
	private static final String ERROR_MESSAGE = "Novalnet webhook configuration failed";

	@Mock
	private NovalnetWebhookService novalnetWebhookService;

	@Mock
	private BaseStoreModel baseStoreModel;

	@InjectMocks
	private DefaultNovalnetWebhookConfigFacade novalnetWebhookConfigFacade;


	@Test
	public void shouldDelegateToServiceWhenConfigureWebhookSucceeds() throws Exception
	{
		willDoNothing().given(novalnetWebhookService).configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStoreModel);

		assertThatCode(() -> novalnetWebhookConfigFacade.configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStoreModel))
				.doesNotThrowAnyException();

		verify(novalnetWebhookService).configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStoreModel);

		verifyNoMoreInteractions(novalnetWebhookService);
	}


	@Test
	public void shouldPropagateExceptionWhenConfigureWebhookFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetWebhookService).configureWebhook(PRODUCT_KEY, PAYMENT_KEY,
				WEBHOOK_URL, baseStoreModel);

		assertThatThrownBy(
				() -> novalnetWebhookConfigFacade.configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStoreModel))
						.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetWebhookService).configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStoreModel);

		verifyNoMoreInteractions(novalnetWebhookService);
	}
}

