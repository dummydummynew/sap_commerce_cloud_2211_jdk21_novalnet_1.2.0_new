/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.localization.Localization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novalnet.dto.payment.request.Custom;
import com.novalnet.dto.payment.request.NovalnetPaymentRequest;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentService;

import jakarta.annotation.Resource;


/**
 * Verifies the outcome of a Novalnet transaction, creates the corresponding SAP Commerce order, updates the transaction with the created
 * order information, and handles payment reference data for future one-click purchases.
 */
public class NovalnetTransactionResultService
{
	private static final String TRANSACTION = "transaction";

	private static final String PAYMENT_DATA = "payment_data";

	private static final String NOVALNET_CHECKOUT_ERROR = "novalnetCheckoutError";

	private static final String NOVALNET_ZERO_AMOUNT_BOOKING = "novalnetZeroAmountBooking";

	private static final String NOVALNET_UNKNOWN_ERROR = "novalnet.unknown.error";

	private static final String NOVALNET_CREDIT_CARD = "novalnetCreditCard";

	private static final String NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA = "novalnetCreditCardStorePaymentData";

	private static final String NOVALNET_GOOGLE_PAY = "novalnetGooglePay";

	private static final String NOVALNET_APPLE_PAY = "novalnetApplePay";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Logger LOGGER = LoggerFactory.getLogger(NovalnetTransactionResultService.class);

	@Resource
	private NovalnetApiService novalnetApiService;

	@Resource
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Resource
	private SessionService sessionService;

	@Resource
	private NovalnetCheckoutService novalnetCheckoutService;

	@Resource
	private NovalnetPaymentService novalnetPaymentService;

	/**
	 * Processes the Novalnet transaction result and handles the corresponding order creation.
	 *
	 * @param resultMap
	 *           the transaction result data received from the payment request
	 * @return {@code true} if the transaction is processed successfully; otherwise, {@code false}
	 */
	public boolean processTransaction(Map<String, String> resultMap)
	{
		Transaction transaction = new Transaction();
		Custom custom = new Custom();
		transaction.setTid(resultMap.get("tid"));
		custom.setLang(resolveLanguageCode());
		NovalnetPaymentRequest novalnetRequest = new NovalnetPaymentRequest();
		novalnetRequest.setTransaction(transaction);
		novalnetRequest.setCustom(custom);
		String jsonString = serializeOrSetError(novalnetRequest, "Error while converting Novalnet request to JSON");
		if (jsonString == null)
		{
			return false;
		}
		StringBuilder response = novalnetApiService.sendRequest(novalnetEndpointConfigService.getTransactionDetailsUrl(),
				jsonString);
		LOGGER.info("response: {}", response);
		JsonNode rootNode = parseResponseOrSetError(response);
		if (rootNode == null)
		{
			return false;
		}
		JsonNode resultJsonObject = rootNode.path("result");
		JsonNode customerJsonObject = rootNode.path("customer");
		JsonNode transactionJsonObject = rootNode.path(TRANSACTION);
		String currentPayment = sessionService.getAttribute("selectedPaymentMethodId");
		String[] successStatus =
		{ "CONFIRMED", "ON_HOLD", "PENDING" };
		if (Arrays.asList(successStatus).contains(transactionJsonObject.path("status").asText()))
		{
			return handleSuccessfulTransaction(resultMap, custom, currentPayment, response, transactionJsonObject,
					customerJsonObject, jsonString);
		}
		return handleFailedTransaction(resultMap, resultJsonObject);
	}

	/**
	 * Resolves the current session language and converts it to the Novalnet language format.
	 *
	 * @return the uppercase language code, or {@code EN} when no session language is available
	 */
	private String resolveLanguageCode()
	{
		Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
		return language != null ? language.toString().toUpperCase(Locale.ENGLISH) : "EN";
	}

	/**
	 * Serializes a Novalnet payment request to JSON and stores a checkout error when serialization fails.
	 *
	 * @param request
	 *           the Novalnet payment request to serialize
	 * @param errorLogMessage
	 *           the error message to log when serialization fails
	 * @return the serialized JSON string, or {@code null} when serialization fails
	 */
	private String serializeOrSetError(NovalnetPaymentRequest request, String errorLogMessage)
	{
		try
		{
			return MAPPER.writeValueAsString(request);
		}
		catch (JsonProcessingException e)
		{
			LOGGER.error(errorLogMessage, e);
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_UNKNOWN_ERROR));
			return null;
		}
	}

	/**
	 * Parses the Novalnet response JSON and stores a checkout error when parsing fails.
	 *
	 * @param response
	 *           the response received from Novalnet
	 * @return the parsed JSON response, or {@code null} when parsing fails
	 */
	private JsonNode parseResponseOrSetError(StringBuilder response)
	{
		try
		{
			return MAPPER.readTree(response.toString());
		}
		catch (IOException e)
		{
			LOGGER.error("Error while parsing Novalnet response", e);
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_UNKNOWN_ERROR));
			return null;
		}
	}

	/**
	 * Creates the SAP Commerce order for a successful Novalnet transaction and stores the resulting order data.
	 *
	 * @param resultMap
	 *           the transaction result data received from Novalnet
	 * @param custom
	 *           the custom transaction information
	 * @param currentPayment
	 *           the selected payment method
	 * @param response
	 *           the Novalnet transaction response
	 * @param transactionJsonObject
	 *           the transaction details from the Novalnet response
	 * @param customerJsonObject
	 *           the customer details from the Novalnet response
	 * @param initialJsonString
	 *           the initial transaction request JSON
	 * @return {@code true} if the order is created and processed successfully; otherwise, {@code false}
	 */
	private boolean handleSuccessfulTransaction(Map<String, String> resultMap, Custom custom, String currentPayment,
			StringBuilder response, JsonNode transactionJsonObject, JsonNode customerJsonObject, String initialJsonString)
	{
		String orderComments = buildOrderComments(currentPayment, transactionJsonObject);
		AddressData addressData = sessionService.getAttribute("novalnetAddressData");
		int orderAmountCentValue = transactionJsonObject.path("amount").asInt();
		String transactionEmail = customerJsonObject.path("email").asText();
		OrderData orderData;
		String bankDetails = "";
		try
		{
			orderData = novalnetCheckoutService.saveOrderData(orderComments, currentPayment,
					transactionJsonObject.path("status").asText(), orderAmountCentValue,
					transactionJsonObject.path("currency").asText(), transactionJsonObject.path("tid").asText(), transactionEmail,
					addressData, bankDetails);
		}
		catch (InvalidCartException e)
		{
			sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, Localization.getLocalizedString(NOVALNET_UNKNOWN_ERROR));
			return false;
		}
		updateTransactionOrder(resultMap, custom, orderData, initialJsonString);
		handleStorePayment(currentPayment, response, customerJsonObject);
		sessionService.setAttribute("tid", orderComments);
		sessionService.setAttribute("email", transactionEmail);
		sessionService.setAttribute("novalnetOrderData", orderData);
		return true;
	}

	/**
	 * Builds the order comments using the payment name, transaction details, test mode information, and zero-amount booking information.
	 *
	 * @param currentPayment
	 *           the selected payment method
	 * @param transactionJsonObject
	 *           the transaction details from the Novalnet response
	 * @return the formatted order comments
	 */
	private String buildOrderComments(String currentPayment, JsonNode transactionJsonObject)
	{
		String paymentName = novalnetPaymentService.getPaymentName(currentPayment);
		String testMode = "";
		if ("1".equals(transactionJsonObject.path("test_mode").asText()))
		{
			testMode = " " + Localization.getLocalizedString("novalnet.testOrderText");
		}
		String orderComments = Localization.getLocalizedString("novalnet.paymentname") + ": " + paymentName + "<br>";
		orderComments += Localization.getLocalizedString("novalnet.transactionId") + " : "
				+ transactionJsonObject.path("tid").asText() + "<br>" + testMode;
		Boolean isZeroAmountBooking = sessionService.getAttribute(NOVALNET_ZERO_AMOUNT_BOOKING);
		if (Boolean.TRUE.equals(isZeroAmountBooking))
		{
			orderComments += "<br>" + Localization.getLocalizedString("novalnet.zeroAmountBooking");
		}
		sessionService.removeAttribute(NOVALNET_ZERO_AMOUNT_BOOKING);
		return orderComments;
	}

	/**
	 * Updates the Novalnet transaction with the SAP Commerce order number.
	 *
	 * @param resultMap
	 *           the transaction result data received from Novalnet
	 * @param custom
	 *           the custom transaction information
	 * @param orderData
	 *           the SAP Commerce order data
	 * @param fallbackJson
	 *           the fallback JSON request used when the update request cannot be serialized
	 */
	private void updateTransactionOrder(Map<String, String> resultMap, Custom custom, OrderData orderData, String fallbackJson)
	{
		Transaction updateTransaction = new Transaction();
		updateTransaction.setTid(resultMap.get("tid"));
		updateTransaction.setOrder_no(orderData.getCode());
		NovalnetPaymentRequest updateRequest = new NovalnetPaymentRequest();
		updateRequest.setTransaction(updateTransaction);
		updateRequest.setCustom(custom);
		String jsonString = fallbackJson;
		try
		{
			jsonString = MAPPER.writeValueAsString(updateRequest);
		}
		catch (JsonProcessingException e)
		{
			LOGGER.error("Error while converting Novalnet update request to JSON", e);
		}
		StringBuilder responseString = novalnetApiService.sendRequest(novalnetEndpointConfigService.getTransactionUpdateUrl(),
				jsonString);
		LOGGER.info("Novalnet response received: {}", responseString);
	}

	/**
	 * Handles a failed Novalnet transaction and stores the corresponding checkout error information.
	 *
	 * @param resultMap
	 *           the transaction result data received from Novalnet
	 * @param resultJsonObject
	 *           the result section of the Novalnet response
	 * @return {@code false} because the transaction was not successful
	 */
	private boolean handleFailedTransaction(Map<String, String> resultMap, JsonNode resultJsonObject)
	{
		sessionService.setAttribute("novalnetOrderCurrency", null);
		sessionService.setAttribute("novalnetOrderAmount", null);
		sessionService.setAttribute("novalnetCustomerParams", null);
		sessionService.setAttribute("novalnetRedirectPaymentTestModeValue", null);
		sessionService.setAttribute("novalnetRedirectPaymentName", null);
		sessionService.setAttribute("novalnetCreditCardPanHash", null);
		sessionService.setAttribute("paymentAccessKey", null);
		String statusMessage = !resultJsonObject.path("status_text").isMissingNode() ? resultJsonObject.path("status_text").asText()
				: resultMap.get("status_desc");
		sessionService.setAttribute(NOVALNET_CHECKOUT_ERROR, statusMessage);
		return false;
	}

	/**
	 * Handles storage of payment reference data for supported payment methods.
	 *
	 * @param currentPayment
	 *           the selected payment method
	 * @param response
	 *           the Novalnet transaction response
	 * @param customerJsonObject
	 *           the customer details from the Novalnet response
	 */
	public void handleStorePayment(String currentPayment, StringBuilder response, JsonNode customerJsonObject)
	{
		if (NOVALNET_CREDIT_CARD.equals(currentPayment) && !novalnetCheckoutService.isGuestUser())
		{
			handleCreditCardStorePayment(response, customerJsonObject);
			return;
		}
		String[] walletPayments =
		{ NOVALNET_GOOGLE_PAY, NOVALNET_APPLE_PAY };
		if (Arrays.asList(walletPayments).contains(currentPayment))
		{
			handleWalletStorePayment(currentPayment, response, customerJsonObject);
		}
	}

	/**
	 * Stores credit card reference transaction information when payment data storage is enabled.
	 *
	 * @param response
	 *           the Novalnet transaction response
	 * @param customerJsonObject
	 *           the customer details from the Novalnet response
	 */
	private void handleCreditCardStorePayment(StringBuilder response, JsonNode customerJsonObject)
	{
		boolean novalnetCreditCardStorePaymentData = sessionService.getAttribute(NOVALNET_CREDIT_CARD_STORE_PAYMENT_DATA);
		if (novalnetCreditCardStorePaymentData)
		{
			novalnetPaymentService.handleReferenceTransactionInfo(response, customerJsonObject.path("customer_no").asText(),
					NOVALNET_CREDIT_CARD);
		}
	}

	/**
	 * Stores wallet payment reference information when the wallet payment contains a valid token and payment data storage is enabled.
	 *
	 * @param currentPayment
	 *           the selected wallet payment method
	 * @param response
	 *           the Novalnet transaction response
	 * @param customerJsonObject
	 *           the customer details from the Novalnet response
	 */
	private void handleWalletStorePayment(String currentPayment, StringBuilder response, JsonNode customerJsonObject)
	{
		JSONObject responseJson = new JSONObject(response.toString());
		if (!responseJson.has(TRANSACTION))
		{
			return;
		}
		JSONObject transaction = responseJson.getJSONObject(TRANSACTION);
		if (!transaction.has(PAYMENT_DATA))
		{
			return;
		}
		JSONObject paymentData = transaction.getJSONObject(PAYMENT_DATA);
		if (!paymentData.has("token") || novalnetCheckoutService.isGuestUser())
		{
			return;
		}
		Boolean storePaymentData = sessionService.getAttribute(currentPayment + "StorePaymentData");
		if (Boolean.TRUE.equals(storePaymentData))
		{
			novalnetPaymentService.handleReferenceTransactionInfo(response, customerJsonObject.path("customer_no").asText(),
					currentPayment);
		}
	}
}
