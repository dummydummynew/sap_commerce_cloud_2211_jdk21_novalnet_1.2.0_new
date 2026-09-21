/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.novalnet.service.http.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.service.payment.NovalnetEndpointConfigService;



@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetApiServiceTest
{
	private static final String URL = "https://payport.novalnet.de/v2/transaction";
	private static final String WEBHOOK_URL = "https://test.com/novalnet/webhook";
	private static final String JSON = "{\"transaction\":{\"amount\":1000}}";
	private static final String API_RESPONSE = "{\"status\":\"success\"}";
	private static final String PAYMENT_ACCESS_KEY = "payment-access-key";
	private static final String PRODUCT_ACTIVATION_KEY = "product-activation-key";
	private static final String ENDPOINT_URL = "https://test.com/v2/webhook/configure";

	@Mock
	private BaseStoreService baseStoreService;

	@Mock
	private BaseStoreModel baseStoreModel;

	@Mock
	private LanguageModel languageModel;

	@Mock
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Mock
	private HttpClient httpClient;

	@Mock
	private HttpResponse<String> httpResponse;

	@InjectMocks
	private DefaultNovalnetApiService testObj;

	private MockedStatic<HttpClient> httpClientMock;


	@BeforeEach
	public void setUp()
	{
		httpClientMock = mockStatic(HttpClient.class);
	}

	@AfterEach
	public void tearDown()
	{
		httpClientMock.close();
		Thread.interrupted();
	}


	private void mockHttpClientBuilder()
	{
		HttpClient.Builder builder = mock(HttpClient.Builder.class);

		httpClientMock.when(HttpClient::newBuilder).thenReturn(builder);

		given(builder.connectTimeout(any())).willReturn(builder);
		given(builder.build()).willReturn(httpClient);
	}


	@Test
	public void shouldReturnResponseWhenSendRequestSucceeds() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreService.getCurrentBaseStore()).willReturn(baseStoreModel);
		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpResponse.body()).willReturn(API_RESPONSE);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);

		StringBuilder result = testObj.sendRequest(URL, JSON);

		assertThat(result).hasToString(API_RESPONSE);
		verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
		verify(httpResponse).body();
	}

	@Test
	public void shouldReturnEmptyResponseWhenSendRequestFailsWithIOException() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreService.getCurrentBaseStore()).willReturn(baseStoreModel);
		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new IOException("Connection failed"));

		StringBuilder result = testObj.sendRequest(URL, JSON);

		assertThat(result).isEmpty();
	}


	@Test
	public void shouldReturnEmptyResponseWhenSendRequestIsInterrupted() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreService.getCurrentBaseStore()).willReturn(baseStoreModel);
		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new InterruptedException("Request interrupted"));

		StringBuilder result = testObj.sendRequest(URL, JSON);

		assertThat(result).isEmpty();
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}


	@Test
	public void shouldReturnResponseWhenFollowupSendRequestSucceeds() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpResponse.body()).willReturn(API_RESPONSE);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);

		StringBuilder result = testObj.followupSendRequest(URL, JSON, baseStoreModel);

		assertThat(result).hasToString(API_RESPONSE);
		verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
		verify(httpResponse).body();
	}


	@Test
	public void shouldThrowIllegalArgumentExceptionWhenFollowupBaseStoreIsNull()
	{
		assertThatThrownBy(() -> testObj.followupSendRequest(URL, JSON, null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("BaseStore cannot be null");
	}


	@Test
	public void shouldReturnEmptyResponseWhenFollowupSendRequestFailsWithIOException() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new IOException("Connection failed"));

		StringBuilder result = testObj.followupSendRequest(URL, JSON, baseStoreModel);

		assertThat(result).isEmpty();
	}


	@Test
	public void shouldReturnEmptyResponseWhenFollowupSendRequestIsInterrupted() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new InterruptedException("Request interrupted"));

		StringBuilder result = testObj.followupSendRequest(URL, JSON, baseStoreModel);

		assertThat(result).isEmpty();
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}


	@Test
	public void shouldReturnResponseWhenBookTransactionAmountSucceeds() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpResponse.body()).willReturn(API_RESPONSE);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);

		StringBuilder result = testObj.bookTransactionAmount(URL, JSON, baseStoreModel);

		assertThat(result).hasToString(API_RESPONSE);
		verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
		verify(httpResponse).body();
	}


	@Test
	public void shouldReturnEmptyResponseWhenBookTransactionAmountFailsWithIOException() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new IOException("Connection failed"));

		StringBuilder result = testObj.bookTransactionAmount(URL, JSON, baseStoreModel);

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldReturnEmptyResponseWhenBookTransactionAmountIsInterrupted() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new InterruptedException("Request interrupted"));

		StringBuilder result = testObj.bookTransactionAmount(URL, JSON, baseStoreModel);

		assertThat(result).isEmpty();
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}


	@Test
	public void shouldReturnResponseWhenFetchMerchantDetailsSucceeds() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpResponse.body()).willReturn(API_RESPONSE);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);

		String result = testObj.fetchMerchantDetails(URL, JSON, baseStoreModel);

		assertThat(result).isEqualTo(API_RESPONSE);
		verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
		verify(httpResponse).body();
	}


	@Test
	public void shouldReturnEmptyResponseWhenFetchMerchantDetailsFailsWithIOException() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new IOException("Connection failed"));

		String result = testObj.fetchMerchantDetails(URL, JSON, baseStoreModel);

		assertThat(result).isEmpty();
	}


	@Test
	public void shouldReturnEmptyResponseWhenFetchMerchantDetailsIsInterrupted() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getNovalnetPaymentAccessKey()).willReturn(PAYMENT_ACCESS_KEY);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new InterruptedException("Request interrupted"));

		String result = testObj.fetchMerchantDetails(URL, JSON, baseStoreModel);

		assertThat(result).isEmpty();
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}


	@Test
	public void shouldReturnResponseWhenConfigureWebhookUrlSucceeds() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getDefaultLanguage()).willReturn(languageModel);
		given(languageModel.getIsocode()).willReturn("de");
		given(novalnetEndpointConfigService.getWebhookConfigureUrl()).willReturn(ENDPOINT_URL);
		given(httpResponse.body()).willReturn(API_RESPONSE);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);

		String result = testObj.configureWebhookUrl(PRODUCT_ACTIVATION_KEY, PAYMENT_ACCESS_KEY, WEBHOOK_URL, baseStoreModel);

		assertThat(result).isEqualTo(API_RESPONSE);
		verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
		verify(httpResponse).body();
	}


	@Test
	public void shouldReturnEmptyResponseWhenConfigureWebhookUrlFailsWithIOException() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getDefaultLanguage()).willReturn(languageModel);
		given(languageModel.getIsocode()).willReturn("de");
		given(novalnetEndpointConfigService.getWebhookConfigureUrl()).willReturn(ENDPOINT_URL);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new IOException("Connection failed"));

		String result = testObj.configureWebhookUrl(PRODUCT_ACTIVATION_KEY, PAYMENT_ACCESS_KEY, WEBHOOK_URL, baseStoreModel);

		assertThat(result).isEmpty();
	}


	@Test
	public void shouldReturnEmptyResponseWhenConfigureWebhookUrlIsInterrupted() throws Exception
	{
		mockHttpClientBuilder();

		given(baseStoreModel.getDefaultLanguage()).willReturn(languageModel);
		given(languageModel.getIsocode()).willReturn("de");
		given(novalnetEndpointConfigService.getWebhookConfigureUrl()).willReturn(ENDPOINT_URL);
		given(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.willThrow(new InterruptedException("Request interrupted"));

		String result = testObj.configureWebhookUrl(PRODUCT_ACTIVATION_KEY, PAYMENT_ACCESS_KEY, WEBHOOK_URL, baseStoreModel);

		assertThat(result).isEmpty();
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}


	@Test
	public void shouldThrowExceptionWhenConfigureWebhookUrlHasNullBaseStore()
	{
		assertThatThrownBy(() -> testObj.configureWebhookUrl(PRODUCT_ACTIVATION_KEY, PAYMENT_ACCESS_KEY, WEBHOOK_URL, null))
				.isInstanceOf(NullPointerException.class);
	}
}
