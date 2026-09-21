/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.servicelayer.session.SessionService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.util.NovalnetUtils;


@UnitTest
@ExtendWith(MockitoExtension.class)
class NovalnetGuaranteeValidationServiceTest
{
	private static final String PAYMENT_NAME = "novalnetGuaranteedDirectDebitSepa";
	private static final String DOB_VALID = "1990-01-01";
	private static final String STREET_1 = "Main Street 1";
	private static final String STREET_2 = "Apt 4";
	private static final String POSTAL_CODE = "12345";
	private static final String CITY = "Munich";
	private static final String COUNTRY_ISOCODE = "DE";
	private static final String GUARANTEE_ERROR_KEY = "novalnet.guarantee.declined";
	private static final String ADDRESS_ERROR_KEY = "novalnet.address.error";
	private static final String DOB_ERROR_KEY = "novalnet.dob.error";
	private static final String AGE_ERROR_KEY = "novalnet.age.error";
	private static final String EMPTY = "";

	private static final String ATTR_GUARANTEE_ERROR_SUFFIX = "GuaranteeError";
	private static final String ATTR_DATE_OF_BIRTH_SUFFIX = "DateOfBirth";
	private static final String ATTR_PAYMENT_GUARANTEE_SUFFIX = "PaymentGuarantee";

	private NovalnetGuaranteeValidationService service;

	@Mock
	private SessionService sessionService;

	@Mock
	private NovalnetPaymentDetailsForm paymentDetailsForm;

	@Mock
	private AddressData deliveryAddress;

	@Mock
	private CountryData deliveryCountry;

	private MockedStatic<NovalnetUtils> novalnetUtilsMock;

	@BeforeEach
	void setUp()
	{
		service = new NovalnetGuaranteeValidationService();
		ReflectionTestUtils.setField(service, "sessionService", sessionService);

		novalnetUtilsMock = mockStatic(NovalnetUtils.class);
	}

	@AfterEach
	void tearDown()
	{
		novalnetUtilsMock.close();
	}

	private void stubMatchingBillingAddress()
	{
		when(paymentDetailsForm.getBillTo_street1()).thenReturn(STREET_1);
		when(paymentDetailsForm.getBillTo_street2()).thenReturn(STREET_2);
		when(paymentDetailsForm.getBillTo_postalCode()).thenReturn(POSTAL_CODE);
		when(paymentDetailsForm.getBillTo_city()).thenReturn(CITY);
		when(paymentDetailsForm.getBillTo_country()).thenReturn(COUNTRY_ISOCODE);

		when(deliveryAddress.getLine1()).thenReturn(STREET_1);
		when(deliveryAddress.getLine2()).thenReturn(STREET_2);
		when(deliveryAddress.getPostalCode()).thenReturn(POSTAL_CODE);
		when(deliveryAddress.getTown()).thenReturn(CITY);
		when(deliveryAddress.getCountry()).thenReturn(deliveryCountry);
		when(deliveryCountry.getIsocode()).thenReturn(COUNTRY_ISOCODE);
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenPaymentDetailsFormIsNull()
	{
		assertThatIllegalArgumentException()
				.isThrownBy(() -> service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, null, deliveryAddress))
				.withMessage("Payment details are required.");

		verify(sessionService, never()).getAttribute(anyString());
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenDeliveryAddressIsNull()
	{
		assertThatIllegalArgumentException()
				.isThrownBy(() -> service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, null))
				.withMessage("Delivery address is required.");

		verify(sessionService, never()).getAttribute(anyString());
	}

	@Test
	void shouldReturnAddressErrorWhenNotUsingDeliveryAddressAndBillingAddressMismatches()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(false);
		when(paymentDetailsForm.getBillTo_street1()).thenReturn("Different Street");
		when(deliveryAddress.getLine1()).thenReturn(STREET_1);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEqualTo(ADDRESS_ERROR_KEY);
		verify(sessionService, never()).getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX);
	}

	@Test
	void shouldProceedPastAddressCheckWhenNotUsingDeliveryAddressAndBillingAddressMatches()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(false);
		stubMatchingBillingAddress();
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(null);
		novalnetUtilsMock.when(() -> NovalnetUtils.hasAgeRequirement(DOB_VALID)).thenReturn(true);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEmpty();
	}

	@Test
	void shouldSkipAddressCheckWhenUsingDeliveryAddress()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(null);
		novalnetUtilsMock.when(() -> NovalnetUtils.hasAgeRequirement(DOB_VALID)).thenReturn(true);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEmpty();
		verify(paymentDetailsForm, never()).getBillTo_street1();
	}

	@Test
	void shouldReturnStoredGuaranteeErrorWhenPresentInSession()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(GUARANTEE_ERROR_KEY);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEqualTo(GUARANTEE_ERROR_KEY);
		novalnetUtilsMock.verifyNoInteractions();
	}

	@Test
	void shouldReturnDobErrorWhenDateOfBirthIsEmpty()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(null);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, EMPTY, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEqualTo(DOB_ERROR_KEY);
		novalnetUtilsMock.verifyNoInteractions();
	}

	@Test
	void shouldReturnAgeErrorWhenDateOfBirthFailsAgeRequirement()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(null);
		novalnetUtilsMock.when(() -> NovalnetUtils.hasAgeRequirement(DOB_VALID)).thenReturn(false);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEqualTo(AGE_ERROR_KEY);
		verify(sessionService, never()).setAttribute(PAYMENT_NAME + ATTR_DATE_OF_BIRTH_SUFFIX, DOB_VALID);
	}

	@Test
	void shouldStoreDateOfBirthAndReturnEmptyWhenAgeRequirementMet()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(null);
		novalnetUtilsMock.when(() -> NovalnetUtils.hasAgeRequirement(DOB_VALID)).thenReturn(true);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEmpty();
		verify(sessionService).setAttribute(PAYMENT_NAME + ATTR_DATE_OF_BIRTH_SUFFIX, DOB_VALID);
		verify(sessionService).setAttribute(PAYMENT_NAME + ATTR_PAYMENT_GUARANTEE_SUFFIX, true);
	}

	@Test
	void shouldTrimDateOfBirthBeforeStoringInSession()
	{
		final String dobWithWhitespace = "  " + DOB_VALID + "  ";
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(true);
		when(sessionService.<String> getAttribute(PAYMENT_NAME + ATTR_GUARANTEE_ERROR_SUFFIX)).thenReturn(null);
		novalnetUtilsMock.when(() -> NovalnetUtils.hasAgeRequirement(dobWithWhitespace)).thenReturn(true);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, dobWithWhitespace, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEmpty();
		verify(sessionService).setAttribute(PAYMENT_NAME + ATTR_DATE_OF_BIRTH_SUFFIX, DOB_VALID);
	}

	@Test
	void shouldReturnAddressErrorWhenDeliveryCountryIsNull()
	{
		when(paymentDetailsForm.isUseDeliveryAddress()).thenReturn(false);
		when(paymentDetailsForm.getBillTo_street1()).thenReturn(STREET_1);
		when(paymentDetailsForm.getBillTo_street2()).thenReturn(STREET_2);
		when(paymentDetailsForm.getBillTo_postalCode()).thenReturn(POSTAL_CODE);
		when(paymentDetailsForm.getBillTo_city()).thenReturn(CITY);
		when(paymentDetailsForm.getBillTo_country()).thenReturn(COUNTRY_ISOCODE);

		when(deliveryAddress.getLine1()).thenReturn(STREET_1);
		when(deliveryAddress.getLine2()).thenReturn(STREET_2);
		when(deliveryAddress.getPostalCode()).thenReturn(POSTAL_CODE);
		when(deliveryAddress.getTown()).thenReturn(CITY);
		when(deliveryAddress.getCountry()).thenReturn(null);

		final String result = service.handleGuaranteeProcess(PAYMENT_NAME, DOB_VALID, paymentDetailsForm, deliveryAddress);

		assertThat(result).isEqualTo(ADDRESS_ERROR_KEY);
	}
}

