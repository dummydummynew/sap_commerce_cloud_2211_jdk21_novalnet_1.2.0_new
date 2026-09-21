/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;


import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.store.BaseStoreModel;

import java.util.List;

import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;


/**
 * Defines operations for managing Novalnet payment information and transactions.
 */
public interface NovalnetPaymentService
{
	/**
	 * Retrieves the applicable Novalnet payment information model.
	 *
	 * @param paymentInfo the list of Novalnet payment information models
	 * @return the applicable Novalnet payment information model
	 */
	public NovalnetPaymentInfoModel getPaymentModel(List<NovalnetPaymentInfoModel> paymentInfo);

	/**
	 * Handles reference transaction information returned by Novalnet.
	 *
	 * @param response the Novalnet transaction response
	 * @param customerNo the customer number
	 * @param currentPayment the currently selected payment method
	 */
	public void handleReferenceTransactionInfo(StringBuilder response, String customerNo, String currentPayment);

	/**
	 * Retrieves the display name of the specified payment method.
	 *
	 * @param currentPayment the currently selected payment method
	 * @return the payment method name
	 */
	public String getPaymentName(String currentPayment);

	/**
	 * Retrieves the current base store model.
	 *
	 * @return the current base store model
	 */
	public BaseStoreModel getBaseStoreModel();

	/**
	 * Updates payment information with the specified transaction status.
	 *
	 * @param orderReference the payment information associated with the order
	 * @param tidStatus the transaction status
	 */
	public void updatePaymentInfo(List<NovalnetPaymentInfoModel> orderReference, String tidStatus);

	/**
	 * Updates callback information with the callback transaction ID and paid amount.
	 *
	 * @param callbackTid the callback transaction ID
	 * @param orderReference the callback information associated with the order
	 * @param orderPaidAmount the amount paid for the order
	 */
	public void updateCallbackInfo(long callbackTid, List<NovalnetCallbackInfoModel> orderReference, int orderPaidAmount);

	/**
	 * Updates the payment status of the specified order to cancelled.
	 *
	 * @param orderCode the unique code of the order
	 */
	public void updateCancelStatus(final String orderCode);

	/**
	 * Creates a payment transaction entry for the specified cart.
	 *
	 * @param requestId the request identifier
	 * @param cartModel the cart associated with the transaction
	 * @param amount the transaction amount
	 * @param backendTransactionComments the backend transaction comments
	 * @param currencyCode the currency code of the transaction
	 * @return the created payment transaction entry
	 */
	public PaymentTransactionEntryModel createTransactionEntry(String requestId, CartModel cartModel, int amount,
			String backendTransactionComments, String currencyCode);

	/**
	 * Adds the Novalnet payment process data to the specified model.
	 *
	 * @param model the model used to store payment process data
	 * @param paymentDetailsForm the payment details form
	 * @param cartData the current cart data
	 * @throws CMSItemNotFoundException if the required CMS item cannot be found
	 */
	void addPaymentProcess(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData)
			throws CMSItemNotFoundException;

}
