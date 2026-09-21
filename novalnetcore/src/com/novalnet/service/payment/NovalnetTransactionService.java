/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.returns.model.ReturnRequestModel;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.dto.OrderPaymentCommentsData;


/**
 * Defines operations for managing Novalnet transactions, cancellations, captures, and refunds.
 */
public interface NovalnetTransactionService
{
	/**
	 * Builds order and payment comments from the transaction response and cart information.
	 *
	 * @param currentPayment
	 *           the currently selected payment method
	 * @param transactionJsonObject
	 *           the Novalnet transaction response
	 * @param cartData
	 *           the current cart data
	 * @return the order and payment comments data
	 */
	public OrderPaymentCommentsData buildOrderAndPaymentComments(String currentPayment, JSONObject transactionJsonObject,
			CartData cartData);

	/**
	 * Cancels the specified order transaction.
	 *
	 * @param order
	 *           the order to cancel
	 * @return the result of the cancellation operation
	 */
	public NovalnetTransactionResult cancelOrder(OrderModel order);

	/**
	 * Determines whether the specified order can be cancelled.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order can be cancelled; otherwise false
	 */
	public boolean canCancel(OrderModel order);

	/**
	 * Captures the transaction associated with the specified order.
	 *
	 * @param order
	 *           the order for which the transaction should be captured
	 * @return the result of the capture operation
	 * @throws JsonProcessingException
	 *            if an error occurs while processing JSON
	 */
	public NovalnetTransactionResult captureOrder(OrderModel order) throws JsonProcessingException;

	/**
	 * Determines whether the specified order can be captured.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order can be captured; otherwise false
	 */
	public boolean canCapture(OrderModel order);

	/**
	 * Determines whether a return request can be created for the specified order.
	 *
	 * @param order
	 *           the order to check
	 * @return true if a return request can be created; otherwise false
	 */
	public boolean canCreateReturnRequest(OrderModel order);

	/**
	 * Determines whether the specified order is eligible for return.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order is returnable; otherwise false
	 */
	public boolean isReturnable(OrderModel order);

	/**
	 * Determines whether the specified order is eligible for a refund.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order is refundable; otherwise false
	 */
	public boolean isRefundable(OrderModel order);

	/**
	 * Determines whether the specified order has been fully refunded.
	 *
	 * @param order
	 *           the order to check
	 * @return true if the order has been fully refunded; otherwise false
	 */
	public boolean isFullyRefunded(OrderModel order);

	/**
	 * Processes a refund for the specified return request.
	 *
	 * @param returnRequest
	 *           the return request containing the refund information
	 * @return the result of the refund operation
	 */
	public NovalnetTransactionResult refund(ReturnRequestModel returnRequest);

	/**
	 * Processes a follow-on refund request.
	 *
	 * @param request
	 *           the follow-on refund request
	 * @return the result of the refund operation
	 * @throws JsonProcessingException
	 *            if an error occurs while processing JSON
	 */
	public NovalnetTransactionResult processRefund(FollowOnRefundRequest request) throws JsonProcessingException;

}
