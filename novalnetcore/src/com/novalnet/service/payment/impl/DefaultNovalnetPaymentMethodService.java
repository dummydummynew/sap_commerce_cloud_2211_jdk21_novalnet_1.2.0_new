/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.novalnet.dto.AddressForm;
import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.PaymentConfigResult;
import com.novalnet.dto.payment.request.Billing;
import com.novalnet.dto.payment.request.Custom;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.dto.payment.request.HostedPage;
import com.novalnet.dto.payment.request.Merchant;
import com.novalnet.dto.payment.request.NovalnetPaymentRequest;
import com.novalnet.dto.payment.request.PaymentData;
import com.novalnet.dto.payment.request.Shipping;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.exception.NovalnetException;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentHandlerService;
import com.novalnet.service.payment.NovalnetPaymentMethodService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.util.NovalnetUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


/**
 * Default implementation of the Novalnet payment method service.
 *
 * <p>
 * Provides operations for preparing payment requests, processing payment transactions, handling wallet transactions, and validating
 * guarantee payments.
 * </p>
 */
public class DefaultNovalnetPaymentMethodService implements NovalnetPaymentMethodService
{
	public static final String REDIRECT_PREFIX = "redirect:";
	public static final int PREPAYMENT_FROM_DATE = 7;
	public static final int PREPAYMENT_TILL_DATE = 28;
	protected static final String REDIRECT_URL_ORDER_CONFIRMATION = REDIRECT_PREFIX + "/checkout/novalnet/orderConfirmation/";

	private static final String NOVALNET_VERSION = "1.2.0";
	private static final String NOVALNET_GOOGLE_PAY = "novalnetGooglePay";
	private static final String NOVALNET_APPLE_PAY = "novalnetApplePay";
	private static final String NOVALNET_ZERO_AMOUNT_BOOKING = "novalnetZeroAmountBooking";
	private static final String TRANSACTION = "transaction";
	private static final String TXN_SECRET = "txn_secret";
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNovalnetPaymentMethodService.class);

	@Resource
	private NovalnetApiService novalnetApiService;

	@Resource
	private NovalnetPaymentService novalnetPaymentService;

	@Resource
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Resource
	private I18NFacade i18NFacade;

	@Resource
	private SessionService sessionService;

	@Resource
	private PaymentModeService paymentModeService;

	@Resource
	private NovalnetPaymentHandlerService novalnetPaymentHandlerService;

	@Resource
	private NovalnetOneClickPaymentService novalnetOneClickPaymentService;

	@Resource
	private NovalnetTransactionResultService novalnetTransactionResultService;

	@Resource
	private NovalnetGuaranteeValidationService novalnetGuaranteeValidationService;

	/**
	 * Retrieves Novalnet merchant details using the product activation key.
	 *
	 * @param productActivationKey
	 *           the Novalnet product activation key
	 * @param baseStore
	 *           the current base store
	 * @return the Novalnet merchant details response
	 * @throws Exception
	 *            if an error occurs while creating the request or retrieving merchant details
	 */
	@Override
	public String callNovalnetMerchantDetails(String productActivationKey, BaseStoreModel baseStore) throws Exception
	{
		Map<String, Object> requestMap = new HashMap<>();
		Map<String, Object> merchantMap = new HashMap<>();
		merchantMap.put("signature", productActivationKey);
		Map<String, Object> customMap = new HashMap<>();
		customMap.put("lang", baseStore.getDefaultLanguage().getIsocode().toUpperCase());
		requestMap.put("merchant", merchantMap);
		requestMap.put("custom", customMap);
		String requestBody = MAPPER.writeValueAsString(requestMap);

		return novalnetApiService.fetchMerchantDetails(novalnetEndpointConfigService.getMerchantDetailsUrl(), requestBody,
				baseStore);
	}

	/**
	 * Adds the Novalnet payment process to the checkout model.
	 *
	 * @param model
	 *           the Spring MVC model
	 * @param paymentDetailsForm
	 *           the payment details submitted by the customer
	 * @param cartData
	 *           the current cart data
	 * @throws CMSItemNotFoundException
	 *            if a required CMS item cannot be found
	 */
	@Override
	public void addPaymentProcess(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData)
			throws CMSItemNotFoundException
	{
		novalnetPaymentService.addPaymentProcess(model, paymentDetailsForm, cartData);
	}

	/**
	 * Populates customer, billing, and shipping address details for the payment request.
	 *
	 * @param model
	 *           the Spring MVC model
	 * @param paymentDetailsForm
	 *           the payment details submitted by the customer
	 * @param cartData
	 *           the current cart data
	 * @param customer
	 *           the customer payment request data
	 * @param addressData
	 *           the address data to populate
	 */
	@Override
	public void populateCustomerAddressDetails(Model model, NovalnetPaymentDetailsForm paymentDetailsForm, CartData cartData,
			Customer customer, AddressData addressData)
	{
		AddressForm addressForm = paymentDetailsForm.getBillingAddress();
		Billing billing = new Billing();
		Shipping shipping = new Shipping();
		NovalnetPaymentRequest request = new NovalnetPaymentRequest();

		if (Boolean.TRUE.equals(paymentDetailsForm.isUseDeliveryAddress()))
		{
			billing.setFirst_name(addressData.getFirstName());
			billing.setLast_name(addressData.getLastName());
			billing.setStreet(addressData.getLine1() + addressData.getLine2());
			billing.setCity(addressData.getTown());
			billing.setZip(addressData.getPostalCode());

			if (addressData.getCountry() != null)
			{
				billing.setCountry_code(addressData.getCountry().getIsocode());
			}

			shipping.setSame_as_billing("1");
		}
		else
		{
			shipping.setFirst_name(cartData.getDeliveryAddress().getFirstName());
			shipping.setLast_name(cartData.getDeliveryAddress().getLastName());
			shipping.setStreet(cartData.getDeliveryAddress().getLine1() + cartData.getDeliveryAddress().getLine2());
			shipping.setCity(cartData.getDeliveryAddress().getTown());
			shipping.setZip(cartData.getDeliveryAddress().getPostalCode());

			if (cartData.getDeliveryAddress().getCountry() != null)
			{
				shipping.setCountry_code(cartData.getDeliveryAddress().getCountry().getIsocode());
			}

			if (addressForm != null)
			{
				addressData.setId(addressForm.getAddressId());
				addressData.setTitleCode(addressForm.getTitleCode());
				addressData.setFirstName(addressForm.getFirstName());
				addressData.setLastName(addressForm.getLastName());
				addressData.setLine1(addressForm.getLine1());
				addressData.setLine2(addressForm.getLine2());
				addressData.setTown(addressForm.getTownCity());
				addressData.setPostalCode(addressForm.getPostcode());

				if (addressForm.getCountryIso() != null)
				{
					addressData.setCountry(i18NFacade.getCountryForIsocode(addressForm.getCountryIso()));
				}

				if (addressForm.getRegionIso() != null)
				{
					addressData.setRegion(i18NFacade.getRegion(addressForm.getCountryIso(), addressForm.getRegionIso()));
				}

				addressData.setShippingAddress(Boolean.TRUE.equals(addressForm.getShippingAddress()));
				addressData.setBillingAddress(Boolean.TRUE.equals(addressForm.getBillingAddress()));
			}

			billing.setFirst_name(paymentDetailsForm.getBillTo_firstName());
			billing.setLast_name(paymentDetailsForm.getBillTo_lastName());
			billing.setStreet(paymentDetailsForm.getBillTo_street1() + paymentDetailsForm.getBillTo_street2());
			billing.setCity(paymentDetailsForm.getBillTo_city());
			billing.setZip(paymentDetailsForm.getBillTo_postalCode());
			billing.setCountry_code(paymentDetailsForm.getBillTo_country());

			addressData.setTitleCode(paymentDetailsForm.getBillTo_titleCode());
			addressData.setFirstName(paymentDetailsForm.getBillTo_firstName());
			addressData.setLastName(paymentDetailsForm.getBillTo_lastName());
			addressData.setLine1(paymentDetailsForm.getBillTo_street1());
			addressData.setLine2(paymentDetailsForm.getBillTo_street2());
			addressData.setTown(paymentDetailsForm.getBillTo_city());
			addressData.setPostalCode(paymentDetailsForm.getBillTo_postalCode());
		}

		customer.setFirst_name(addressData.getFirstName());
		customer.setLast_name(addressData.getLastName());
		customer.setBilling(billing);
		customer.setShipping(shipping);
		request.setCustomer(customer);

		sessionService.setAttribute("novalnetPaymentRequest", request);
	}

	/**
	 * Processes one-click payment token data for the selected payment method.
	 *
	 * @param currentPayment
	 *           the selected payment method
	 * @param paymentDetailsForm
	 *           the payment details submitted by the customer
	 * @param model
	 *           the Spring MVC model
	 * @param cartData
	 *           the current cart data
	 * @param deliveryAddress
	 *           the delivery address
	 * @return {@code true} if the token data is processed successfully; otherwise, {@code false}
	 * @throws CMSItemNotFoundException
	 *            if a required CMS item cannot be found
	 */
	@Override
	public boolean processOneClickTokenData(String currentPayment, NovalnetPaymentDetailsForm paymentDetailsForm, Model model,
			CartData cartData, AddressData deliveryAddress) throws CMSItemNotFoundException
	{
		return novalnetOneClickPaymentService.processOneClickTokenData(currentPayment, paymentDetailsForm, model, cartData,
				deliveryAddress);
	}

	/**
	 * Processes the Novalnet transaction result.
	 *
	 * @param resultMap
	 *           the transaction result data
	 * @return {@code true} if the transaction is processed successfully; otherwise, {@code false}
	 */
	@Override
	public boolean processTransaction(Map<String, String> resultMap)
	{
		return novalnetTransactionResultService.processTransaction(resultMap);
	}

	/**
	 * Handles storage of payment reference data for the selected payment method.
	 *
	 * @param currentPayment
	 *           the selected payment method
	 * @param response
	 *           the Novalnet transaction response
	 * @param customerJsonObject
	 *           the customer information from the response
	 */
	@Override
	public void handleStorePayment(String currentPayment, StringBuilder response, JsonNode customerJsonObject)
	{
		novalnetTransactionResultService.handleStorePayment(currentPayment, response, customerJsonObject);
	}

	/**
	 * Creates and sends a Novalnet transaction request.
	 *
	 * @param request
	 *           the current HTTP servlet request
	 * @param baseStore
	 *           the current base store
	 * @param currentPayment
	 *           the selected payment method
	 * @param customerNo
	 *           the customer number
	 * @param orderAmountCent
	 *           the order amount in cents
	 * @param cartData
	 *           the current cart data
	 * @return the Novalnet API response
	 */
	@Override
	public StringBuilder createTransaction(HttpServletRequest request, BaseStoreModel baseStore, String currentPayment,
			String customerNo, Integer orderAmountCent, CartData cartData)
	{
		HostedPage hostedPage = new HostedPage();
		Merchant merchant = new Merchant();
		Transaction transaction = new Transaction();
		PaymentData paymentData = new PaymentData();
		Custom custom = new Custom();

		NovalnetPaymentRequest paymentRequest = sessionService.getAttribute("novalnetPaymentRequest");

		if (paymentRequest == null || paymentRequest.getCustomer() == null)
		{
			throw new IllegalStateException("Customer data not found");
		}

		Customer customer = paymentRequest.getCustomer();
		Integer tariff = baseStore.getNovalnetTariffId();
		String apiKey = baseStore.getNovalnetAPIKey();
		String hybrisVersion = Config.getString("build.version", "unknown");
		String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);
		Integer sessionOrderAmountCent = sessionService.getAttribute("novalnetOrderAmount");

		merchant.setSignature(apiKey);
		merchant.setTariff(String.valueOf(tariff));

		populateTransactionCustomer(customer, paymentRequest, request, customerNo);
		populateTransactionBase(transaction, currentPayment, currency, sessionOrderAmountCent, hybrisVersion);

		custom.setLang(resolveLanguageCode());

		String paymentName = novalnetPaymentService.getPaymentName(currentPayment);
		custom.setInput1("paymentName");
		custom.setInputval1(paymentName);

		PaymentConfigResult configResult = novalnetPaymentHandlerService.handlePayment(currentPayment, paymentModeModel,
				transaction, paymentData, customer, sessionOrderAmountCent, request, hostedPage);

		boolean verifyPaymentData = configResult.isVerifyPaymentData();
		transaction.setTest_mode(String.valueOf(configResult.getTestMode()));

		applyRedirectSettings(transaction, paymentData, request, currentPayment, configResult.isRedirect());
		applyZeroAmountBookingFlag(transaction, configResult.isZeroAmountBooking());

		Map<String, Object> dataParameters = new HashMap<>();
		dataParameters.put("merchant", merchant);
		dataParameters.put("customer", customer);
		dataParameters.put(TRANSACTION, transaction);
		dataParameters.put("custom", custom);
		dataParameters.put("hosted_page", hostedPage);

		Gson gson = new GsonBuilder().create();
		String jsonString = gson.toJson(dataParameters);

		LOGGER.info("verify_payment_data = {}", verifyPaymentData);

		String url = resolveTransactionUrl(currentPayment, verifyPaymentData);
		StringBuilder response = novalnetApiService.sendRequest(url, jsonString);

		LOGGER.info("Novalnet API Response : {}", response);

		return response;
	}

	/**
	 * Resolves the language code from the current Hybris session.
	 *
	 * @return the uppercase language code, or {@code EN} if no locale is available
	 */
	private String resolveLanguageCode()
	{
		Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
		return language != null ? language.toString().toUpperCase(Locale.ENGLISH) : "EN";
	}

	/**
	 * Populates customer information required for the Novalnet transaction.
	 *
	 * @param customer
	 *           the customer request data to populate
	 * @param paymentRequest
	 *           the stored Novalnet payment request
	 * @param request
	 *           the current HTTP servlet request
	 * @param customerNo
	 *           the customer number
	 */
	private void populateTransactionCustomer(Customer customer, NovalnetPaymentRequest paymentRequest, HttpServletRequest request,
			String customerNo)
	{
		customer.setFirst_name(paymentRequest.getCustomer().getFirst_name());
		customer.setLast_name(paymentRequest.getCustomer().getLast_name());
		customer.setCustomer_ip(NovalnetUtils.getRemoteIpAddr(request));
		customer.setCustomer_no(customerNo);
		customer.setGender("u");
	}

	/**
	 * Populates the common transaction details required by Novalnet.
	 *
	 * @param transaction
	 *           the transaction request to populate
	 * @param currentPayment
	 *           the selected payment method
	 * @param currency
	 *           the transaction currency
	 * @param orderAmountCent
	 *           the order amount in cents
	 * @param hybrisVersion
	 *           the SAP Commerce version
	 */
	private void populateTransactionBase(Transaction transaction, String currentPayment, String currency, Integer orderAmountCent,
			String hybrisVersion)
	{
		transaction.setPayment_type(NovalnetUtils.getPaymentType(currentPayment));
		transaction.setCurrency(currency);
		transaction.setAmount(orderAmountCent.longValue());
		transaction.setSystem_name("SAP Commerce Cloud");
		transaction.setSystem_version(hybrisVersion + "-NN" + NOVALNET_VERSION);
	}

	/**
	 * Applies redirect URLs and payment data to the transaction request.
	 *
	 * @param transaction
	 *           the transaction request to update
	 * @param paymentData
	 *           the payment-specific data
	 * @param request
	 *           the current HTTP servlet request
	 * @param currentPayment
	 *           the selected payment method
	 * @param redirect
	 *           whether the payment requires a redirect
	 */
	private void applyRedirectSettings(Transaction transaction, PaymentData paymentData, HttpServletRequest request,
			String currentPayment, boolean redirect)
	{
		if (Boolean.TRUE.equals(redirect))
		{
			String currentUrl = request.getRequestURL().toString();
			String[] walletPayments =
			{ NOVALNET_GOOGLE_PAY };

			String returnUrl = currentUrl
					.replace(!Arrays.asList(walletPayments).contains(currentPayment) ? "novalnet/summary/placeOrder"
							: "novalnet/summary/bookWalletTransaction", "novalnet/hop-response");

			transaction.setReturn_url(returnUrl);
			transaction.setError_return_url(returnUrl);
		}

		transaction.setPayment_data(paymentData);
	}

	/**
	 * Applies the zero-amount booking configuration to the transaction and session.
	 *
	 * @param transaction
	 *           the transaction request to update
	 * @param zeroAmountBooking
	 *           whether zero-amount booking is enabled
	 */
	private void applyZeroAmountBookingFlag(Transaction transaction, boolean zeroAmountBooking)
	{
		if (Boolean.TRUE.equals(zeroAmountBooking))
		{
			LOGGER.info("Zero amount booking is enabled.");
			transaction.setAmount(0L);
			sessionService.setAttribute(NOVALNET_ZERO_AMOUNT_BOOKING, Boolean.TRUE);
		}
		else
		{
			sessionService.setAttribute(NOVALNET_ZERO_AMOUNT_BOOKING, Boolean.FALSE);
		}
	}

	/**
	 * Resolves the Novalnet transaction endpoint based on the payment method and payment verification requirement.
	 *
	 * @param currentPayment
	 *           the selected payment method
	 * @param verifyPaymentData
	 *           whether payment data verification is required
	 * @return the appropriate Novalnet transaction endpoint
	 */
	private String resolveTransactionUrl(String currentPayment, boolean verifyPaymentData)
	{
		if (NOVALNET_APPLE_PAY.equals(currentPayment))
		{
			return verifyPaymentData ? novalnetEndpointConfigService.getAuthorizeHostedUrl()
					: novalnetEndpointConfigService.getPaymentHostedUrl();
		}

		return verifyPaymentData ? novalnetEndpointConfigService.getAuthorizeUrl() : novalnetEndpointConfigService.getPaymentUrl();
	}

	/**
	 * Books a Google Pay wallet transaction through Novalnet.
	 *
	 * @param request
	 *           the current HTTP servlet request
	 * @param baseStore
	 *           the current base store
	 * @param customerNo
	 *           the customer number
	 * @param orderAmountCent
	 *           the order amount in cents
	 * @param cartData
	 *           the current cart data
	 * @return the Novalnet transaction response as a string
	 * @throws NovalnetException
	 *            if an error occurs while creating or processing the transaction
	 */
	@Override
	public String bookWalletTransaction(HttpServletRequest request, BaseStoreModel baseStore, String customerNo,
			Integer orderAmountCent, CartData cartData) throws NovalnetException
	{
		String currentPayment = NOVALNET_GOOGLE_PAY;
		StringBuilder response = createTransaction(request, baseStore, currentPayment, customerNo, orderAmountCent, cartData);

		LOGGER.info("Novalnet Response: {}", response);

		JSONObject tomJsonObject = new JSONObject(response.toString());
		JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");

		if (resultJsonObject.has("redirect_url"))
		{
			sessionService.setAttribute("txn_check", baseStore.getNovalnetPaymentAccessKey().trim());
		}

		if (tomJsonObject.has(TRANSACTION))
		{
			JSONObject transactionJsonObject = tomJsonObject.getJSONObject(TRANSACTION);

			if (transactionJsonObject.has(TXN_SECRET))
			{
				sessionService.setAttribute(TXN_SECRET, transactionJsonObject.get(TXN_SECRET).toString());
			}

			if (transactionJsonObject.has("tid"))
			{
				sessionService.setAttribute("wallet_tid", transactionJsonObject.get("tid").toString());
			}
		}

		return response.toString();
	}

	/**
	 * Handles guarantee payment validation for the selected payment method.
	 *
	 * @param paymentName
	 *           the selected payment method name
	 * @param dob
	 *           the customer's date of birth
	 * @param paymentDetailsForm
	 *           the payment details submitted by the customer
	 * @param deliveryAddress
	 *           the delivery address
	 * @return the guarantee validation result
	 */
	public String handleGuaranteeProcess(String paymentName, String dob, NovalnetPaymentDetailsForm paymentDetailsForm,
			AddressData deliveryAddress)
	{
		return novalnetGuaranteeValidationService.handleGuaranteeProcess(paymentName, dob, paymentDetailsForm, deliveryAddress);
	}
}
