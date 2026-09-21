/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetEndpointConfigServiceTest
{
	private static final String PAYMENT_URL_KEY = "novalnet.payment.url";
	private static final String PAYMENT_HOSTED_URL_KEY = "novalnet.payment.hosted.url";
	private static final String AUTHORIZE_URL_KEY = "novalnet.authorize.url";
	private static final String AUTHORIZE_HOSTED_URL_KEY = "novalnet.authorize.hosted.url";
	private static final String TRANSACTION_CAPTURE_URL_KEY = "novalnet.transaction.capture.url";
	private static final String TRANSACTION_CANCEL_URL_KEY = "novalnet.transaction.cancel.url";
	private static final String TRANSACTION_REFUND_URL_KEY = "novalnet.transaction.refund.url";
	private static final String TRANSACTION_DETAILS_URL_KEY = "novalnet.transaction.details.url";
	private static final String TRANSACTION_UPDATE_URL_KEY = "novalnet.transaction.update.url";
	private static final String WEBHOOK_CONFIGURE_URL_KEY = "novalnet.webhook.configure.url";
	private static final String MERCHANT_DETAILS_URL_KEY = "novalnet.merchant.details.url";

	private static final String PAYMENT_URL = "https://test.com/payment";
	private static final String PAYMENT_HOSTED_URL = "https://test.com/payment/hosted";
	private static final String AUTHORIZE_URL = "https://test.com/authorize";
	private static final String AUTHORIZE_HOSTED_URL = "https://test.com/authorize/hosted";
	private static final String TRANSACTION_CAPTURE_URL = "https://test.com/transaction/capture";
	private static final String TRANSACTION_CANCEL_URL = "https://test.com/transaction/cancel";
	private static final String TRANSACTION_REFUND_URL = "https://test.com/transaction/refund";
	private static final String TRANSACTION_DETAILS_URL = "https://test.com/transaction/details";
	private static final String TRANSACTION_UPDATE_URL = "https://test.com/transaction/update";
	private static final String WEBHOOK_CONFIGURE_URL = "https://test.com/webhook/configure";
	private static final String MERCHANT_DETAILS_URL = "https://test.com/merchant/details";

	@Mock
	private ConfigurationService configurationService;

	@Mock
	private Configuration configuration;

	@InjectMocks
	private DefaultNovalnetEndpointConfigService novalnetEndpointConfigService;

	@BeforeEach
	public void setUp()
	{
		when(configurationService.getConfiguration()).thenReturn(configuration);
	}

	@Test
	public void getPaymentUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(PAYMENT_URL_KEY)).thenReturn(PAYMENT_URL);

		assertThat(novalnetEndpointConfigService.getPaymentUrl()).isEqualTo(PAYMENT_URL);
	}

	@Test
	public void getPaymentUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(PAYMENT_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getPaymentUrl()).isNull();
	}

	@Test
	public void getPaymentHostedUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(PAYMENT_HOSTED_URL_KEY)).thenReturn(PAYMENT_HOSTED_URL);

		assertThat(novalnetEndpointConfigService.getPaymentHostedUrl()).isEqualTo(PAYMENT_HOSTED_URL);
	}

	@Test
	public void getPaymentHostedUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(PAYMENT_HOSTED_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getPaymentHostedUrl()).isNull();
	}

	@Test
	public void getAuthorizeUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(AUTHORIZE_URL_KEY)).thenReturn(AUTHORIZE_URL);

		assertThat(novalnetEndpointConfigService.getAuthorizeUrl()).isEqualTo(AUTHORIZE_URL);
	}

	@Test
	public void getAuthorizeUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(AUTHORIZE_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getAuthorizeUrl()).isNull();
	}

	@Test
	public void getAuthorizeHostedUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(AUTHORIZE_HOSTED_URL_KEY)).thenReturn(AUTHORIZE_HOSTED_URL);

		assertThat(novalnetEndpointConfigService.getAuthorizeHostedUrl()).isEqualTo(AUTHORIZE_HOSTED_URL);
	}

	@Test
	public void getAuthorizeHostedUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(AUTHORIZE_HOSTED_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getAuthorizeHostedUrl()).isNull();
	}

	@Test
	public void getTransactionCaptureUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(TRANSACTION_CAPTURE_URL_KEY)).thenReturn(TRANSACTION_CAPTURE_URL);

		assertThat(novalnetEndpointConfigService.getTransactionCaptureUrl()).isEqualTo(TRANSACTION_CAPTURE_URL);
	}

	@Test
	public void getTransactionCaptureUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(TRANSACTION_CAPTURE_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getTransactionCaptureUrl()).isNull();
	}

	@Test
	public void getTransactionCancelUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(TRANSACTION_CANCEL_URL_KEY)).thenReturn(TRANSACTION_CANCEL_URL);

		assertThat(novalnetEndpointConfigService.getTransactionCancelUrl()).isEqualTo(TRANSACTION_CANCEL_URL);
	}

	@Test
	public void getTransactionCancelUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(TRANSACTION_CANCEL_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getTransactionCancelUrl()).isNull();
	}

	@Test
	public void getTransactionRefundUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(TRANSACTION_REFUND_URL_KEY)).thenReturn(TRANSACTION_REFUND_URL);

		assertThat(novalnetEndpointConfigService.getTransactionRefundUrl()).isEqualTo(TRANSACTION_REFUND_URL);
	}

	@Test
	public void getTransactionRefundUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(TRANSACTION_REFUND_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getTransactionRefundUrl()).isNull();
	}

	@Test
	public void getTransactionDetailsUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(TRANSACTION_DETAILS_URL_KEY)).thenReturn(TRANSACTION_DETAILS_URL);

		assertThat(novalnetEndpointConfigService.getTransactionDetailsUrl()).isEqualTo(TRANSACTION_DETAILS_URL);
	}

	@Test
	public void getTransactionDetailsUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(TRANSACTION_DETAILS_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getTransactionDetailsUrl()).isNull();
	}

	@Test
	public void getTransactionUpdateUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(TRANSACTION_UPDATE_URL_KEY)).thenReturn(TRANSACTION_UPDATE_URL);

		assertThat(novalnetEndpointConfigService.getTransactionUpdateUrl()).isEqualTo(TRANSACTION_UPDATE_URL);
	}

	@Test
	public void getTransactionUpdateUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(TRANSACTION_UPDATE_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getTransactionUpdateUrl()).isNull();
	}

	@Test
	public void getWebhookConfigureUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(WEBHOOK_CONFIGURE_URL_KEY)).thenReturn(WEBHOOK_CONFIGURE_URL);

		assertThat(novalnetEndpointConfigService.getWebhookConfigureUrl()).isEqualTo(WEBHOOK_CONFIGURE_URL);
	}

	@Test
	public void getWebhookConfigureUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(WEBHOOK_CONFIGURE_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getWebhookConfigureUrl()).isNull();
	}

	@Test
	public void getMerchantDetailsUrlReturnsConfiguredUrl()
	{
		when(configuration.getString(MERCHANT_DETAILS_URL_KEY)).thenReturn(MERCHANT_DETAILS_URL);

		assertThat(novalnetEndpointConfigService.getMerchantDetailsUrl()).isEqualTo(MERCHANT_DETAILS_URL);
	}

	@Test
	public void getMerchantDetailsUrlReturnsNullWhenNotConfigured()
	{
		when(configuration.getString(MERCHANT_DETAILS_URL_KEY)).thenReturn(null);

		assertThat(novalnetEndpointConfigService.getMerchantDetailsUrl()).isNull();
	}
}
