/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.novalnet.service.checkout.impl;

import de.hybris.platform.acceleratorfacades.order.impl.DefaultAcceleratorCheckoutFacade;
import de.hybris.platform.commercefacades.consent.ConsentFacade;
import de.hybris.platform.commercefacades.consent.CustomerConsentDataStrategy;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.enums.CustomerType;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.session.SessionService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.novalnet.model.NovalnetAliPayPaymentModeModel;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetBancontactPaymentModeModel;
import com.novalnet.model.NovalnetBlikPaymentModeModel;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitAchPaymentModeModel;
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
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.service.payment.NovalnetPaymentService;

import jakarta.annotation.Resource;


/**
 * Default implementation of {@link NovalnetCheckoutService} for managing Novalnet checkout data and orders.
 */
@SuppressWarnings("java:S1200")
public class DefaultNovalnetCheckoutService extends DefaultAcceleratorCheckoutFacade implements NovalnetCheckoutService
{
	public static final String REDIRECT_PREFIX = "redirect:";
	public static final String ROOT = "/";
	private static final Pattern BR_TAG_PATTERN = Pattern.compile("<br\\s*/?>");
	private static final Logger LOGGER = Logger.getLogger(DefaultNovalnetCheckoutService.class);

	private static final Map<String, Class<? extends PaymentModeModel>> PAYMENT_MODE_TYPES_BY_CODE = Map.ofEntries(
			Map.entry("novalnetCreditCard", NovalnetCreditCardPaymentModeModel.class),
			Map.entry("novalnetDirectDebitSepa", NovalnetDirectDebitSepaPaymentModeModel.class),
			Map.entry("novalnetDirectDebitAch", NovalnetDirectDebitAchPaymentModeModel.class),
			Map.entry("novalnetGuaranteedInvoice", NovalnetGuaranteedInvoicePaymentModeModel.class),
			Map.entry("novalnetGuaranteedDirectDebitSepa", NovalnetGuaranteedDirectDebitSepaPaymentModeModel.class),
			Map.entry("novalnetPayPal", NovalnetPayPalPaymentModeModel.class),
			Map.entry("novalnetInvoice", NovalnetInvoicePaymentModeModel.class),
			Map.entry("novalnetPrepayment", NovalnetPrepaymentPaymentModeModel.class),
			Map.entry("novalnetOnlineBankTransfer", NovalnetOnlineBankTransferPaymentModeModel.class),
			Map.entry("novalnetMultibanco", NovalnetMultibancoPaymentModeModel.class),
			Map.entry("novalnetBancontact", NovalnetBancontactPaymentModeModel.class),
			Map.entry("novalnetPostFinanceCard", NovalnetPostFinanceCardPaymentModeModel.class),
			Map.entry("novalnetPostFinance", NovalnetPostFinancePaymentModeModel.class),
			Map.entry("novalnetIdeal", NovalnetIdealPaymentModeModel.class),
			Map.entry("novalnetTwint", NovalnetTwintPaymentModeModel.class),
			Map.entry("novalnetMbWay", NovalnetMbWayPaymentModeModel.class),
			Map.entry("novalnetTrustly", NovalnetTrustlyPaymentModeModel.class),
			Map.entry("novalnetBlik", NovalnetBlikPaymentModeModel.class),
			Map.entry("novalnetWechatPay", NovalnetWechatPayPaymentModeModel.class),
			Map.entry("novalnetAlipay", NovalnetAliPayPaymentModeModel.class),
			Map.entry("novalnetGooglePay", NovalnetGooglePayPaymentModeModel.class),
			Map.entry("novalnetApplePay", NovalnetApplePayPaymentModeModel.class),
			Map.entry("novalnetEps", NovalnetEpsPaymentModeModel.class),
			Map.entry("novalnetPrzelewy24", NovalnetPrzelewy24PaymentModeModel.class));

	@Resource
	private Converter<AddressData, AddressModel> addressReverseConverter;

	@Resource
	private NovalnetPaymentService novalnetPaymentService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource
	private NovalnetOrderService novalnetOrderService;

	@Resource
	private SessionService sessionService;

	@Resource(name = "consentFacade")
	protected ConsentFacade consentFacade;

	@Resource(name = "customerFacade")
	private CustomerFacade customerFacade;

	@Resource(name = "customerConsentDataStrategy")
	protected CustomerConsentDataStrategy customerConsentDataStrategy;

	@Resource(name = "orderFacade")
	private OrderFacade orderFacade;

	@Resource(name = "productFacade")
	private ProductFacade productFacade;

	/**
	 * Sets the cart service used by the checkout facade.
	 *
	 * @param cartService
	 *           the cart service
	 */
	@Override
	public void setCartService(final CartService cartService)
	{
		super.setCartService(cartService);
		this.cartService = cartService;
	}

	/**
	 * Assigns the configured payment mode to the order.
	 *
	 * @param currentPayment
	 *           the selected payment method
	 * @param paymentModeModel
	 *           the payment mode model
	 * @param orderModel
	 *           the order model
	 */
	private void assignPaymentMode(String currentPayment, PaymentModeModel paymentModeModel, OrderModel orderModel)
	{
		Class<? extends PaymentModeModel> expectedType = PAYMENT_MODE_TYPES_BY_CODE.get(currentPayment);

		if (expectedType == null || paymentModeModel == null)
		{
			return;
		}

		if (!expectedType.isInstance(paymentModeModel))
		{
			throw new IllegalStateException(
					"Configured payment mode for code " + currentPayment + " is not of expected type " + expectedType);
		}

		LOGGER.info("Payment mode set on order for code: " + currentPayment);
		orderModel.setPaymentMode(paymentModeModel);
	}

	/**
	 * Removes HTML line break tags from the specified order comments.
	 *
	 * @param orderComments
	 *           the order comments to sanitize
	 * @return the comments with HTML line break tags replaced by spaces
	 */
	private String sanitizeOrderComments(String orderComments)
	{
		return BR_TAG_PATTERN.matcher(orderComments).replaceAll(" ");
	}

	/**
	 * Creates and assigns the payment transaction for the current cart.
	 *
	 * @param transactionID
	 *           the Novalnet transaction ID
	 * @param cartModel
	 *           the current cart
	 * @param orderAmountCent
	 *           the order amount in cents
	 * @param backendTransactionComments
	 *           the backend transaction comments
	 * @param currency
	 *           the order currency
	 * @param currentPayment
	 *           the selected payment method
	 * @param paymentInfoModel
	 *           the Novalnet payment information
	 */
	private void createPaymentTransaction(String transactionID, CartModel cartModel, int orderAmountCent,
			String backendTransactionComments, String currency, String currentPayment, NovalnetPaymentInfoModel paymentInfoModel)
	{
		List<PaymentTransactionEntryModel> paymentTransactionEntries = new ArrayList<>();

		PaymentTransactionEntryModel orderTransactionEntry = novalnetPaymentService.createTransactionEntry(transactionID, cartModel,
				orderAmountCent, backendTransactionComments, currency);

		paymentTransactionEntries.add(orderTransactionEntry);

		PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
		paymentTransactionModel.setPaymentProvider(currentPayment);
		paymentTransactionModel.setRequestId(transactionID);
		paymentTransactionModel.setEntries(paymentTransactionEntries);
		paymentTransactionModel.setOrder(cartModel);
		paymentTransactionModel.setInfo(paymentInfoModel);

		cartModel.setPaymentTransactions(Arrays.asList(paymentTransactionModel));
	}

	/**
	 * Saves order data for the Novalnet checkout process.
	 *
	 * @param orderComments
	 *           the comments associated with the order
	 * @param currentPayment
	 *           the selected payment method
	 * @param transactionStatus
	 *           the transaction status
	 * @param orderAmountCent
	 *           the order amount in cents
	 * @param currency
	 *           the order currency
	 * @param transactionID
	 *           the Novalnet transaction ID
	 * @param email
	 *           the customer's email address
	 * @param addressData
	 *           the customer's address data
	 * @param bankDetails
	 *           the bank details associated with the transaction
	 * @return the saved order data
	 * @throws InvalidCartException
	 *            if the cart is invalid
	 */
	@Override
	public OrderData saveOrderData(String orderComments, String currentPayment, String transactionStatus, int orderAmountCent,
			String currency, String transactionID, String email, AddressData addressData, String bankDetails)
			throws InvalidCartException
	{
		CartModel cartModel = cartService.getSessionCart();
		UserModel currentUser = getCurrentUserForCheckout();
		String backendTransactionComments = sanitizeOrderComments(orderComments);

		AddressModel billingAddress = getModelService().create(AddressModel.class);
		billingAddress = addressReverseConverter.convert(addressData, billingAddress);
		billingAddress.setEmail(email);
		billingAddress.setOwner(cartModel);

		NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
		paymentInfoModel.setBillingAddress(billingAddress);
		paymentInfoModel.setPaymentEmailAddress(email);
		paymentInfoModel.setDuplicate(Boolean.FALSE);
		paymentInfoModel.setSaved(Boolean.TRUE);
		paymentInfoModel.setUser(currentUser);
		paymentInfoModel.setPaymentInfo(orderComments);
		paymentInfoModel.setOrderHistoryNotes(bankDetails);
		paymentInfoModel.setPaymentProvider(currentPayment);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);

		cartModel.setPaymentInfo(paymentInfoModel);
		paymentInfoModel.setCode("");

		createPaymentTransaction(transactionID, cartModel, orderAmountCent, backendTransactionComments, currency, currentPayment,
				paymentInfoModel);

		beforePlaceOrder(cartModel);

		OrderModel orderModel = placeOrder(cartModel);
		String orderNumber = orderModel.getCode();

		novalnetOrderService.updateOrderStatus(orderNumber, paymentInfoModel);

		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);
		assignPaymentMode(currentPayment, paymentModeModel, orderModel);

		paymentInfoModel.setPaymentInfo(orderComments);
		paymentInfoModel.setPaymentProvider(currentPayment);
		paymentInfoModel.setPaymentGatewayStatus(transactionStatus);
		paymentInfoModel.setOrderHistoryNotes(bankDetails);

		orderModel.setStatusInfo(backendTransactionComments);
		paymentInfoModel.setCode(orderNumber);

		getModelService().saveAll(paymentInfoModel, cartModel, billingAddress);

		OrderHistoryEntryModel orderEntry = getModelService().create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(Timestamp.from(Instant.now()));
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription(backendTransactionComments);

		orderModel.setPaymentInfo(paymentInfoModel);

		int orderPaidAmount;
		String[] bankPayments =
		{ "novalnetInvoice", "novalnetPrepayment", "novalnetGuaranteedDirectDebitSepa", "novalnetGuaranteedInvoice" };

		boolean isInvoicePrepayment = Arrays.asList(bankPayments).contains(currentPayment);

		String[] pendingStatusCode =
		{ "PENDING" };

		if (isInvoicePrepayment || Arrays.asList(pendingStatusCode).contains(transactionStatus))
		{
			orderPaidAmount = 0;
		}
		else
		{
			orderPaidAmount = orderAmountCent;
		}

		orderModel.setPaidAmount(orderPaidAmount);

		getModelService().saveAll(orderModel, orderEntry);

		afterPlaceOrder(cartModel, orderModel);

		long callbackInfoTid = Long.parseLong(transactionID);

		NovalnetCallbackInfoModel novalnetCallbackInfo = new NovalnetCallbackInfoModel();
		novalnetCallbackInfo.setPaymentType(currentPayment);
		novalnetCallbackInfo.setOrderAmount(orderAmountCent);
		novalnetCallbackInfo.setCallbackTid(callbackInfoTid);
		novalnetCallbackInfo.setOrginalTid(callbackInfoTid);
		novalnetCallbackInfo.setPaidAmount(orderPaidAmount);
		novalnetCallbackInfo.setOrderNo(orderNumber);

		getModelService().save(novalnetCallbackInfo);

		return getOrderConverter().convert(orderModel);
	}

	/**
	 * Saves the billing address and cart data.
	 *
	 * @param billingAddress
	 *           the billing address to save
	 * @param cartModel
	 *           the cart model to save
	 */
	@Override
	public void saveData(AddressModel billingAddress, CartModel cartModel)
	{
		getModelService().saveAll(billingAddress, cartModel);
	}

	/**
	 * Creates and returns a billing address model.
	 *
	 * @return a new billing address model
	 */
	@Override
	public AddressModel getBillingAddress()
	{
		return getModelService().create(AddressModel.class);
	}

	/**
	 * Determines whether the current checkout user is a guest customer.
	 *
	 * @return {@code true} if the current user is a guest customer; otherwise {@code false}
	 */
	@Override
	public boolean isGuestUser()
	{
		CartModel cart = cartService.getSessionCart();
		UserModel user = cart.getUser();

		return user instanceof CustomerModel && ((CustomerModel) user).getType() == CustomerType.GUEST;
	}

	/**
	 * Retrieves the email address of the current guest user.
	 *
	 * @return the guest user's email address, or {@code null} if the current user is not a guest
	 */
	@Override
	public String getGuestEmail()
	{
		CartModel cart = cartService.getSessionCart();
		UserModel user = cart.getUser();

		return user instanceof CustomerModel && ((CustomerModel) user).getType() == CustomerType.GUEST
				? user.getUid().substring(user.getUid().indexOf('|') + 1)
				: null;
	}

	/**
	 * Retrieves the current Novalnet checkout cart.
	 *
	 * @return the current checkout cart
	 */
	@Override
	public CartModel getNovalnetCheckoutCart()
	{
		return getCart();
	}

	/**
	 * Retrieves the current user for the checkout process.
	 *
	 * @return the current checkout user
	 */
	@Override
	public UserModel getCurrentUser()
	{
		return getCurrentUserForCheckout();
	}
}
