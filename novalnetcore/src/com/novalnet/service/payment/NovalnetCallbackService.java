/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.novalnet.beans.NnCallbackRequestData;

import jakarta.servlet.http.HttpServletRequest;


/**
 * Defines operations for processing Novalnet callback requests and transaction events.
 */
public interface NovalnetCallbackService
{

	/**
	 * Processes an incoming Novalnet callback request.
	 *
	 * @param request the Novalnet callback request data
	 * @param httpRequest the HTTP servlet request containing callback request information
	 * @return the result of callback processing
	 */
	public String processCallback(NnCallbackRequestData request, HttpServletRequest httpRequest);

	/**
	 * Handles a transaction capture callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of transaction capture processing
	 */
	public String handleTransactionCapture(NnCallbackRequestData request);

	/**
	 * Handles a transaction cancellation callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of transaction cancellation processing
	 */
	public String handleTransactionCancel(NnCallbackRequestData request);

	/**
	 * Handles a transaction update callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of transaction update processing
	 */
	public String handleTransactionUpdate(NnCallbackRequestData request);

	/**
	 * Handles a payment callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of payment processing
	 */
	public String handlePayment(NnCallbackRequestData request);

	/**
	 * Handles a credit callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of credit processing
	 */
	public String handleCredit(NnCallbackRequestData request);

	/**
	 * Handles a refund callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of refund processing
	 */
	public String handleRefund(NnCallbackRequestData request);

	/**
	 * Handles a payment reminder callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of reminder processing
	 */
	public String handleReminder(NnCallbackRequestData request);

	/**
	 * Handles a collection callback from Novalnet.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of collection processing
	 */
	public String handleCollection(NnCallbackRequestData request);
}
