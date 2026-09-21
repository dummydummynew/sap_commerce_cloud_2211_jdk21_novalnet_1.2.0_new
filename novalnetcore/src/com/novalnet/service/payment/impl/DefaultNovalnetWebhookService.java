/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment.impl;

import de.hybris.platform.store.BaseStoreModel;

import org.apache.log4j.Logger;

import com.novalnet.exception.NovalnetException;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetWebhookService;

import jakarta.annotation.Resource;


/**
* Default implementation of {@link NovalnetWebhookService} for configuring the Novalnet webhook.
*/
public class DefaultNovalnetWebhookService implements NovalnetWebhookService
{
	private static final Logger LOG = Logger.getLogger(DefaultNovalnetWebhookService.class);

	@Resource(name = "novalnetApiService")
	private NovalnetApiService novalnetApiService;

	@Override
	public void configureWebhook(String productKey, String paymentKey, String webhookUrl, BaseStoreModel baseStore)
			throws NovalnetException
	{
		String response = novalnetApiService.configureWebhookUrl(productKey, paymentKey, webhookUrl, baseStore);

		LOG.info("Webhook Response: " + response);
	}

}
