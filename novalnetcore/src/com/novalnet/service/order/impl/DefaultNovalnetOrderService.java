/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.order.impl;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.model.NovalnetAliPayPaymentModeModel;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetBancontactPaymentModeModel;
import com.novalnet.model.NovalnetBlikPaymentModeModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetEpsPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedInvoicePaymentModeModel;
import com.novalnet.model.NovalnetIdealPaymentModeModel;
import com.novalnet.model.NovalnetInvoicePaymentModeModel;
import com.novalnet.model.NovalnetMbWayPaymentModeModel;
import com.novalnet.model.NovalnetMultibancoPaymentModeModel;
import com.novalnet.model.NovalnetOnlineBankTransferPaymentModeModel;
import com.novalnet.model.NovalnetPayPalPaymentModeModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPostFinanceCardPaymentModeModel;
import com.novalnet.model.NovalnetPostFinancePaymentModeModel;
import com.novalnet.model.NovalnetPrepaymentPaymentModeModel;
import com.novalnet.model.NovalnetPrzelewy24PaymentModeModel;
import com.novalnet.model.NovalnetTrustlyPaymentModeModel;
import com.novalnet.model.NovalnetTwintPaymentModeModel;
import com.novalnet.model.NovalnetWechatPayPaymentModeModel;
import com.novalnet.service.order.NovalnetOrderService;

import jakarta.annotation.Resource;


/**
 * Default implementation of {@link NovalnetOrderService} for managing Novalnet order
 * status and callback information.
 */
public class DefaultNovalnetOrderService implements NovalnetOrderService
{
	private static final String STATUS_ON_HOLD = "ON_HOLD";
	private static final String STATUS_PENDING = "PENDING";
	private static final String PAYMENT_NOVALNET_INVOICE = "novalnetInvoice";
	private static final String PAYMENT_NOVALNET_PREPAYMENT = "novalnetPrepayment";
	private static final String PAYMENT_NOVALNET_CREDIT_CARD = "novalnetCreditCard";
	private static final String PAYMENT_NOVALNET_DIRECT_DEBIT_SEPA = "novalnetDirectDebitSepa";
	private static final String PAYMENT_NOVALNET_GUARANTEED_DIRECT_DEBIT_SEPA = "novalnetGuaranteedDirectDebitSepa";
	private static final String PAYMENT_NOVALNET_GUARANTEED_INVOICE = "novalnetGuaranteedInvoice";
	private static final String PAYMENT_NOVALNET_MULTIBANCO = "novalnetMultibanco";
	private static final String PAYMENT_NOVALNET_PAYPAL = "novalnetPayPal";
	private static final String PAYMENT_NOVALNET_ONLINE_BANK_TRANSFER = "novalnetOnlineBankTransfer";
	private static final String PAYMENT_NOVALNET_BANCONTACT = "novalnetBancontact";
	private static final String PAYMENT_NOVALNET_POSTFINANCE_CARD = "novalnetPostFinanceCard";
	private static final String PAYMENT_NOVALNET_POSTFINANCE = "novalnetPostFinance";
	private static final String PAYMENT_NOVALNET_IDEAL = "novalnetIdeal";
	private static final String PAYMENT_NOVALNET_GOOGLE_PAY = "novalnetGooglePay";
	private static final String PAYMENT_NOVALNET_APPLE_PAY = "novalnetApplePay";
	private static final String PAYMENT_NOVALNET_TWINT = "novalnetTwint";
	private static final String PAYMENT_NOVALNET_MBWAY = "novalnetMbWay";
	private static final String PAYMENT_NOVALNET_TRUSTLY = "novalnetTrustly";
	private static final String PAYMENT_NOVALNET_BLIK = "novalnetBlik";
	private static final String PAYMENT_NOVALNET_WECHAT_PAY = "novalnetWechatPay";
	private static final String PAYMENT_NOVALNET_ALIPAY = "novalnetAlipay";
	private static final String PAYMENT_NOVALNET_EPS = "novalnetEps";
	private static final String PAYMENT_NOVALNET_PRZELEWY24 = "novalnetPrzelewy24";

	@Resource
	private ModelService modelService;

	@Resource
	private FlexibleSearchService flexibleSearchService;

	@Resource
	private BaseStoreService baseStoreService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource
	private NovalnetDao novalnetDao;

	/**
	 * Per-payment-method order status resolution logic, keyed by payment method code and looked up in
	 * {@link #getOrderStatus(NovalnetPaymentInfoModel, BaseStoreModel)}. Built once, since the resolution logic itself is stateless.
	 */
	private final Map<String, OrderStatusResolver> orderStatusResolvers = buildOrderStatusResolvers();

	/**
	 * Resolves an {@link OrderStatus} for a given payment mode and Novalnet gateway status.
	 */
	@FunctionalInterface
	private interface OrderStatusResolver
	{
		OrderStatus resolve(PaymentModeModel paymentModeModel, String gatewayStatus);
	}

	/**
	 * Updates the order status and payment status based on the Novalnet payment gateway status.
	 *
	 * @param orderCode
	 *           the code of the order to update
	 * @param paymentInfoModel
	 *           the Novalnet payment information containing the gateway status
	 */
	@Override
	public void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfoModel)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());
		BaseStoreModel baseStore = baseStoreService.getCurrentBaseStore();

		orderModel.setStatus(getOrderStatus(paymentInfoModel, baseStore));

		String paymentMethod = paymentInfoModel.getPaymentProvider();

		String[] bankPayments =
		{ PAYMENT_NOVALNET_INVOICE, PAYMENT_NOVALNET_PREPAYMENT };

		boolean isInvoicePrepayment = Arrays.asList(bankPayments).contains(paymentMethod);

		String[] pendingStatusCode =
		{ STATUS_ON_HOLD, STATUS_PENDING };

		if (isInvoicePrepayment || Arrays.asList(pendingStatusCode).contains(paymentInfoModel.getPaymentGatewayStatus()))
		{
			orderModel.setPaymentStatus(PaymentStatus.NOTPAID);
		}
		else
		{
			orderModel.setPaymentStatus(PaymentStatus.PAID);
		}

		modelService.save(orderModel);
	}

	/**
	 * Updates the specified order status to cancelled.
	 *
	 * @param orderCode the code of the order to cancel
	 */
	@Override
	public void updateCancelStatus(String orderCode)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		OrderStatus orderStatus = OrderStatus.CANCELLED;
		orderModel.setStatus(orderStatus);

		modelService.save(orderModel);
	}

	/**
	 * Updates the order status and payment status based on the callback payment method.
	 *
	 * @param orderCode the code of the order to update
	 * @param paymentMethod the Novalnet payment method code
	 */
	@Override
	public void updateCallbackOrderStatus(String orderCode, String paymentMethod)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(paymentMethod);

		if (PAYMENT_NOVALNET_INVOICE.equals(paymentMethod))
		{
			NovalnetInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetInvoicePaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetCallbackOrderStatus());
		}
		else if (PAYMENT_NOVALNET_MULTIBANCO.equals(paymentMethod))
		{
			NovalnetMultibancoPaymentModeModel novalnetPaymentMethod = (NovalnetMultibancoPaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetCallbackOrderStatus());
		}
		else if (PAYMENT_NOVALNET_PREPAYMENT.equals(paymentMethod))
		{
			NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod = (NovalnetPrepaymentPaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetCallbackOrderStatus());
		}
		else if (PAYMENT_NOVALNET_PAYPAL.equals(paymentMethod))
		{
			NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetOrderSuccessStatus());
		}
		else if (PAYMENT_NOVALNET_PRZELEWY24.equals(paymentMethod))
		{
			NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod = (NovalnetPrzelewy24PaymentModeModel) paymentModeModel;

			orderModel.setStatus(novalnetPaymentMethod.getNovalnetOrderSuccessStatus());
		}

		orderModel.setPaymentStatus(PaymentStatus.PAID);
		modelService.save(orderModel);
	}

	/**
	 * Updates the payment status of the specified order to partially paid.
	 *
	 * @param orderCode the code of the order to update
	 */
	@Override
	public void updatePartPaidStatus(String orderCode)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		orderModel.setPaymentStatus(PaymentStatus.PARTPAID);
		modelService.save(orderModel);
	}

	/**
	 * Determines the appropriate SAP Commerce order status based on the
	 * configured payment method and Novalnet gateway status.
	 *
	 * @param paymentInfoModel the Novalnet payment information
	 * @param baseStore the current base store
	 * @return the order status to apply to the order
	 */
	@Override
	public OrderStatus getOrderStatus(NovalnetPaymentInfoModel paymentInfoModel, BaseStoreModel baseStore)
	{
		String paymentMethod = paymentInfoModel.getPaymentProvider();
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(paymentMethod);
		String gatewayStatus = paymentInfoModel.getPaymentGatewayStatus();

		OrderStatusResolver resolver = orderStatusResolvers.get(paymentMethod);

		return resolver != null ? resolver.resolve(paymentModeModel, gatewayStatus) : OrderStatus.COMPLETED;
	}

	/**
	 * Builds the payment-method-code to {@link OrderStatusResolver} lookup used by
	 * {@link #getOrderStatus(NovalnetPaymentInfoModel, BaseStoreModel)}.
	 *
	 * @return the populated resolver map
	 */
	private Map<String, OrderStatusResolver> buildOrderStatusResolvers()
	{
		Map<String, OrderStatusResolver> resolvers = new HashMap<>();

		resolvers.put(PAYMENT_NOVALNET_CREDIT_CARD, (paymentModeModel, gatewayStatus) -> resolveWithOnHoldCheck(gatewayStatus,
				((NovalnetCreditCardPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_DIRECT_DEBIT_SEPA, (paymentModeModel, gatewayStatus) -> resolveWithOnHoldCheck(gatewayStatus,
				((NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_GUARANTEED_DIRECT_DEBIT_SEPA,
				(paymentModeModel, gatewayStatus) -> resolveWithPendingAndOnHoldCheck(gatewayStatus,
						((NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_INVOICE, (paymentModeModel, gatewayStatus) -> resolveWithOnHoldCheck(gatewayStatus,
				((NovalnetInvoicePaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_GUARANTEED_INVOICE,
				(paymentModeModel, gatewayStatus) -> resolveWithPendingAndOnHoldCheck(gatewayStatus,
						((NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_PREPAYMENT, (paymentModeModel,
				gatewayStatus) -> ((NovalnetPrepaymentPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_MULTIBANCO, (paymentModeModel,
				gatewayStatus) -> ((NovalnetMultibancoPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_PAYPAL, (paymentModeModel, gatewayStatus) -> resolveWithPendingAndOnHoldCheck(gatewayStatus,
				((NovalnetPayPalPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_ONLINE_BANK_TRANSFER, (paymentModeModel,
				gatewayStatus) -> ((NovalnetOnlineBankTransferPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_BANCONTACT, (paymentModeModel,
				gatewayStatus) -> ((NovalnetBancontactPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_POSTFINANCE_CARD, (paymentModeModel,
				gatewayStatus) -> ((NovalnetPostFinanceCardPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_POSTFINANCE, (paymentModeModel,
				gatewayStatus) -> ((NovalnetPostFinancePaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_IDEAL, (paymentModeModel,
				gatewayStatus) -> ((NovalnetIdealPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_GOOGLE_PAY, (paymentModeModel, gatewayStatus) -> resolveWithOnHoldCheck(gatewayStatus,
				((NovalnetGooglePayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_APPLE_PAY, (paymentModeModel, gatewayStatus) -> resolveWithOnHoldCheck(gatewayStatus,
				((NovalnetApplePayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		resolvers.put(PAYMENT_NOVALNET_TWINT, (paymentModeModel,
				gatewayStatus) -> ((NovalnetTwintPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_MBWAY, (paymentModeModel,
				gatewayStatus) -> ((NovalnetMbWayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_TRUSTLY, (paymentModeModel,
				gatewayStatus) -> ((NovalnetTrustlyPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_BLIK, (paymentModeModel, gatewayStatus) -> ((NovalnetBlikPaymentModeModel) paymentModeModel)
				.getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_WECHAT_PAY, (paymentModeModel,
				gatewayStatus) -> ((NovalnetWechatPayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_ALIPAY, (paymentModeModel,
				gatewayStatus) -> ((NovalnetAliPayPaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_EPS, (paymentModeModel, gatewayStatus) -> ((NovalnetEpsPaymentModeModel) paymentModeModel)
				.getNovalnetOrderSuccessStatus());

		resolvers.put(PAYMENT_NOVALNET_PRZELEWY24, (paymentModeModel, gatewayStatus) -> resolveWithPendingCheck(gatewayStatus,
				((NovalnetPrzelewy24PaymentModeModel) paymentModeModel).getNovalnetOrderSuccessStatus()));

		return resolvers;
	}

	/**
	 * Resolves the order status by checking whether the payment gateway
	 * status indicates that the transaction is on hold.
	 *
	 * @param gatewayStatus the Novalnet gateway status
	 * @param successStatus the configured success status
	 * @return PAYMENT_AUTHORIZED when the transaction is on hold; otherwise the success status
	 */
	private OrderStatus resolveWithOnHoldCheck(String gatewayStatus, OrderStatus successStatus)
	{
		if (STATUS_ON_HOLD.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_AUTHORIZED;
		}

		return successStatus;
	}

	/**
	 * Resolves the order status by checking pending and on-hold gateway statuses.
	 *
	 * @param gatewayStatus the Novalnet gateway status
	 * @param successStatus the configured success status
	 * @return PAYMENT_NOT_CAPTURED for pending status, PAYMENT_AUTHORIZED for on-hold status,
	 *         or the configured success status otherwise
	 */
	private OrderStatus resolveWithPendingAndOnHoldCheck(String gatewayStatus, OrderStatus successStatus)
	{
		if (STATUS_PENDING.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_NOT_CAPTURED;
		}

		if (STATUS_ON_HOLD.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_AUTHORIZED;
		}

		return successStatus;
	}

	/**
	 * Resolves the order status by checking whether the payment gateway
	 * status is pending.
	 *
	 * @param gatewayStatus the Novalnet gateway status
	 * @param successStatus the configured success status
	 * @return PAYMENT_NOT_CAPTURED when the transaction is pending; otherwise the success status
	 */
	private OrderStatus resolveWithPendingCheck(String gatewayStatus, OrderStatus successStatus)
	{
		if (STATUS_PENDING.equals(gatewayStatus))
		{
			return OrderStatus.PAYMENT_NOT_CAPTURED;
		}

		return successStatus;
	}

	/**
	 * Updates the payment information and order history with callback comments.
	 *
	 * @param comments the callback comments to add
	 * @param orderCode the code of the associated order
	 * @param transactionStatus the transaction status to store
	 */
	@Override
	public void updateCallbackComments(String comments, String orderCode, String transactionStatus)
	{
		List<NovalnetPaymentInfoModel> paymentInfo = novalnetDao.getNovalnetPaymentInfo(orderCode);
		NovalnetPaymentInfoModel paymentInfoModel = modelService.get(paymentInfo.get(0).getPk());

		String previousComments = paymentInfoModel.getOrderHistoryNotes();
		paymentInfoModel.setOrderHistoryNotes(previousComments + "<br><br>" + comments);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);

		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		OrderHistoryEntryModel orderEntry = modelService.create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(Timestamp.from(Instant.now()));
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription(comments);

		modelService.saveAll(paymentInfoModel, orderEntry);
	}

	/**
	 * Updates the payment information and order history with guaranteed invoice
	 * callback comments.
	 *
	 * @param callbackComments the callback comments to add to the payment information
	 * @param shortComment the short comment to add to the order history entry
	 * @param orderNo the order number associated with the callback
	 * @param transactionStatus the transaction status to store
	 */
	@Override
	public void updateGuaranteedInvoiceCallbackComments(String callbackComments, String shortComment, String orderNo,
			String transactionStatus)
	{
		List<NovalnetPaymentInfoModel> paymentInfo = novalnetDao.getNovalnetPaymentInfo(orderNo);
		NovalnetPaymentInfoModel paymentInfoModel = modelService.get(paymentInfo.get(0).getPk());

		String previousComments = paymentInfoModel.getOrderHistoryNotes();
		paymentInfoModel.setOrderHistoryNotes(previousComments + "<br><br>" + callbackComments);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);

		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderNo);
		OrderModel orderModel = modelService.get(orderInfoModel.get(0).getPk());

		OrderHistoryEntryModel orderEntry = modelService.create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(Timestamp.from(Instant.now()));
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription(shortComment);

		modelService.saveAll(paymentInfoModel, orderEntry);
	}

	/**
	 * Retrieves the order associated with the specified order code.
	 *
	 * @param orderCode the code of the order to retrieve
	 * @return the order model associated with the specified order code
	 */
	@Override
	public OrderModel getOrder(String orderCode)
	{
		List<OrderModel> orderInfoModel = novalnetDao.getOrderInfoModel(orderCode);
		return modelService.get(orderInfoModel.get(0).getPk());
	}
}

