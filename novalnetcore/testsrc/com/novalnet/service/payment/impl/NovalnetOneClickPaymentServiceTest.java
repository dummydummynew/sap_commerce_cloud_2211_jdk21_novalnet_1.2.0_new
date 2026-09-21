/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.localization.Localization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.enums.OnholdActionTypeWithZeroAmount;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitAchPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.util.NovalnetUtils;


/**
 * Unit tests for {@link NovalnetOneClickPaymentService}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
class NovalnetOneClickPaymentServiceTest
{
	private static final String SEPA = "novalnetDirectDebitSepa";
	private static final String ACH = "novalnetDirectDebitAch";
	private static final String GUARANTEED_SEPA = "novalnetGuaranteedDirectDebitSepa";
	private static final String GUARANTEED_INVOICE = "novalnetGuaranteedInvoice";
	private static final String CREDIT_CARD = "novalnetCreditCard";
	private static final String GOOGLE_PAY = "novalnetGooglePay";
	private static final String APPLE_PAY = "novalnetApplePay";
	private static final String UNKNOWN_METHOD = "novalnetInvoice";

	private static final String ATTR_CHECKOUT_ERROR = "novalnetCheckoutError";

	private static final String ATTR_SEPA_STORE_PAYMENT_DATA = "novalnetDirectDebitSepaStorePaymentData";
	private static final String ATTR_SEPA_IBAN = "novalnetDirectDebitSepaAccountIban";
	private static final String ATTR_SEPA_HOLDER = "novalnetDirectDebitSepaAccountHolder";
	private static final String ATTR_SEPA_BIC = "novalnetDirectDebitSepaAccountBic";
	private static final String ATTR_SEPA_TOKEN = "novalnetDirectDebitSepatoken";

	private static final String ATTR_ACH_STORE_PAYMENT_DATA = "novalnetDirectDebitAchStorePaymentData";
	private static final String ATTR_ACH_HOLDER = "novalnetDirectDebitAchAccountHolder";
	private static final String ATTR_ACH_NUMBER = "novalnetDirectDebitAchAchAccountNumber";
	private static final String ATTR_ACH_ROUTING = "novalnetDirectDebitAchRoutingNumber";
	private static final String ATTR_ACH_TOKEN = "novalnetDirectDebitAchtoken";

	private static final String ATTR_GSEPA_IBAN = "novalnetGuaranteedDirectDebitSepaAccountIban";
	private static final String ATTR_GSEPA_HOLDER = "novalnetGuaranteedDirectDebitSepaAccountHolder";
	private static final String ATTR_GSEPA_BIC = "novalnetGuaranteedDirectDebitSepaAccountBic";

	private static final String ATTR_CC_PAN_HASH = "novalnetCreditCardPanHash";
	private static final String ATTR_CC_UNIQUE_ID = "novalnetCreditCardUniqueId";
	private static final String ATTR_CC_DO_REDIRECT = "do_redirect";
	private static final String ATTR_CC_STORE_PAYMENT_DATA = "novalnetCreditCardStorePaymentData";
	private static final String ATTR_CC_TOKEN = "novalnetCreditCardtoken";

	private static final String ATTR_GOOGLE_PAY_STORE_PAYMENT_DATA = "novalnetGooglePayStorePaymentData";
	private static final String ATTR_APPLE_PAY_STORE_PAYMENT_DATA = "novalnetApplePayStorePaymentData";

	private static final String SEPA_TOKEN_1 = "sepa-token-1";
	private static final String SEPA_TOKEN_2 = "sepa-token-2";
	private static final String ACH_TOKEN_1 = "ach-token-1";
	private static final String ACH_TOKEN_2 = "ach-token-2";
	private static final String CC_TOKEN_1 = "cc-token-1";
	private static final String CC_TOKEN_2 = "cc-token-2";

	private static final String IBAN = "DE89370400440532013000";
	private static final String HOLDER = "John Doe";
	private static final String BIC = "COBADEFFXXX";
	private static final String ACH_ACCOUNT_NUMBER = "1234567890";
	private static final String ACH_ROUTING_NUMBER = "021000021";
	private static final String PAN_HASH = "panhash123";
	private static final String UNIQUE_ID = "unique123";
	private static final String DO_REDIRECT = "1";

	/**
	 * Expected number of times that valid payment data is stored in the session.
	 */
	private static final int EXPECTED_INVOCATION_COUNT = 2;

	private NovalnetOneClickPaymentService service;

	@Mock
	private SessionService sessionService;

	@Mock
	private NovalnetCheckoutService novalnetCheckoutService;

	@Mock
	private PaymentModeService paymentModeService;

	@Mock
	private NovalnetPaymentService novalnetPaymentService;

	@Mock
	private NovalnetGuaranteeValidationService novalnetGuaranteeValidationService;

	@Mock
	private NovalnetPaymentDetailsForm form;

	@Mock
	private Model model;

	@Mock
	private CartData cartData;

	@Mock
	private AddressData deliveryAddress;

	@Mock
	private NovalnetDirectDebitSepaPaymentModeModel sepaPaymentMode;

	@Mock
	private NovalnetDirectDebitAchPaymentModeModel achPaymentMode;

	@Mock
	private NovalnetGuaranteedDirectDebitSepaPaymentModeModel guaranteedSepaPaymentMode;

	@Mock
	private NovalnetCreditCardPaymentModeModel creditCardPaymentMode;

	@Mock
	private NovalnetGooglePayPaymentModeModel googlePayPaymentMode;

	@Mock
	private NovalnetApplePayPaymentModeModel applePayPaymentMode;

	private MockedStatic<Localization> localizationMock;
	private MockedStatic<NovalnetUtils> novalnetUtilsMock;

	@BeforeEach
	void setUp()
	{
		service = new NovalnetOneClickPaymentService();

		ReflectionTestUtils.setField(service, "sessionService", sessionService);
		ReflectionTestUtils.setField(service, "novalnetCheckoutService", novalnetCheckoutService);
		ReflectionTestUtils.setField(service, "paymentModeService", paymentModeService);
		ReflectionTestUtils.setField(service, "novalnetPaymentService", novalnetPaymentService);
		ReflectionTestUtils.setField(service, "novalnetGuaranteeValidationService", novalnetGuaranteeValidationService);

		localizationMock = mockStatic(Localization.class);
		localizationMock.when(() -> Localization.getLocalizedString(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		novalnetUtilsMock = mockStatic(NovalnetUtils.class);
	}

	@AfterEach
	void tearDown()
	{
		localizationMock.close();
		novalnetUtilsMock.close();
	}

	@Test
	void shouldReturnFalseWhenCurrentPaymentIsNull() throws CMSItemNotFoundException
	{
		final boolean result = service.processOneClickTokenData(null, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(paymentModeService, never()).getPaymentModeForCode(anyString());
		verify(sessionService, never()).setAttribute(anyString(), any());
	}

	@Test
	void shouldReturnTrueForUnknownPaymentMethod() throws CMSItemNotFoundException
	{
		final boolean result = service.processOneClickTokenData(UNKNOWN_METHOD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(paymentModeService, never()).getPaymentModeForCode(anyString());
	}

	@Test
	void shouldStoreGooglePayFlagAndReturnTrueWhenPaymentModeExists() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(GOOGLE_PAY)).thenReturn(googlePayPaymentMode);

		when(googlePayPaymentMode.getOnholdActionTypeWithZeroAmount())
				.thenReturn(OnholdActionTypeWithZeroAmount.AUTHORIZE_WITH_ZERO_AMOUNT);

		novalnetUtilsMock.when(() -> NovalnetUtils.isZeroAmountBooking(OnholdActionTypeWithZeroAmount.AUTHORIZE_WITH_ZERO_AMOUNT))
				.thenReturn(true);

		final boolean result = service.processOneClickTokenData(GOOGLE_PAY, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_GOOGLE_PAY_STORE_PAYMENT_DATA, true);
	}

	@Test
	void shouldReturnFalseForGooglePayWhenPaymentModeNotFound() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(GOOGLE_PAY)).thenReturn(null);

		final boolean result = service.processOneClickTokenData(GOOGLE_PAY, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService, never()).setAttribute(eq(ATTR_GOOGLE_PAY_STORE_PAYMENT_DATA), any());
	}

	@Test
	void shouldStoreApplePayFlagAndReturnTrueWhenPaymentModeExists() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(APPLE_PAY)).thenReturn(applePayPaymentMode);

		when(applePayPaymentMode.getOnholdActionTypeWithZeroAmount()).thenReturn(OnholdActionTypeWithZeroAmount.CAPTURE);

		novalnetUtilsMock.when(() -> NovalnetUtils.isZeroAmountBooking(OnholdActionTypeWithZeroAmount.CAPTURE)).thenReturn(false);

		final boolean result = service.processOneClickTokenData(APPLE_PAY, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_APPLE_PAY_STORE_PAYMENT_DATA, false);
	}

	@Test
	void shouldReturnFalseForApplePayWhenPaymentModeNotFound() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(APPLE_PAY)).thenReturn(null);

		final boolean result = service.processOneClickTokenData(APPLE_PAY, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService, never()).setAttribute(eq(ATTR_APPLE_PAY_STORE_PAYMENT_DATA), any());
	}

	@Test
	void shouldReturnFalseWhenSepaGuestMissingIban() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAccountIban()).thenReturn(null);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "novalnet.iban.required");
	}

	@Test
	void shouldReturnFalseWhenSepaGuestMissingAccountHolder() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAccountIban()).thenReturn(IBAN);
		when(form.getAccountHolder()).thenReturn(null);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "novalnet.account.holder.required");
	}

	@Test
	void shouldReturnTrueWhenSepaGuestFieldsValid() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAccountIban()).thenReturn(IBAN);
		when(form.getAccountHolder()).thenReturn(HOLDER);
		when(form.getAccountBic()).thenReturn(BIC);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService, times(EXPECTED_INVOCATION_COUNT)).setAttribute(ATTR_SEPA_IBAN, IBAN);
		verify(sessionService, times(EXPECTED_INVOCATION_COUNT)).setAttribute(ATTR_SEPA_HOLDER, HOLDER);
		verify(sessionService, times(EXPECTED_INVOCATION_COUNT)).setAttribute(ATTR_SEPA_BIC, BIC);
	}

	@Test
	void shouldStoreSepaFieldsDirectlyWhenNotOneClickEligible() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getAccountIban()).thenReturn(IBAN);
		when(form.getAccountHolder()).thenReturn(HOLDER);
		when(form.getAccountBic()).thenReturn(BIC);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_SEPA_IBAN, IBAN);
		verify(sessionService).setAttribute(ATTR_SEPA_HOLDER, HOLDER);
		verify(sessionService).setAttribute(ATTR_SEPA_BIC, BIC);
	}

	@Test
	void shouldFallBackToManualEntryWhenSepaOneClickDataEmpty() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitSepaOneClickData1()).thenReturn("");
		when(form.isDirectDebitSepaSaveData()).thenReturn(true);
		when(form.getAccountIban()).thenReturn(IBAN);
		when(form.getAccountHolder()).thenReturn(HOLDER);
		when(form.getAccountBic()).thenReturn(BIC);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_SEPA_STORE_PAYMENT_DATA, true);
		verify(sessionService).setAttribute(ATTR_SEPA_IBAN, IBAN);
		verify(sessionService).setAttribute(ATTR_SEPA_HOLDER, HOLDER);
	}

	@Test
	void shouldUseFirstStoredTokenWhenSepaSelectionIsOne() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitSepaOneClickData1()).thenReturn("1");
		when(sessionService.getAttribute("novalnetDirectDebitSepaOneClickToken1")).thenReturn(SEPA_TOKEN_1);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_SEPA_TOKEN, SEPA_TOKEN_1);
	}

	@Test
	void shouldUseSecondStoredTokenWhenSepaSelectionIsTwo() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitSepaOneClickData1()).thenReturn("2");
		when(sessionService.getAttribute("novalnetDirectDebitSepaOneClickToken2")).thenReturn(SEPA_TOKEN_2);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_SEPA_TOKEN, SEPA_TOKEN_2);
	}

	@Test
	void shouldFallBackToManualEntryWhenSepaSelectionIsThree() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitSepaOneClickData1()).thenReturn("3");
		when(form.isDirectDebitSepaSaveData()).thenReturn(false);
		when(form.getAccountIban()).thenReturn(IBAN);
		when(form.getAccountHolder()).thenReturn(HOLDER);
		when(form.getAccountBic()).thenReturn(BIC);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_SEPA_IBAN, IBAN);
		verify(sessionService).setAttribute(ATTR_SEPA_HOLDER, HOLDER);
	}

	@Test
	void shouldReturnFalseWhenSepaManualEntryMissingIban() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAccountIban()).thenReturn("");

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(eq(ATTR_CHECKOUT_ERROR), anyString());
	}

	@Test
	void shouldStoreSepaPaymentDataPreferenceWhenSaveDataTrue() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(SEPA)).thenReturn(sepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitSepaOneClickData1()).thenReturn("3");
		when(form.isDirectDebitSepaSaveData()).thenReturn(true);
		when(form.getAccountIban()).thenReturn(IBAN);
		when(form.getAccountHolder()).thenReturn(HOLDER);
		when(form.getAccountBic()).thenReturn(BIC);

		final boolean result = service.processOneClickTokenData(SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_SEPA_STORE_PAYMENT_DATA, true);
	}

	@Test
	void shouldReturnFalseWhenAchGuestMissingAccountHolder() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAchAccountHolder()).thenReturn("");

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(eq(ATTR_CHECKOUT_ERROR), anyString());
	}

	@Test
	void shouldReturnFalseWhenAchGuestMissingAccountNumber() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn("");

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(eq(ATTR_CHECKOUT_ERROR), anyString());
	}

	@Test
	void shouldReturnFalseWhenAchGuestMissingRoutingNumber() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(null);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "novalnet.routing.number.required");
	}

	@Test
	void shouldReturnTrueWhenAchGuestFieldsValid() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(ACH_ROUTING_NUMBER);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService, times(EXPECTED_INVOCATION_COUNT)).setAttribute(ATTR_ACH_HOLDER, HOLDER);
		verify(sessionService, times(EXPECTED_INVOCATION_COUNT)).setAttribute(ATTR_ACH_NUMBER, ACH_ACCOUNT_NUMBER);
		verify(sessionService, times(EXPECTED_INVOCATION_COUNT)).setAttribute(ATTR_ACH_ROUTING, ACH_ROUTING_NUMBER);
	}

	@Test
	void shouldStoreAchFieldsDirectlyWhenNotOneClickEligible() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(ACH_ROUTING_NUMBER);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_ACH_HOLDER, HOLDER);
		verify(sessionService).setAttribute(ATTR_ACH_NUMBER, ACH_ACCOUNT_NUMBER);
		verify(sessionService).setAttribute(ATTR_ACH_ROUTING, ACH_ROUTING_NUMBER);
	}

	@Test
	void shouldUseFirstStoredTokenWhenAchSelectionIsOne() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitAchOneClickData1()).thenReturn("1");
		when(sessionService.getAttribute("novalnetDirectDebitAchOneClickToken1")).thenReturn(ACH_TOKEN_1);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_ACH_TOKEN, ACH_TOKEN_1);
	}

	@Test
	void shouldUseSecondStoredTokenWhenAchSelectionIsTwo() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitAchOneClickData1()).thenReturn("2");
		when(sessionService.getAttribute("novalnetDirectDebitAchOneClickToken2")).thenReturn(ACH_TOKEN_2);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_ACH_TOKEN, ACH_TOKEN_2);
	}

	@Test
	void shouldFallBackToManualEntryWhenAchSelectionIsThree() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitAchOneClickData1()).thenReturn("3");
		when(form.isDirectDebitAchSaveData()).thenReturn(false);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(ACH_ROUTING_NUMBER);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_ACH_HOLDER, HOLDER);
		verify(sessionService).setAttribute(ATTR_ACH_NUMBER, ACH_ACCOUNT_NUMBER);
		verify(sessionService).setAttribute(ATTR_ACH_ROUTING, ACH_ROUTING_NUMBER);
	}

	@Test
	void shouldFallBackToManualEntryWhenAchOneClickDataEmpty() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitAchOneClickData1()).thenReturn("");
		when(form.isDirectDebitAchSaveData()).thenReturn(true);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(ACH_ROUTING_NUMBER);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_ACH_STORE_PAYMENT_DATA, true);
		verify(sessionService).setAttribute(ATTR_ACH_HOLDER, HOLDER);
		verify(sessionService).setAttribute(ATTR_ACH_NUMBER, ACH_ACCOUNT_NUMBER);
		verify(sessionService).setAttribute(ATTR_ACH_ROUTING, ACH_ROUTING_NUMBER);
	}

	@Test
	void shouldReturnFalseWhenAchManualEntryMissingRoutingNumber() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitAchOneClickData1()).thenReturn("3");
		when(form.isDirectDebitAchSaveData()).thenReturn(false);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(null);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "novalnet.routing.number.required");
	}

	@Test
	void shouldStoreAchPaymentDataPreferenceWhenSaveDataTrue() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(ACH)).thenReturn(achPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(achPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(form.getDirectDebitAchOneClickData1()).thenReturn("3");
		when(form.isDirectDebitAchSaveData()).thenReturn(true);
		when(form.getAchAccountHolder()).thenReturn(HOLDER);
		when(form.getAchAccountNumber()).thenReturn(ACH_ACCOUNT_NUMBER);
		when(form.getAchRoutingNumber()).thenReturn(ACH_ROUTING_NUMBER);

		final boolean result = service.processOneClickTokenData(ACH, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_ACH_STORE_PAYMENT_DATA, true);
	}

	@Test
	void shouldReturnFalseWhenGuaranteedSepaGuestMissingIban() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(GUARANTEED_SEPA)).thenReturn(guaranteedSepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(form.getGuaranteeAccountIban()).thenReturn(null);

		final boolean result = service.processOneClickTokenData(GUARANTEED_SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isFalse();
		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "novalnet.iban.required");
	}

	@Test
	void shouldStoreGuaranteedSepaFieldsAndValidateWhenNotOneClickEligible() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(GUARANTEED_SEPA)).thenReturn(guaranteedSepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(guaranteedSepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getGuaranteeAccountIban()).thenReturn(IBAN);
		when(form.getGuaranteeAccountHolder()).thenReturn(HOLDER);
		when(form.getGuaranteeAccountBic()).thenReturn(BIC);

		when(novalnetGuaranteeValidationService.handleGuaranteeProcess(eq(GUARANTEED_SEPA), any(), eq(form), eq(deliveryAddress)))
				.thenReturn("");

		final boolean result = service.processOneClickTokenData(GUARANTEED_SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_GSEPA_IBAN, IBAN);
		verify(sessionService).setAttribute(ATTR_GSEPA_HOLDER, HOLDER);
		verify(sessionService).setAttribute(ATTR_GSEPA_BIC, BIC);
		verify(novalnetPaymentService, never()).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldAddPaymentProcessWhenGuaranteedSepaValidationFails() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(GUARANTEED_SEPA)).thenReturn(guaranteedSepaPaymentMode);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(guaranteedSepaPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getGuaranteeAccountIban()).thenReturn(IBAN);
		when(form.getGuaranteeAccountHolder()).thenReturn(HOLDER);
		when(form.getGuaranteeAccountBic()).thenReturn(BIC);

		when(novalnetGuaranteeValidationService.handleGuaranteeProcess(eq(GUARANTEED_SEPA), any(), eq(form), eq(deliveryAddress)))
				.thenReturn("validation.error");

		final boolean result = service.processOneClickTokenData(GUARANTEED_SEPA, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(novalnetPaymentService).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldReturnTrueWhenGuaranteedInvoiceValidationSucceeds() throws CMSItemNotFoundException
	{
		when(novalnetGuaranteeValidationService.handleGuaranteeProcess(eq(GUARANTEED_INVOICE), any(), eq(form),
				eq(deliveryAddress))).thenReturn("");

		final boolean result = service.processOneClickTokenData(GUARANTEED_INVOICE, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(novalnetPaymentService, never()).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldTriggerAddPaymentProcessWhenGuaranteedInvoiceValidationFails() throws CMSItemNotFoundException
	{
		when(novalnetGuaranteeValidationService.handleGuaranteeProcess(eq(GUARANTEED_INVOICE), any(), eq(form),
				eq(deliveryAddress))).thenReturn("validation.error");

		final boolean result = service.processOneClickTokenData(GUARANTEED_INVOICE, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(novalnetPaymentService).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldStoreCreditCardCoreFieldsAndReturnTrue() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();

		verify(sessionService).setAttribute(ATTR_CC_PAN_HASH, PAN_HASH);
		verify(sessionService).setAttribute(ATTR_CC_UNIQUE_ID, UNIQUE_ID);
		verify(sessionService).setAttribute(ATTR_CC_DO_REDIRECT, DO_REDIRECT);
		verify(novalnetPaymentService, never()).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldAddPaymentProcessWhenCreditCardPanHashEmptyAndOneClickDisabled() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn("");
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(novalnetPaymentService).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldNotAddPaymentProcessWhenCreditCardPanHashPresent() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(novalnetPaymentService, never()).addPaymentProcess(model, form, cartData);
	}

	@Test
	void shouldStoreFirstTokenWhenCreditCardOneClickEligibleAndSelectionOne() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);
		when(form.getCreditCardOneClickData1()).thenReturn("1");
		when(sessionService.getAttribute("novalnetCreditCardOneClickToken1")).thenReturn(CC_TOKEN_1);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_CC_TOKEN, CC_TOKEN_1);
	}

	@Test
	void shouldStoreSecondTokenWhenCreditCardOneClickEligibleAndSelectionTwo() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);
		when(form.getCreditCardOneClickData1()).thenReturn("2");
		when(sessionService.getAttribute("novalnetCreditCardOneClickToken2")).thenReturn(CC_TOKEN_2);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_CC_TOKEN, CC_TOKEN_2);
	}

	@Test
	void shouldStorePaymentDataPreferenceWhenCreditCardSelectionThreeAndSaveDataTrue() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);
		when(form.getCreditCardOneClickData1()).thenReturn("3");
		when(form.isCreditcardSaveData()).thenReturn(true);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_CC_STORE_PAYMENT_DATA, true);
	}

	@Test
	void shouldStorePaymentDataPreferenceWhenCreditCardOneClickDataEmptyAndSaveDataTrue() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(true);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);
		when(form.getCreditCardOneClickData1()).thenReturn("");
		when(form.isCreditcardSaveData()).thenReturn(true);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService).setAttribute(ATTR_CC_STORE_PAYMENT_DATA, true);
	}

	@Test
	void shouldNotSetTokenWhenCreditCardNotOneClickEligible() throws CMSItemNotFoundException
	{
		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD)).thenReturn(creditCardPaymentMode);
		when(creditCardPaymentMode.getNovalnetOneClickShopping()).thenReturn(false);
		when(form.getNovalnetCreditCardPanHash()).thenReturn(PAN_HASH);
		when(form.getNovalnetCreditCardUniqueId()).thenReturn(UNIQUE_ID);
		when(form.getDo_redirect()).thenReturn(DO_REDIRECT);

		final boolean result = service.processOneClickTokenData(CREDIT_CARD, form, model, cartData, deliveryAddress);

		assertThat(result).isTrue();
		verify(sessionService, never()).setAttribute(eq(ATTR_CC_TOKEN), any());
	}
}


