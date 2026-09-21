/* 
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved. 
 */ 
package com.novalnet.facades; 
 
import de.novalnet.beans.NnCallbackRequestData; 
 
import jakarta.servlet.http.HttpServletRequest; 
 
 
/** 
 * Defines operations for processing Novalnet callback requests.
 */ 
public interface NovalnetCallbackFacade
{ 
 
	/**
	 * Processes an incoming Novalnet callback request.
	 *
	 * @param request the Novalnet callback request data
	 * @param httpRequest the HTTP servlet request
	 * @return the result of processing the callback
	 */
	public String processCallback(NnCallbackRequestData request, HttpServletRequest httpRequest); 
 
	/**
	 * Handles a transaction capture callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleTransactionCapture(NnCallbackRequestData request); 
 
	/**
	 * Handles a transaction cancellation callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleTransactionCancel(NnCallbackRequestData request); 
 
	/**
	 * Handles a transaction update callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleTransactionUpdate(NnCallbackRequestData request); 
 
	/**
	 * Handles a payment callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handlePayment(NnCallbackRequestData request); 
 
	/**
	 * Handles a credit callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleCredit(NnCallbackRequestData request); 
 
	/**
	 * Handles a refund callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleRefund(NnCallbackRequestData request); 
 
	/**
	 * Handles a reminder callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleReminder(NnCallbackRequestData request); 
 
	/**
	 * Handles a collection callback.
	 *
	 * @param request the Novalnet callback request data
	 * @return the result of processing the callback
	 */
	public String handleCollection(NnCallbackRequestData request); 
}


