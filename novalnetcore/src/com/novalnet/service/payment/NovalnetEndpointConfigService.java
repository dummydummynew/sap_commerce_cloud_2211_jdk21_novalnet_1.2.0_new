/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;


/**
 * Defines operations for retrieving Novalnet API endpoint configuration.
 */
public interface NovalnetEndpointConfigService
{
	/**
	 * Retrieves the Novalnet payment API URL.
	 *
	 * @return the payment API URL
	 */
	public String getPaymentUrl();

	/**
	 * Retrieves the Novalnet hosted payment API URL.
	 *
	 * @return the hosted payment API URL
	 */
	public String getPaymentHostedUrl();

	/**
	 * Retrieves the Novalnet authorization API URL.
	 *
	 * @return the authorization API URL
	 */
	public String getAuthorizeUrl();

	/**
	 * Retrieves the Novalnet hosted authorization API URL.
	 *
	 * @return the hosted authorization API URL
	 */
	public String getAuthorizeHostedUrl();

	/**
	 * Retrieves the Novalnet transaction capture API URL.
	 *
	 * @return the transaction capture API URL
	 */
	public String getTransactionCaptureUrl();

	/**
	 * Retrieves the Novalnet transaction cancellation API URL.
	 *
	 * @return the transaction cancellation API URL
	 */
	public String getTransactionCancelUrl();

	/**
	 * Retrieves the Novalnet transaction refund API URL.
	 *
	 * @return the transaction refund API URL
	 */
	public String getTransactionRefundUrl();

	/**
	 * Retrieves the Novalnet transaction details API URL.
	 *
	 * @return the transaction details API URL
	 */
	public String getTransactionDetailsUrl();

	/**
	 * Retrieves the Novalnet transaction update API URL.
	 *
	 * @return the transaction update API URL
	 */
	public String getTransactionUpdateUrl();

	/**
	 * Retrieves the Novalnet webhook configuration API URL.
	 *
	 * @return the webhook configuration API URL
	 */
	public String getWebhookConfigureUrl();

	/**
	 * Retrieves the Novalnet merchant details API URL.
	 *
	 * @return the merchant details API URL
	 */
	public String getMerchantDetailsUrl();

}
