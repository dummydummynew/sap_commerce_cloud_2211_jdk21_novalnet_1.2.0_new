
/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.facades;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.novalnet.dto.NovalnetTransactionResult;


/**
 * Defines operations for managing Novalnet transactions.
 */
public interface NovalnetTransactionFacade
{
	/**
	 * Cancels a Novalnet transaction for the given order.
	 *
	 * @param order
	 *           the order whose transaction should be cancelled
	 * @return the result of the cancellation
	 */
	public NovalnetTransactionResult cancelOrder(OrderModel order);

	/**
	 * Determines whether the transaction for the given order can be cancelled.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order can be cancelled; otherwise false
	 */
	public boolean canCancel(OrderModel order);

	/**
	 * Captures a Novalnet transaction for the given order.
	 *
	 * @param order
	 *           the order whose transaction should be captured
	 * @return the result of the capture operation
	 * @throws JsonProcessingException
	 *            if an error occurs while processing JSON data
	 */
	public NovalnetTransactionResult captureOrder(OrderModel order) throws JsonProcessingException;

	/**
	 * Captures the transaction for the given order.
	 *
	 * @param order
	 *           the order to capture
	 * @return the result of the capture operation
	 * @throws JsonProcessingException
	 *            if an error occurs while processing JSON data
	 */
	public boolean canCapture(OrderModel order);

	/**
	 * Determines whether a return request can be created for the given order.
	 *
	 * @param order
	 *           the order to check
	 * @return true if a return request can be created; otherwise false
	 */
	public boolean canCreateReturnRequest(OrderModel order);

	/**
	 * Determines whether the given order is returnable.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order is returnable; otherwise false
	 */
	public boolean isReturnable(OrderModel order);

	/**
	 * Determines whether the given order is refundable.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order is refundable; otherwise false
	 */
	public boolean isRefundable(OrderModel order);

	/**
	 * Determines whether the given order has been fully refunded.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order is fully refunded; otherwise false
	 */
	public boolean isFullyRefunded(OrderModel order);

	/**
	 * Refunds a return request through Novalnet.
	 *
	 * @param returnRequest
	 *           the return request to refund
	 * @return the result of the refund operation
	 */
	public NovalnetTransactionResult refund(ReturnRequestModel returnRequest);

	/**
	 * Processes the follow-on refund request.
	 *
	 * @param request
	 *           the follow-on refund request
	 * @return the result of the refund operation
	 * @throws JsonProcessingException
	 *            if an error occurs while processing JSON data
	 */
	public NovalnetTransactionResult processRefund(FollowOnRefundRequest request) throws JsonProcessingException;

	/**
	 * Books a Novalnet transaction for the given order.
	 *
	 * @param order
	 *           the order for which the transaction should be booked
	 * @throws InterceptorException
	 *            if an error occurs while booking the transaction
	 */
	public void bookTransaction(OrderModel order) throws InterceptorException;

}


