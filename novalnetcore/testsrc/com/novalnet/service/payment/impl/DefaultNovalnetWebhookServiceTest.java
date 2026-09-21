/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.store.BaseStoreModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.service.http.NovalnetApiService;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetWebhookServiceTest
{
	private static final String PRODUCT_KEY = "testProductKey";
	private static final String PAYMENT_KEY = "testPaymentKey";
	private static final String WEBHOOK_URL = "https://test.com/webhook";
	private static final String SUCCESS_RESPONSE = "{\"result\":{\"status\":\"SUCCESS\"}}";
	private static final String API_FAILURE_MESSAGE = "Novalnet API is unreachable";

	@Mock
	private NovalnetApiService novalnetApiService;

	@Mock
	private BaseStoreModel baseStore;

	@InjectMocks
	private DefaultNovalnetWebhookService novalnetWebhookService;

	@Test
	public void configureWebhookCallsApiServiceWhenApiCallSucceeds() throws Exception
	{
		when(novalnetApiService.configureWebhookUrl(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStore)).thenReturn(SUCCESS_RESPONSE);

		novalnetWebhookService.configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStore);

		verify(novalnetApiService).configureWebhookUrl(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStore);
	}

	@Test
	public void configureWebhookPropagatesExceptionWhenApiCallFails() throws Exception
	{
		when(novalnetApiService.configureWebhookUrl(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStore))
				.thenThrow(new RuntimeException(API_FAILURE_MESSAGE));

		assertThatThrownBy(() -> novalnetWebhookService.configureWebhook(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStore))
				.isInstanceOf(RuntimeException.class);

		verify(novalnetApiService).configureWebhookUrl(PRODUCT_KEY, PAYMENT_KEY, WEBHOOK_URL, baseStore);
	}
}

