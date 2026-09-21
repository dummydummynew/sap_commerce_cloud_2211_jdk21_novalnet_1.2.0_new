/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.RegionData;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.Config;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;

import com.fasterxml.jackson.databind.JsonNode;
import com.novalnet.dto.AddressForm;
import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.PaymentConfigResult;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.dto.payment.request.NovalnetPaymentRequest;
import com.novalnet.dto.payment.request.PaymentData;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.exception.NovalnetException;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentHandlerService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.util.NovalnetUtils;

import jakarta.servlet.http.HttpServletRequest;


@UnitTest
@ExtendWith(MockitoExtension.class)
class DefaultNovalnetPaymentMethodServiceTest
{
	private static final String GOOGLE_PAY = "novalnetGooglePay";
	private static final String APPLE_PAY = "novalnetApplePay";
	private static final String CREDIT_CARD = "novalnetCreditCard";

	private static final String ACTIVATION_KEY = "activation-key-123";
	private static final String DEFAULT_LANGUAGE_ISO = "en";

	private static final String MERCHANT_DETAILS_URL = "https://payport.novalnet.de/v2/merchant/details";

	private static final String MERCHANT_DETAILS_RESPONSE = "{\"result\":{\"status\":\"SUCCESS\"}}";

	private static final String CUSTOMER_NO = "CUST-001";

	private static final int ORDER_AMOUNT_CENT = 1000;

	private static final String CURRENCY_ISO = "EUR";

	private static final String API_KEY = "api-key-abc";

	private static final int TARIFF_ID = 12345;

	private static final String PAYMENT_NAME = "Credit Card";

	private static final String CUSTOMER_IP = "127.0.0.1";

	private static final String CURRENT_URL = "https://shop.example.com/checkout/multi/novalnet/summary/placeOrder";

	private static final String AUTHORIZE_URL = "https://payport.novalnet.de/v2/authorize";

	private static final String PAYMENT_URL = "https://payport.novalnet.de/v2/payment";

	private static final String AUTHORIZE_HOSTED_URL = "https://payport.novalnet.de/v2/authorize/hosted";

	private static final String NOVALNET_ACCESS_KEY = "access-key-xyz";

	private static final String TXN_SECRET_VALUE = "secret-abc";

	private static final String WALLET_TID = "17071700000000001";

	private static final String STREET_1 = "Main Street 1";

	private static final String STREET_2 = "Apt 4";

	private static final String CITY = "Munich";

	private static final String POSTAL_CODE = "12345";

	private static final String FIRST_NAME = "John";

	private static final String LAST_NAME = "Doe";

	private static final String COUNTRY_ISO = "DE";

	private DefaultNovalnetPaymentMethodService service;

	@Mock
	private NovalnetApiService novalnetApiService;

	@Mock
	private NovalnetPaymentService novalnetPaymentService;

	@Mock
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Mock
	private I18NFacade i18NFacade;

	@Mock
	private SessionService sessionService;

	@Mock
	private PaymentModeService paymentModeService;

	@Mock
	private NovalnetPaymentHandlerService novalnetPaymentHandlerService;

	@Mock
	private NovalnetOneClickPaymentService novalnetOneClickPaymentService;

	@Mock
	private NovalnetTransactionResultService novalnetTransactionResultService;

	@Mock
	private NovalnetGuaranteeValidationService novalnetGuaranteeValidationService;

	@Mock
	private BaseStoreModel baseStore;

	@Mock
	private LanguageModel defaultLanguage;

	@Mock
	private Model model;

	@Mock
	private NovalnetPaymentDetailsForm paymentDetailsForm;

	@Mock
	private AddressForm addressForm;

	@Mock
	private CartData cartData;

	@Mock
	private PriceData totalPriceWithTax;

	@Mock
	private PaymentModeModel paymentModeModel;

	@Mock
	private PaymentConfigResult paymentConfigResult;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private JaloSession jaloSession;

	@Mock
	private SessionContext sessionContext;

	private MockedStatic<JaloSession> jaloSessionMock;

	private MockedStatic<Config> configMock;

	private MockedStatic<NovalnetUtils> novalnetUtilsMock;

	@BeforeEach
	void setUp()
	{
		service = new DefaultNovalnetPaymentMethodService();

		ReflectionTestUtils.setField(service, "novalnetApiService", novalnetApiService);

		ReflectionTestUtils.setField(service, "novalnetPaymentService", novalnetPaymentService);

		ReflectionTestUtils.setField(service, "novalnetEndpointConfigService", novalnetEndpointConfigService);

		ReflectionTestUtils.setField(service, "i18NFacade", i18NFacade);

		ReflectionTestUtils.setField(service, "sessionService", sessionService);

		ReflectionTestUtils.setField(service, "paymentModeService", paymentModeService);

		ReflectionTestUtils.setField(service, "novalnetPaymentHandlerService", novalnetPaymentHandlerService);

		ReflectionTestUtils.setField(service, "novalnetOneClickPaymentService", novalnetOneClickPaymentService);

		ReflectionTestUtils.setField(service, "novalnetTransactionResultService", novalnetTransactionResultService);

		ReflectionTestUtils.setField(service, "novalnetGuaranteeValidationService", novalnetGuaranteeValidationService);

		jaloSessionMock = mockStatic(JaloSession.class);

		lenient().when(JaloSession.getCurrentSession()).thenReturn(jaloSession);

		lenient().when(jaloSession.getSessionContext()).thenReturn(sessionContext);

		lenient().when(sessionContext.getLocale()).thenReturn(Locale.US);

		configMock = mockStatic(Config.class);

		lenient().when(Config.getString("build.version", "unknown")).thenReturn("2211.30");

		novalnetUtilsMock = mockStatic(NovalnetUtils.class);
	}

	@AfterEach
	void tearDown()
	{
		jaloSessionMock.close();
		configMock.close();
		novalnetUtilsMock.close();
	}

	private AddressData givenRealAddressData()
	{
		AddressData addressData = new AddressData();

		addressData.setFirstName(FIRST_NAME);
		addressData.setLastName(LAST_NAME);
		addressData.setLine1(STREET_1);
		addressData.setLine2(STREET_2);
		addressData.setTown(CITY);
		addressData.setPostalCode(POSTAL_CODE);

		return addressData;
	}

	private CountryData givenCountryData(String isocode)
	{
		CountryData countryData = new CountryData();

		countryData.setIsocode(isocode);

		return countryData;
	}

	private NovalnetPaymentRequest givenStoredPaymentRequest()
	{
		Customer storedCustomer = new Customer();

		storedCustomer.setFirst_name(FIRST_NAME);
		storedCustomer.setLast_name(LAST_NAME);

		NovalnetPaymentRequest paymentRequest = new NovalnetPaymentRequest();

		paymentRequest.setCustomer(storedCustomer);

		return paymentRequest;
	}

	private void givenCreateTransactionCommonStubs(String currentPayment)
	{
		when(sessionService.<NovalnetPaymentRequest> getAttribute("novalnetPaymentRequest"))
				.thenReturn(givenStoredPaymentRequest());

		when(baseStore.getNovalnetTariffId()).thenReturn(TARIFF_ID);

		when(baseStore.getNovalnetAPIKey()).thenReturn(API_KEY);

		when(totalPriceWithTax.getCurrencyIso()).thenReturn(CURRENCY_ISO);

		when(cartData.getTotalPriceWithTax()).thenReturn(totalPriceWithTax);

		when(paymentModeService.getPaymentModeForCode(currentPayment)).thenReturn(paymentModeModel);

		when(sessionService.<Integer> getAttribute("novalnetOrderAmount")).thenReturn(ORDER_AMOUNT_CENT);

		novalnetUtilsMock.when(() -> NovalnetUtils.getRemoteIpAddr(httpServletRequest)).thenReturn(CUSTOMER_IP);

		novalnetUtilsMock.when(() -> NovalnetUtils.getPaymentType(currentPayment)).thenReturn("CREDITCARD");

		when(novalnetPaymentService.getPaymentName(currentPayment)).thenReturn(PAYMENT_NAME);

		when(novalnetPaymentHandlerService.handlePayment(eq(currentPayment), eq(paymentModeModel), any(Transaction.class),
				any(PaymentData.class), any(Customer.class), eq(ORDER_AMOUNT_CENT), eq(httpServletRequest), any()))
						.thenReturn(paymentConfigResult);

		when(paymentConfigResult.getTestMode()).thenReturn(0);
	}


	@Test
	void shouldReturnMerchantDetailsResponseWhenApiCallSucceeds() throws Exception
	{
		when(baseStore.getDefaultLanguage()).thenReturn(defaultLanguage);

		when(defaultLanguage.getIsocode()).thenReturn(DEFAULT_LANGUAGE_ISO);

		when(novalnetEndpointConfigService.getMerchantDetailsUrl()).thenReturn(MERCHANT_DETAILS_URL);

		when(novalnetApiService.fetchMerchantDetails(eq(MERCHANT_DETAILS_URL), anyString(), eq(baseStore)))
				.thenReturn(MERCHANT_DETAILS_RESPONSE);

		String result = service.callNovalnetMerchantDetails(ACTIVATION_KEY, baseStore);

		assertThat(result).isEqualTo(MERCHANT_DETAILS_RESPONSE);

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

		verify(novalnetApiService).fetchMerchantDetails(eq(MERCHANT_DETAILS_URL), bodyCaptor.capture(), eq(baseStore));

		assertThat(bodyCaptor.getValue()).contains(ACTIVATION_KEY).contains(DEFAULT_LANGUAGE_ISO.toUpperCase(Locale.ENGLISH));
	}

	@Test
	void shouldPropagateExceptionWhenMerchantDetailsApiCallFails()
	{

		when(baseStore.getDefaultLanguage()).thenReturn(defaultLanguage);

		when(defaultLanguage.getIsocode()).thenReturn(DEFAULT_LANGUAGE_ISO);

		when(novalnetEndpointConfigService.getMerchantDetailsUrl()).thenReturn(MERCHANT_DETAILS_URL);

		when(novalnetApiService.fetchMerchantDetails(eq(MERCHANT_DETAILS_URL), anyString(), eq(baseStore)))
				.thenThrow(new NovalnetException("API unreachable", new RuntimeException("API unavailable")));

		assertThatThrownBy(() -> service.callNovalnetMerchantDetails(ACTIVATION_KEY, baseStore))
				.isInstanceOf(NovalnetException.class);
	}

	@Test
	void shouldDelegateAddPaymentProcessToNovalnetPaymentService() throws CMSItemNotFoundException
	{
		service.addPaymentProcess(model, paymentDetailsForm, cartData);

		verify(novalnetPaymentService).addPaymentProcess(model, paymentDetailsForm, cartData);
	}

	@Test
	void shouldPropagateCmsItemNotFoundExceptionFromAddPaymentProcess() throws CMSItemNotFoundException
	{
		doThrow(new CMSItemNotFoundException("CMS component missing")).when(novalnetPaymentService).addPaymentProcess(model,
				paymentDetailsForm, cartData);

		assertThatThrownBy(() -> service.addPaymentProcess(model, paymentDetailsForm, cartData))
				.isInstanceOf(CMSItemNotFoundException.class).hasMessage("CMS component missing");
	}


	@Test
	void shouldPopulateBillingFromDeliveryAddressWhenUseDeliveryAddressTrueAndCountryPresent()
	{
		AddressData addressData = givenRealAddressData();

		addressData.setCountry(givenCountryData(COUNTRY_ISO));

		Customer customer = new Customer();

		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);

		service.populateCustomerAddressDetails(model, paymentDetailsForm, cartData, customer, addressData);

		assertThat(customer.getFirst_name()).isEqualTo(FIRST_NAME);

		assertThat(customer.getLast_name()).isEqualTo(LAST_NAME);

		assertThat(customer.getBilling().getCountry_code()).isEqualTo(COUNTRY_ISO);

		assertThat(customer.getShipping().getSame_as_billing()).isEqualTo("1");

		verify(sessionService).setAttribute(eq("novalnetPaymentRequest"), any(NovalnetPaymentRequest.class));
	}

	@Test
	void shouldPopulateBillingFromDeliveryAddressWithoutCountryCodeWhenCountryAbsent()
	{
		AddressData addressData = givenRealAddressData();

		addressData.setCountry(null);

		Customer customer = new Customer();

		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);

		service.populateCustomerAddressDetails(model, paymentDetailsForm, cartData, customer, addressData);

		assertThat(customer.getBilling().getCountry_code()).isNull();
	}

	@Test
	void shouldPopulateShippingFromCartAndOverwriteAddressDataFromFormWhenAddressFormPresent()
	{
		AddressData addressData = givenRealAddressData();

		AddressData deliveryAddress = givenRealAddressData();

		deliveryAddress.setCountry(givenCountryData(COUNTRY_ISO));

		Customer customer = new Customer();

		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(false);

		when(cartData.getDeliveryAddress()).thenReturn(deliveryAddress);

		when(paymentDetailsForm.getBillingAddress()).thenReturn(addressForm);

		when(addressForm.getAddressId()).thenReturn("addr-1");

		when(addressForm.getCountryIso()).thenReturn(COUNTRY_ISO);

		when(addressForm.getRegionIso()).thenReturn("BY");

		when(addressForm.getShippingAddress()).thenReturn(Boolean.TRUE);

		when(addressForm.getBillingAddress()).thenReturn(Boolean.FALSE);

		when(i18NFacade.getCountryForIsocode(COUNTRY_ISO)).thenReturn(givenCountryData(COUNTRY_ISO));

		when(i18NFacade.getRegion(COUNTRY_ISO, "BY")).thenReturn(new RegionData());

		when(paymentDetailsForm.getBillTo_firstName()).thenReturn(FIRST_NAME);

		when(paymentDetailsForm.getBillTo_lastName()).thenReturn(LAST_NAME);

		when(paymentDetailsForm.getBillTo_street1()).thenReturn(STREET_1);

		when(paymentDetailsForm.getBillTo_street2()).thenReturn(STREET_2);

		when(paymentDetailsForm.getBillTo_city()).thenReturn(CITY);

		when(paymentDetailsForm.getBillTo_postalCode()).thenReturn(POSTAL_CODE);

		when(paymentDetailsForm.getBillTo_country()).thenReturn(COUNTRY_ISO);

		service.populateCustomerAddressDetails(model, paymentDetailsForm, cartData, customer, addressData);

		assertThat(customer.getShipping().getFirst_name()).isEqualTo(FIRST_NAME);

		assertThat(customer.getBilling().getStreet()).isEqualTo(STREET_1 + STREET_2);

		assertThat(addressData.getLine1()).isEqualTo(STREET_1);

		verify(i18NFacade).getCountryForIsocode(COUNTRY_ISO);

		verify(i18NFacade).getRegion(COUNTRY_ISO, "BY");
	}

	@Test
	void shouldSkipAddressFormOverwriteWhenAddressFormAbsent()
	{
		AddressData addressData = givenRealAddressData();

		AddressData deliveryAddress = givenRealAddressData();

		deliveryAddress.setCountry(null);

		Customer customer = new Customer();

		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(false);

		when(cartData.getDeliveryAddress()).thenReturn(deliveryAddress);

		when(paymentDetailsForm.getBillingAddress()).thenReturn(null);

		when(paymentDetailsForm.getBillTo_firstName()).thenReturn(FIRST_NAME);

		when(paymentDetailsForm.getBillTo_lastName()).thenReturn(LAST_NAME);

		when(paymentDetailsForm.getBillTo_street1()).thenReturn(STREET_1);

		when(paymentDetailsForm.getBillTo_street2()).thenReturn(STREET_2);

		when(paymentDetailsForm.getBillTo_city()).thenReturn(CITY);

		when(paymentDetailsForm.getBillTo_postalCode()).thenReturn(POSTAL_CODE);

		when(paymentDetailsForm.getBillTo_country()).thenReturn(COUNTRY_ISO);

		service.populateCustomerAddressDetails(model, paymentDetailsForm, cartData, customer, addressData);

		assertThat(customer.getShipping().getCountry_code()).isNull();

		verify(i18NFacade, never()).getCountryForIsocode(anyString());

		verify(i18NFacade, never()).getRegion(anyString(), anyString());
	}


	@Test
	void shouldDelegateProcessOneClickTokenDataAndReturnTrue() throws CMSItemNotFoundException
	{
		when(novalnetOneClickPaymentService.processOneClickTokenData(CREDIT_CARD, paymentDetailsForm, model, cartData, null))
				.thenReturn(true);

		boolean result = service.processOneClickTokenData(CREDIT_CARD, paymentDetailsForm, model, cartData, null);

		assertThat(result).isTrue();

		verify(novalnetOneClickPaymentService).processOneClickTokenData(CREDIT_CARD, paymentDetailsForm, model, cartData, null);
	}

	@Test
	void shouldDelegateProcessOneClickTokenDataAndReturnFalse() throws CMSItemNotFoundException
	{
		when(novalnetOneClickPaymentService.processOneClickTokenData(CREDIT_CARD, paymentDetailsForm, model, cartData, null))
				.thenReturn(false);

		boolean result = service.processOneClickTokenData(CREDIT_CARD, paymentDetailsForm, model, cartData, null);

		assertThat(result).isFalse();

		verify(novalnetOneClickPaymentService).processOneClickTokenData(CREDIT_CARD, paymentDetailsForm, model, cartData, null);
	}


	@Test
	void shouldDelegateProcessTransactionAndReturnTrue()
	{
		Map<String, String> resultMap = Map.of("tid", "123");

		when(novalnetTransactionResultService.processTransaction(resultMap)).thenReturn(true);

		boolean result = service.processTransaction(resultMap);

		assertThat(result).isTrue();

		verify(novalnetTransactionResultService).processTransaction(resultMap);
	}

	@Test
	void shouldDelegateProcessTransactionAndReturnFalse()
	{
		Map<String, String> resultMap = Map.of("tid", "123");

		when(novalnetTransactionResultService.processTransaction(resultMap)).thenReturn(false);

		boolean result = service.processTransaction(resultMap);

		assertThat(result).isFalse();

		verify(novalnetTransactionResultService).processTransaction(resultMap);
	}


	@Test
	void shouldDelegateHandleStorePayment()
	{
		StringBuilder response = new StringBuilder("{}");

		JsonNode customerJson = mock(JsonNode.class);

		service.handleStorePayment(CREDIT_CARD, response, customerJson);

		verify(novalnetTransactionResultService).handleStorePayment(CREDIT_CARD, response, customerJson);
	}

	@Test
	void shouldDelegateHandleGuaranteeProcessAndReturnEmptyOnSuccess()
	{
		when(novalnetGuaranteeValidationService.handleGuaranteeProcess("payment", "1990-01-01", paymentDetailsForm, null))
				.thenReturn("");

		String result = service.handleGuaranteeProcess("payment", "1990-01-01", paymentDetailsForm, null);

		assertThat(result).isEmpty();

		verify(novalnetGuaranteeValidationService).handleGuaranteeProcess("payment", "1990-01-01", paymentDetailsForm, null);
	}

	@Test
	void shouldDelegateHandleGuaranteeProcessAndReturnErrorOnFailure()
	{
		when(novalnetGuaranteeValidationService.handleGuaranteeProcess("payment", "1990-01-01", paymentDetailsForm, null))
				.thenReturn("novalnet.age.error");

		String result = service.handleGuaranteeProcess("payment", "1990-01-01", paymentDetailsForm, null);

		assertThat(result).isEqualTo("novalnet.age.error");

		verify(novalnetGuaranteeValidationService).handleGuaranteeProcess("payment", "1990-01-01", paymentDetailsForm, null);
	}


	@Test
	void shouldThrowIllegalStateExceptionWhenPaymentRequestIsMissing()
	{
		when(sessionService.<NovalnetPaymentRequest> getAttribute("novalnetPaymentRequest")).thenReturn(null);

		assertThatIllegalStateException().isThrownBy(
				() -> service.createTransaction(httpServletRequest, baseStore, CREDIT_CARD, CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData))
				.withMessage("Customer data not found");
	}

	@Test
	void shouldThrowIllegalStateExceptionWhenPaymentRequestCustomerIsMissing()
	{
		when(sessionService.<NovalnetPaymentRequest> getAttribute("novalnetPaymentRequest"))
				.thenReturn(new NovalnetPaymentRequest());

		assertThatIllegalStateException().isThrownBy(
				() -> service.createTransaction(httpServletRequest, baseStore, CREDIT_CARD, CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData))
				.withMessage("Customer data not found");
	}

	@Test
	void shouldCreateTransactionAndSetReturnUrlsWhenRedirectRequired()
	{
		givenCreateTransactionCommonStubs(CREDIT_CARD);

		when(paymentConfigResult.isRedirect()).thenReturn(true);

		when(paymentConfigResult.isZeroAmountBooking()).thenReturn(false);

		when(paymentConfigResult.isVerifyPaymentData()).thenReturn(true);

		when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(CURRENT_URL));

		when(novalnetEndpointConfigService.getAuthorizeUrl()).thenReturn(AUTHORIZE_URL);

		when(novalnetApiService.sendRequest(eq(AUTHORIZE_URL), anyString())).thenReturn(new StringBuilder("{\"result\":\"ok\"}"));

		StringBuilder response = service.createTransaction(httpServletRequest, baseStore, CREDIT_CARD, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);

		assertThat(response.toString()).isEqualTo("{\"result\":\"ok\"}");

		verify(sessionService).setAttribute("novalnetZeroAmountBooking", Boolean.FALSE);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

		verify(novalnetApiService).sendRequest(eq(AUTHORIZE_URL), jsonCaptor.capture());

		assertThat(jsonCaptor.getValue()).contains("hop-response");
	}

	@Test
	void shouldCreateTransactionWithoutRedirectAndWithZeroAmountBooking()
	{
		givenCreateTransactionCommonStubs(CREDIT_CARD);

		when(paymentConfigResult.isRedirect()).thenReturn(false);

		when(paymentConfigResult.isZeroAmountBooking()).thenReturn(true);

		when(paymentConfigResult.isVerifyPaymentData()).thenReturn(false);

		when(novalnetEndpointConfigService.getPaymentUrl()).thenReturn(PAYMENT_URL);

		when(novalnetApiService.sendRequest(eq(PAYMENT_URL), anyString())).thenReturn(new StringBuilder("{\"result\":\"ok\"}"));

		StringBuilder response = service.createTransaction(httpServletRequest, baseStore, CREDIT_CARD, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);

		assertThat(response.toString()).isEqualTo("{\"result\":\"ok\"}");

		verify(sessionService).setAttribute("novalnetZeroAmountBooking", Boolean.TRUE);

		verify(httpServletRequest, never()).getRequestURL();
	}

	@Test
	void shouldUseHostedUrlsWhenCurrentPaymentIsApplePay()
	{
		givenCreateTransactionCommonStubs(APPLE_PAY);

		when(paymentConfigResult.isRedirect()).thenReturn(false);

		when(paymentConfigResult.isZeroAmountBooking()).thenReturn(false);

		when(paymentConfigResult.isVerifyPaymentData()).thenReturn(true);

		when(novalnetEndpointConfigService.getAuthorizeHostedUrl()).thenReturn(AUTHORIZE_HOSTED_URL);

		when(novalnetApiService.sendRequest(eq(AUTHORIZE_HOSTED_URL), anyString()))
				.thenReturn(new StringBuilder("{\"result\":\"ok\"}"));

		service.createTransaction(httpServletRequest, baseStore, APPLE_PAY, CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData);

		verify(novalnetEndpointConfigService).getAuthorizeHostedUrl();

		verify(novalnetEndpointConfigService, never()).getAuthorizeUrl();
	}


	@Test
	void shouldBookWalletTransactionAndStoreAllSessionAttributesWhenResponseIsComplete() throws NovalnetException
	{
		givenCreateTransactionCommonStubs(GOOGLE_PAY);

		when(paymentConfigResult.isRedirect()).thenReturn(true);

		when(paymentConfigResult.isZeroAmountBooking()).thenReturn(false);

		when(paymentConfigResult.isVerifyPaymentData()).thenReturn(false);

		when(httpServletRequest.getRequestURL())
				.thenReturn(new StringBuffer(CURRENT_URL.replace("placeOrder", "bookWalletTransaction")));

		when(novalnetEndpointConfigService.getPaymentUrl()).thenReturn(PAYMENT_URL);

		String jsonResponse = "{" + "\"result\":{" + "\"redirect_url\":\"https://pay.example.com/redirect\"" + "},"
				+ "\"transaction\":{" + "\"txn_secret\":\"" + TXN_SECRET_VALUE + "\"," + "\"tid\":\"" + WALLET_TID + "\"" + "}" + "}";

		when(novalnetApiService.sendRequest(eq(PAYMENT_URL), anyString())).thenReturn(new StringBuilder(jsonResponse));

		when(baseStore.getNovalnetPaymentAccessKey()).thenReturn(NOVALNET_ACCESS_KEY);

		String result = service.bookWalletTransaction(httpServletRequest, baseStore, CUSTOMER_NO, ORDER_AMOUNT_CENT,
				cartData);

		assertThat(result).isEqualTo(jsonResponse);

		verify(sessionService).setAttribute("txn_check", NOVALNET_ACCESS_KEY);

		verify(sessionService).setAttribute("txn_secret", TXN_SECRET_VALUE);

		verify(sessionService).setAttribute("wallet_tid", WALLET_TID);
	}

	@Test
	void shouldBookWalletTransactionWithoutStoringTxnCheckWhenNoRedirectUrlPresent() throws NovalnetException
	{
		givenCreateTransactionCommonStubs(GOOGLE_PAY);

		when(paymentConfigResult.isRedirect()).thenReturn(false);

		when(paymentConfigResult.isZeroAmountBooking()).thenReturn(false);

		when(paymentConfigResult.isVerifyPaymentData()).thenReturn(false);

		when(novalnetEndpointConfigService.getPaymentUrl()).thenReturn(PAYMENT_URL);

		String jsonResponse = "{\"result\":{}," + "\"transaction\":{" + "\"tid\":\"" + WALLET_TID + "\"" + "}}";

		when(novalnetApiService.sendRequest(eq(PAYMENT_URL), anyString())).thenReturn(new StringBuilder(jsonResponse));

		String result = service.bookWalletTransaction(httpServletRequest, baseStore, CUSTOMER_NO, ORDER_AMOUNT_CENT,
				cartData);

		assertThat(result).isEqualTo(jsonResponse);

		verify(sessionService, never()).setAttribute(eq("txn_check"), anyString());

		verify(sessionService).setAttribute("wallet_tid", WALLET_TID);

		verify(sessionService, never()).setAttribute(eq("txn_secret"), anyString());
	}

	@Test
	void shouldBookWalletTransactionWithoutStoringTransactionAttributesWhenTransactionNodeMissing() throws NovalnetException
	{
		givenCreateTransactionCommonStubs(GOOGLE_PAY);

		when(paymentConfigResult.isRedirect()).thenReturn(false);

		when(paymentConfigResult.isZeroAmountBooking()).thenReturn(false);

		when(paymentConfigResult.isVerifyPaymentData()).thenReturn(false);

		when(novalnetEndpointConfigService.getPaymentUrl()).thenReturn(PAYMENT_URL);

		String jsonResponse = "{\"result\":{}}";

		when(novalnetApiService.sendRequest(eq(PAYMENT_URL), anyString())).thenReturn(new StringBuilder(jsonResponse));

		String result = service.bookWalletTransaction(httpServletRequest, baseStore, CUSTOMER_NO, ORDER_AMOUNT_CENT,
				cartData);

		assertThat(result).isEqualTo(jsonResponse);

		verify(sessionService, never()).setAttribute(eq("txn_secret"), anyString());

		verify(sessionService, never()).setAttribute(eq("wallet_tid"), anyString());
	}
}
