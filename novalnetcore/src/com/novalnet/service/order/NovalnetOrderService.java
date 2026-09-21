/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.order;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.store.BaseStoreModel;

import com.novalnet.model.NovalnetPaymentInfoModel;


/**
 * Defines operations for managing Novalnet order status and callback information.
 */
public interface NovalnetOrderService
{
	/**
	 * Updates the order status based on the provided Novalnet payment information.
	 *
	 * @param orderCode the unique code of the order
	 * @param paymentInfoModel the Novalnet payment information
	 */
	public void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfoModel);

	/**
	 * Updates the order status to cancelled.
	 *
	 * @param orderCode the unique code of the order
	 */
	public void updateCancelStatus(String orderCode);

	/**
	 * Updates the order status based on the callback payment method.
	 *
	 * @param orderCode the unique code of the order
	 * @param paymentMethod the payment method received in the callback
	 */
	public void updateCallbackOrderStatus(String orderCode, String paymentMethod);

	/**
	 * Updates the order status to indicate a partial payment.
	 *
	 * @param orderCode the unique code of the order
	 */
	public void updatePartPaidStatus(String orderCode);

	/**
	 * Retrieves an order using its order code.
	 *
	 * @param orderCode the unique code of the order
	 * @return the order matching the specified order code
	 */
	public OrderModel getOrder(String orderCode);

	/**
	 * Determines the order status based on the Novalnet payment information and base store.
	 *
	 * @param paymentInfoModel the Novalnet payment information
	 * @param baseStore the base store associated with the order
	 * @return the corresponding order status
	 */
	public OrderStatus getOrderStatus(NovalnetPaymentInfoModel paymentInfoModel, BaseStoreModel baseStore);

	/**
	 * Updates the order with callback comments and transaction status.
	 *
	 * @param comments the callback comments
	 * @param orderCode the unique code of the order
	 * @param transactionStatus the Novalnet transaction status
	 */
	public void updateCallbackComments(String comments, String orderCode, String transactionStatus);

	/**
	 * Updates callback comments for a guaranteed invoice transaction.
	 *
	 * @param callbackComments the callback comments received from Novalnet
	 * @param shortComment the short callback description
	 * @param orderNo the order number
	 * @param transactionStatus the Novalnet transaction status
	 */
	public void updateGuaranteedInvoiceCallbackComments(String callbackComments, String shortComment, String orderNo,
			String transactionStatus);

}
