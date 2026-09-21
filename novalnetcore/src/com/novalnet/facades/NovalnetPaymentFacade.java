
/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.facades;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.store.BaseStoreModel;

import java.util.Map;

import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.payment.request.Customer;

import jakarta.servlet.http.HttpServletRequest;


/**
 * Defines operations for processing Novalnet payment transactions and payment data.
 */
public interface NovalnetPaymentFacade
{
	/**
	 * Retrieves merchant details from Novalnet.
	 *
	 * @param productActivationKey the Novalnet product activation key
	 * @param baseStore the current base store
	 * @return the merchant details response
	 * @throws Exception if an error occurs while retrieving merchant details
	 */
	public String callNovalnetMerchantDetails(String productActivationKey, BaseStoreModel baseStore) throws Exception;

	/**
	 * Adds the Novalnet payment process data to the model.
	 *
	 * @param model the Spring model
	 * @param paymentDetailsForm the Novalnet payment details form
	 * @param cartData the current cart data
	 * @throws CMSItemNotFoundException if the required CMS item cannot be found
	 */
	public void addPaymentProcess(final Model model, final NovalnetPaymentDetailsForm paymentDetailsForm, final CartData cartData)
			throws CMSItemNotFoundException;


	/**
	 * Populates the customer address details for the payment process.
	 *
	 * @param model the Spring model
	 * @param paymentDetailsForm the Novalnet payment details form
	 * @param cartData the current cart data
	 * @param customer the customer data
	 * @param addressData the customer address data
	 */
	public void populateCustomerAddressDetails(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData,
			Customer customer, AddressData addressData);

	/**
	 * Processes one-click token data for the selected payment method.
	 *
	 * @param currentPayment the current payment method
	 * @param paymentDetailsForm the Novalnet payment details form
	 * @param model the Spring model
	 * @param cartData the current cart data
	 * @param deliveryAddress the delivery address
	 * @return true if the one-click token data was processed successfully; otherwise false
	 * @throws CMSItemNotFoundException if the required CMS item cannot be found
	 */
	public boolean processOneClickTokenData(String currentPayment, NovalnetPaymentDetailsForm paymentDetailsForm, Model model,
			CartData cartData, AddressData deliveryAddress) throws CMSItemNotFoundException;

	/**
	 * Processes the Novalnet transaction result.
	 *
	 * @param resultMap the transaction result data
	 * @return true if the transaction was processed successfully; otherwise false
	 */
	public boolean processTransaction(Map<String, String> resultMap);

	/**
	 * Creates a Novalnet transaction request.
	 *
	 * @param request the HTTP servlet request
	 * @param baseStore the current base store
	 * @param currentPayment the current payment method
	 * @param customerNo the customer number
	 * @param orderAmountCent the order amount in cents
	 * @param cartData the current cart data
	 * @return the generated transaction request
	 */
	public StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment,
			String customerNo,
			Integer orderAmountCent, CartData cartData);


	/**
	 * Books a Novalnet wallet transaction.
	 *
	 * @param request the HTTP servlet request
	 * @param baseStore the current base store
	 * @param customerNo the customer number
	 * @param orderAmountCent the order amount in cents
	 * @param cartData the current cart data
	 * @return the result of the wallet transaction booking
	 * @throws Exception if an error occurs while booking the wallet transaction
	 */
	public String bookWalletTransaction(HttpServletRequest request, BaseStoreModel baseStore, String customerNo,
			Integer orderAmountCent,
			CartData cartData) throws Exception;

}


