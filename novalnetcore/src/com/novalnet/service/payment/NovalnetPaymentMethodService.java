/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.store.BaseStoreModel;

import java.util.Map;

import org.springframework.ui.Model;

import com.fasterxml.jackson.databind.JsonNode;
import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.exception.NovalnetException;

import jakarta.servlet.http.HttpServletRequest;


/**
 * Defines operations for processing Novalnet payment transactions.
 */
public interface NovalnetPaymentMethodService
{
	/**
	 * Retrieves merchant details from Novalnet using the product activation key.
	 *
	 * @param productActivationKey the Novalnet product activation key
	 * @param baseStore the base store associated with the payment request
	 * @return the merchant details response
	 * @throws Exception if an error occurs while retrieving merchant details
	 */
	public String callNovalnetMerchantDetails(String productActivationKey, BaseStoreModel baseStore) throws Exception;

	/**
	 * Adds the payment process data required to the specified model.
	 *
	 * @param model the model used to store payment process data
	 * @param paymentDetailsForm the payment details form
	 * @param cartData the current cart data
	 * @throws CMSItemNotFoundException if the required CMS item cannot be found
	 */
	public void addPaymentProcess(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData)
			throws CMSItemNotFoundException;

	/**
	 * Populates the customer address details required for payment processing.
	 *
	 * @param model the model used to store customer information
	 * @param paymentDetailsForm the payment details form
	 * @param cartData the current cart data
	 * @param customer the customer information to populate
	 * @param addressData the customer address information
	 */
	public void populateCustomerAddressDetails(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData,
			Customer customer, AddressData addressData);

	/**
	 * Processes one-click token data for the selected payment method.
	 *
	 * @param currentPayment the currently selected payment method
	 * @param paymentDetailsForm the payment details form
	 * @param model the model containing payment process data
	 * @param cartData the current cart data
	 * @param deliveryAddress the delivery address associated with the cart
	 * @return true if the one-click token data was processed successfully; otherwise false
	 * @throws CMSItemNotFoundException if the required CMS item cannot be found
	 */
	boolean processOneClickTokenData(String currentPayment, NovalnetPaymentDetailsForm paymentDetailsForm, Model model,
			CartData cartData, AddressData deliveryAddress) throws CMSItemNotFoundException;

	/**
	 * Processes the transaction result returned by Novalnet.
	 *
	 * @param resultMap the transaction result data
	 * @return true if the transaction was processed successfully; otherwise false
	 */
	public boolean processTransaction(Map<String, String> resultMap);

	/**
	 * Creates a Novalnet transaction request.
	 *
	 * @param request the HTTP servlet request
	 * @param baseStore the base store associated with the transaction
	 * @param currentPayment the currently selected payment method
	 * @param customerNo the customer number
	 * @param orderAmountCent the order amount in cents
	 * @param cartData the current cart data
	 * @return the created transaction request
	 */
	public StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment,
			String customerNo, Integer orderAmountCent, CartData cartData);

	/**
	 * Books a wallet transaction with Novalnet.
	 *
	 * @param request the HTTP servlet request
	 * @param baseStore the base store associated with the transaction
	 * @param customerNo the customer number
	 * @param orderAmountCent the order amount in cents
	 * @param cartData the current cart data
	 * @return the wallet transaction response
	 * @throws NovalnetException if an error occurs while booking the wallet transaction
	 */
	public String bookWalletTransaction(HttpServletRequest request, BaseStoreModel baseStore, String customerNo,
			Integer orderAmountCent, CartData cartData) throws NovalnetException;

	/**
	 * Handles storing payment information based on the selected payment method and Novalnet response.
	 *
	 * @param currentPayment the currently selected payment method
	 * @param response the Novalnet transaction response
	 * @param customerJsonObject the customer information from the transaction response
	 */
	public void handleStorePayment(String currentPayment, StringBuilder response, JsonNode customerJsonObject);
}
