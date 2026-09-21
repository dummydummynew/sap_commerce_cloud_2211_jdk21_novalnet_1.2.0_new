/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.facades.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.store.BaseStoreModel;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.service.payment.NovalnetPaymentMethodService;

import jakarta.servlet.http.HttpServletRequest;



@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetPaymentFacadeTest
{
	private static final String PRODUCT_ACTIVATION_KEY = "productActivationKey123";
	private static final String CURRENT_PAYMENT = "NOVALNET_CC";
	private static final String CUSTOMER_NO = "customer123";
	private static final Integer ORDER_AMOUNT_CENT = 1000;
	private static final String RESPONSE = "SUCCESS";
	private static final String ERROR_MESSAGE = "Novalnet payment processing failed";

	@Mock
	private NovalnetPaymentMethodService novalnetPaymentMethodService;

	@Mock
	private Model model;

	@Mock
	private NovalnetPaymentDetailsForm novalnetPaymentDetailsForm;

	@Mock
	private CartData cartData;

	@Mock
	private Customer customer;

	@Mock
	private AddressData addressData;

	@Mock
	private BaseStoreModel baseStoreModel;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private StringBuilder stringBuilderResult;

	@InjectMocks
	private DefaultNovalnetPaymentFacade novalnetPaymentFacade;


	@Test
	public void shouldReturnServiceResponseWhenCallNovalnetMerchantDetailsSucceeds() throws Exception
	{
		given(novalnetPaymentMethodService.callNovalnetMerchantDetails(PRODUCT_ACTIVATION_KEY, baseStoreModel))
				.willReturn(RESPONSE);

		String result = novalnetPaymentFacade.callNovalnetMerchantDetails(PRODUCT_ACTIVATION_KEY, baseStoreModel);

		assertThat(result).isEqualTo(RESPONSE);

		verify(novalnetPaymentMethodService).callNovalnetMerchantDetails(PRODUCT_ACTIVATION_KEY, baseStoreModel);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldPropagateExceptionWhenCallNovalnetMerchantDetailsFails() throws Exception
	{
		willThrow(new Exception(ERROR_MESSAGE)).given(novalnetPaymentMethodService)
				.callNovalnetMerchantDetails(PRODUCT_ACTIVATION_KEY, baseStoreModel);

		assertThatThrownBy(() -> novalnetPaymentFacade.callNovalnetMerchantDetails(PRODUCT_ACTIVATION_KEY, baseStoreModel))
				.isInstanceOf(Exception.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetPaymentMethodService).callNovalnetMerchantDetails(PRODUCT_ACTIVATION_KEY, baseStoreModel);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldDelegateToServiceWhenAddPaymentProcessSucceeds() throws CMSItemNotFoundException
	{
		willDoNothing().given(novalnetPaymentMethodService).addPaymentProcess(model, novalnetPaymentDetailsForm, cartData);

		assertThatCode(() -> novalnetPaymentFacade.addPaymentProcess(model, novalnetPaymentDetailsForm, cartData))
				.doesNotThrowAnyException();

		verify(novalnetPaymentMethodService).addPaymentProcess(model, novalnetPaymentDetailsForm, cartData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldPropagateExceptionWhenAddPaymentProcessFails() throws CMSItemNotFoundException
	{
		willThrow(new CMSItemNotFoundException(ERROR_MESSAGE)).given(novalnetPaymentMethodService).addPaymentProcess(model,
				novalnetPaymentDetailsForm, cartData);

		assertThatThrownBy(() -> novalnetPaymentFacade.addPaymentProcess(model, novalnetPaymentDetailsForm, cartData))
				.isInstanceOf(CMSItemNotFoundException.class).hasMessageContaining(ERROR_MESSAGE);

		verify(novalnetPaymentMethodService).addPaymentProcess(model, novalnetPaymentDetailsForm, cartData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldDelegateToServiceWhenPopulateCustomerAddressDetailsSucceeds()
	{
		willDoNothing().given(novalnetPaymentMethodService).populateCustomerAddressDetails(model, novalnetPaymentDetailsForm,
				cartData, customer, addressData);

		assertThatCode(() -> novalnetPaymentFacade.populateCustomerAddressDetails(model, novalnetPaymentDetailsForm, cartData,
				customer, addressData)).doesNotThrowAnyException();

		verify(novalnetPaymentMethodService).populateCustomerAddressDetails(model, novalnetPaymentDetailsForm, cartData, customer,
				addressData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldPropagateExceptionWhenPopulateCustomerAddressDetailsFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetPaymentMethodService)
				.populateCustomerAddressDetails(model, novalnetPaymentDetailsForm, cartData, customer, addressData);

		assertThatThrownBy(() -> novalnetPaymentFacade.populateCustomerAddressDetails(model, novalnetPaymentDetailsForm, cartData,
				customer, addressData)).isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetPaymentMethodService).populateCustomerAddressDetails(model, novalnetPaymentDetailsForm, cartData, customer,
				addressData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldReturnTrueWhenProcessOneClickTokenDataSucceeds() throws CMSItemNotFoundException
	{
		given(novalnetPaymentMethodService.processOneClickTokenData(CURRENT_PAYMENT, novalnetPaymentDetailsForm, model, cartData,
				addressData)).willReturn(true);

		boolean result = novalnetPaymentFacade.processOneClickTokenData(CURRENT_PAYMENT, novalnetPaymentDetailsForm, model,
				cartData, addressData);

		assertThat(result).isTrue();

		verify(novalnetPaymentMethodService).processOneClickTokenData(CURRENT_PAYMENT, novalnetPaymentDetailsForm, model, cartData,
				addressData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldReturnFalseWhenProcessOneClickTokenDataFails() throws CMSItemNotFoundException
	{
		given(novalnetPaymentMethodService.processOneClickTokenData(CURRENT_PAYMENT, novalnetPaymentDetailsForm, model, cartData,
				addressData)).willReturn(false);

		boolean result = novalnetPaymentFacade.processOneClickTokenData(CURRENT_PAYMENT, novalnetPaymentDetailsForm, model,
				cartData, addressData);

		assertThat(result).isFalse();

		verify(novalnetPaymentMethodService).processOneClickTokenData(CURRENT_PAYMENT, novalnetPaymentDetailsForm, model, cartData,
				addressData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldReturnTrueWhenProcessTransactionSucceeds()
	{
		Map<String, String> resultMap = new HashMap<>();

		given(novalnetPaymentMethodService.processTransaction(resultMap)).willReturn(true);

		boolean result = novalnetPaymentFacade.processTransaction(resultMap);

		assertThat(result).isTrue();

		verify(novalnetPaymentMethodService).processTransaction(resultMap);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldReturnFalseWhenProcessTransactionFails()
	{
		Map<String, String> resultMap = new HashMap<>();

		given(novalnetPaymentMethodService.processTransaction(resultMap)).willReturn(false);

		boolean result = novalnetPaymentFacade.processTransaction(resultMap);

		assertThat(result).isFalse();

		verify(novalnetPaymentMethodService).processTransaction(resultMap);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldReturnServiceResultWhenCreateTransactionSucceeds()
	{
		given(novalnetPaymentMethodService.createTransaction(httpServletRequest, baseStoreModel, CURRENT_PAYMENT, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData)).willReturn(stringBuilderResult);

		StringBuilder result = novalnetPaymentFacade.createTransaction(httpServletRequest, baseStoreModel, CURRENT_PAYMENT,
				CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData);

		assertThat(result).isEqualTo(stringBuilderResult);

		verify(novalnetPaymentMethodService).createTransaction(httpServletRequest, baseStoreModel, CURRENT_PAYMENT, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldPropagateExceptionWhenCreateTransactionFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetPaymentMethodService)
				.createTransaction(httpServletRequest, baseStoreModel, CURRENT_PAYMENT, CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData);

		assertThatThrownBy(() -> novalnetPaymentFacade.createTransaction(httpServletRequest, baseStoreModel, CURRENT_PAYMENT,
				CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData)).isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetPaymentMethodService).createTransaction(httpServletRequest, baseStoreModel, CURRENT_PAYMENT, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldReturnServiceResponseWhenBookWalletTransactionSucceeds() throws Exception
	{
		given(novalnetPaymentMethodService.bookWalletTransaction(httpServletRequest, baseStoreModel, CUSTOMER_NO, ORDER_AMOUNT_CENT,
				cartData)).willReturn(RESPONSE);

		String result = novalnetPaymentFacade.bookWalletTransaction(httpServletRequest, baseStoreModel, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);

		assertThat(result).isEqualTo(RESPONSE);

		verify(novalnetPaymentMethodService).bookWalletTransaction(httpServletRequest, baseStoreModel, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);
		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}


	@Test
	public void shouldPropagateExceptionWhenBookWalletTransactionFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetPaymentMethodService)
				.bookWalletTransaction(httpServletRequest, baseStoreModel, CUSTOMER_NO, ORDER_AMOUNT_CENT, cartData);

		assertThatThrownBy(() -> novalnetPaymentFacade.bookWalletTransaction(httpServletRequest, baseStoreModel, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData)).isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetPaymentMethodService).bookWalletTransaction(httpServletRequest, baseStoreModel, CUSTOMER_NO,
				ORDER_AMOUNT_CENT, cartData);

		verifyNoMoreInteractions(novalnetPaymentMethodService);
	}

}

