/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.localization.Localization;

import java.util.Collections;

import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.order.NovalnetOrderService;


@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultNovalnetTransactionBookingServiceTest
{
	private static final String PAYMENT_URL = "https://test.com/payment";
	private static final String ORDER_CODE = "100001";
	private static final String FIRSTNAME = "John";
	private static final String LASTNAME = "Doe";
	private static final String TOWN = "Berlin";
	private static final String POSTAL_CODE = "10115";
	private static final String CURRENCY_ISOCODE = "EUR";
	private static final int BOOK_AMOUNT = 100;
	private static final int TARIFF_ID = 12345;
	private static final int SUCCESS_STATUS_CODE = 100;
	private static final int FAILURE_STATUS_CODE = 333;
	private static final String CUSTOMER_PK = "8796093023001";

	@Mock
	private NovalnetApiService novalnetApiService;

	@Mock
	private ConfigurationService configurationService;

	@Mock
	private Configuration configuration;

	@Mock
	private NovalnetDao novalnetDao;

	@Mock
	private NovalnetOrderService novalnetOrderService;

	@Mock
	private OrderModel order;

	@Mock
	private NovalnetPaymentInfoModel paymentInfo;

	@Mock
	private AddressModel paymentAddress;

	@Mock
	private BaseStoreModel baseStore;

	@Mock
	private CurrencyModel currency;

	@Mock
	private LanguageModel language;

	@Mock
	private CountryModel country;

	@Mock
	private CustomerModel customer;

	@Mock
	private PK userPk;

	@InjectMocks
	private DefaultNovalnetTransactionBookingService service;

	private MockedStatic<Localization> localizationMock;

	@BeforeEach
	public void setUpTestData()
	{
		localizationMock = org.mockito.Mockito.mockStatic(Localization.class);
		localizationMock.when(() -> Localization.getLocalizedString("novalnet.booking.success")).thenReturn("%s %s %s");

		lenient().when(configurationService.getConfiguration()).thenReturn(configuration);
		lenient().when(configuration.getString("novalnet.payment.url")).thenReturn(PAYMENT_URL);

		lenient().when(order.getPaymentInfo()).thenReturn(paymentInfo);
		lenient().when(order.getStore()).thenReturn(baseStore);
		lenient().when(order.getCurrency()).thenReturn(currency);
		lenient().when(order.getPaymentAddress()).thenReturn(paymentAddress);
		lenient().when(order.getLanguage()).thenReturn(language);
		lenient().when(order.getBookAmount()).thenReturn(BOOK_AMOUNT);
		lenient().when(order.getCode()).thenReturn(ORDER_CODE);
		lenient().when(order.getUser()).thenReturn(customer);

		lenient().when(currency.getIsocode()).thenReturn(CURRENCY_ISOCODE);

		lenient().when(paymentInfo.getPaymentProvider()).thenReturn("novalnetCreditCard");

		lenient().when(paymentAddress.getFirstname()).thenReturn(FIRSTNAME);
		lenient().when(paymentAddress.getLastname()).thenReturn(LASTNAME);
		lenient().when(paymentAddress.getStreetname()).thenReturn("Main Street");
		lenient().when(paymentAddress.getStreetnumber()).thenReturn("10");
		lenient().when(paymentAddress.getTown()).thenReturn(TOWN);
		lenient().when(paymentAddress.getPostalcode()).thenReturn(POSTAL_CODE);
		lenient().when(paymentAddress.getCountry()).thenReturn(country);
		lenient().when(paymentAddress.getEmail()).thenReturn("[john@test.com](mailto:john@test.com)");

		lenient().when(country.getIsocode()).thenReturn("DE");
		lenient().when(language.getIsocode()).thenReturn("en");

		lenient().when(baseStore.getNovalnetAPIKey()).thenReturn("test-api-key");
		lenient().when(baseStore.getNovalnetTariffId()).thenReturn(TARIFF_ID);

		lenient().when(customer.getPk()).thenReturn(userPk);
		lenient().when(userPk.toString()).thenReturn(CUSTOMER_PK);

		lenient().when(novalnetDao.getStoredPaymentToken(order)).thenReturn("test-token");

		lenient().when(novalnetApiService.bookTransactionAmount(eq(PAYMENT_URL), any(String.class), eq(baseStore)))
				.thenReturn(new StringBuilder("{\"result\":{\"status_code\":" + SUCCESS_STATUS_CODE + ",\"status_text\":\"SUCCESS\"},"
						+ "\"transaction\":{\"tid\":\"123456789\"}}"));
	}

	@AfterEach
	public void tearDown()
	{
		if (localizationMock != null)
		{
			localizationMock.close();
		}
	}


	@Test
	public void shouldUpdateOrderWhenBookingSucceeds() throws Exception
	{
		service.bookTransaction(order);

		verify(novalnetApiService).bookTransactionAmount(eq(PAYMENT_URL), any(String.class), eq(baseStore));

		verify(novalnetOrderService).updateCallbackComments(contains("123456789"), eq(ORDER_CODE), any());

		verify(order).setPaidAmount(BOOK_AMOUNT);
		verify(order).setBookAmount(null);
	}

	@Test
	public void shouldThrowExceptionWhenBookAmountIsZero()
	{
		lenient().when(order.getBookAmount()).thenReturn(0);

		assertThatThrownBy(() -> service.bookTransaction(order)).isInstanceOf(InterceptorException.class)
				.hasMessageContaining("Invalid booking amount for order " + ORDER_CODE);

		verifyNoInteractions(novalnetApiService);
	}

	@Test
	public void shouldThrowExceptionWhenPaymentTokenIsMissing()
	{
		lenient().when(novalnetDao.getStoredPaymentToken(order)).thenReturn(null);

		assertThatThrownBy(() -> service.bookTransaction(order)).isInstanceOf(InterceptorException.class)
				.hasMessageContaining("No stored Novalnet payment token found on order " + ORDER_CODE);

		verifyNoInteractions(novalnetApiService);
	}

	@Test
	public void shouldThrowExceptionWhenApiReturnsFailure()
	{
		lenient().when(novalnetApiService.bookTransactionAmount(eq(PAYMENT_URL), any(String.class), eq(baseStore))).thenReturn(
				new StringBuilder("{\"result\":{\"status_code\":" + FAILURE_STATUS_CODE + ",\"status_text\":\"Booking failed\"}}"));

		assertThatThrownBy(() -> service.bookTransaction(order)).isInstanceOf(InterceptorException.class)
				.hasMessageContaining("Novalnet update booking failed for order " + ORDER_CODE + ". Reason: Booking failed");

		verify(order, never()).setPaidAmount(any());
		verify(order, never()).setBookAmount(any());
	}

	@Test
	public void shouldUseDeliveryAddressWhenPaymentAddressIsInvalid() throws Exception
	{
		AddressModel deliveryAddress = mock(AddressModel.class);

		lenient().when(paymentAddress.getFirstname()).thenReturn(null);
		lenient().when(order.getDeliveryAddress()).thenReturn(deliveryAddress);

		lenient().when(deliveryAddress.getFirstname()).thenReturn(FIRSTNAME);
		lenient().when(deliveryAddress.getLastname()).thenReturn(LASTNAME);
		lenient().when(deliveryAddress.getStreetname()).thenReturn("Delivery Street");
		lenient().when(deliveryAddress.getStreetnumber()).thenReturn("20");
		lenient().when(deliveryAddress.getTown()).thenReturn(TOWN);
		lenient().when(deliveryAddress.getPostalcode()).thenReturn(POSTAL_CODE);
		lenient().when(deliveryAddress.getCountry()).thenReturn(country);
		lenient().when(deliveryAddress.getEmail()).thenReturn("[delivery@test.com](mailto:delivery@test.com)");

		service.bookTransaction(order);

		verify(novalnetApiService).bookTransactionAmount(eq(PAYMENT_URL), any(String.class), eq(baseStore));
	}

	@Test
	public void shouldUseCustomerAddressWhenPaymentAndDeliveryAddressesAreInvalid() throws Exception
	{
		AddressModel customerAddress = mock(AddressModel.class);

		lenient().when(order.getPaymentAddress()).thenReturn(null);
		lenient().when(order.getDeliveryAddress()).thenReturn(null);
		lenient().when(order.getUser()).thenReturn(customer);

		lenient().when(customer.getPk()).thenReturn(userPk);
		lenient().when(userPk.toString()).thenReturn(CUSTOMER_PK);
		lenient().when(customer.getAddresses()).thenReturn(Collections.singletonList(customerAddress));

		lenient().when(customerAddress.getFirstname()).thenReturn(FIRSTNAME);
		lenient().when(customerAddress.getLastname()).thenReturn(LASTNAME);
		lenient().when(customerAddress.getStreetname()).thenReturn("Customer Street");
		lenient().when(customerAddress.getStreetnumber()).thenReturn("30");
		lenient().when(customerAddress.getTown()).thenReturn(TOWN);
		lenient().when(customerAddress.getPostalcode()).thenReturn(POSTAL_CODE);
		lenient().when(customerAddress.getCountry()).thenReturn(country);
		lenient().when(customerAddress.getEmail()).thenReturn("[customer@test.com](mailto:customer@test.com)");

		service.bookTransaction(order);

		verify(novalnetApiService).bookTransactionAmount(eq(PAYMENT_URL), any(String.class), eq(baseStore));
	}

	@Test
	public void shouldThrowExceptionWhenNoUsableAddressExists()
	{
		lenient().when(order.getPaymentAddress()).thenReturn(null);
		lenient().when(order.getDeliveryAddress()).thenReturn(null);
		lenient().when(order.getUser()).thenReturn(customer);
		lenient().when(customer.getAddresses()).thenReturn(Collections.emptyList());

		assertThatThrownBy(() -> service.bookTransaction(order)).isInstanceOf(InterceptorException.class)
				.hasMessageContaining("No usable billing address found for order " + ORDER_CODE
						+ " (payment, delivery, and saved addresses are all incomplete)");

		verifyNoInteractions(novalnetApiService);
	}

	@Test
	public void shouldReturnCorrectPaymentTypeForSupportedProviders()
	{
		assertThat(DefaultNovalnetTransactionBookingService.getPaymentType("novalnetCreditCard")).isEqualTo("CREDITCARD");

		assertThat(DefaultNovalnetTransactionBookingService.getPaymentType("novalnetDirectDebitSepa"))
				.isEqualTo("DIRECT_DEBIT_SEPA");

		assertThat(DefaultNovalnetTransactionBookingService.getPaymentType("novalnetGooglePay")).isEqualTo("GOOGLEPAY");

		assertThat(DefaultNovalnetTransactionBookingService.getPaymentType("novalnetApplePay")).isEqualTo("APPLEPAY");

		assertThat(DefaultNovalnetTransactionBookingService.getPaymentType("novalnetDirectDebitAch")).isEqualTo("DIRECT_DEBIT_ACH");
	}


	@Test
	public void shouldReturnOriginalProviderWhenProviderIsUnknown()
	{
		assertThat(DefaultNovalnetTransactionBookingService.getPaymentType("CUSTOM_PAYMENT")).isEqualTo("CUSTOM_PAYMENT");
	}
}


