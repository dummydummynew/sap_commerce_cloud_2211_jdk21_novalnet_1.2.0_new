/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPaymentRefInfoModel;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetDaoTest
{
	private static final String ORDER_CODE = "100001";
	private static final String CUSTOMER_NO = "12345";
	private static final String PAYMENT_TYPE = "novalnetCreditCard";
	private static final String TRANSACTION_ID = "123456789";
	private static final String INVALID_NUMBER = "INVALID";
	private static final String PAYMENT_TOKEN = "TOKEN123";
	private static final String CURRENCY_ISO = "EUR";

	@Mock
	private FlexibleSearchService flexibleSearchService;

	@Mock
	private SearchResult<OrderModel> orderSearchResult;

	@Mock
	private SearchResult<NovalnetPaymentRefInfoModel> paymentRefSearchResult;

	@Mock
	private SearchResult<NovalnetPaymentInfoModel> paymentInfoSearchResult;

	@Mock
	private SearchResult<NovalnetCallbackInfoModel> callbackSearchResult;

	@Mock
	private SearchResult<NovalnetCallbackInfoModel> paymentDetailsSearchResult;

	@Mock
	private SearchResult<OrderModel> orderByTidSearchResult;

	@Mock
	private OrderModel orderModel;

	@Mock
	private NovalnetPaymentRefInfoModel paymentRefInfoModel;

	@Mock
	private NovalnetPaymentInfoModel paymentInfoModel;

	@Mock
	private NovalnetCallbackInfoModel callbackInfoModel;

	@Mock
	private CurrencyModel currencyModel;

	@Mock
	private UserModel userModel;

	@Mock
	private PK customerPk;

	@InjectMocks
	private DefaultNovalnetDao defaultNovalnetDao;


	@Test
	public void getOrderInfoModelShouldReturnOrdersWhenOrdersExist()
	{
		when(orderSearchResult.getResult()).thenReturn(Collections.singletonList(orderModel));
		doReturn(orderSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<OrderModel> result = defaultNovalnetDao.getOrderInfoModel(ORDER_CODE);

		assertEquals(1, result.size());
		assertSame(orderModel, result.get(0));
	}

	@Test
	public void getOrderInfoModelShouldReturnEmptyListWhenNoOrdersExist()
	{
		when(orderSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(orderSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<OrderModel> result = defaultNovalnetDao.getOrderInfoModel(ORDER_CODE);

		assertTrue(result.isEmpty());
	}

	@Test
	public void getPaymentRefInfoShouldReturnPaymentReferencesWhenTheyExist()
	{
		when(paymentRefSearchResult.getResult()).thenReturn(Collections.singletonList(paymentRefInfoModel));
		doReturn(paymentRefSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetPaymentRefInfoModel> result = defaultNovalnetDao.getPaymentRefInfo(CUSTOMER_NO, PAYMENT_TYPE);

		assertEquals(1, result.size());
		assertSame(paymentRefInfoModel, result.get(0));
	}


	@Test
	public void getPaymentRefInfoShouldReturnEmptyListWhenNonePaymentReferenceExists()
	{
		when(paymentRefSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(paymentRefSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetPaymentRefInfoModel> result = defaultNovalnetDao.getPaymentRefInfo(CUSTOMER_NO, PAYMENT_TYPE);

		assertTrue(result.isEmpty());
	}

	@Test
	public void getPaymentRefInfoShouldThrowExceptionWhenCustomerNumberIsInvalid()
	{
		assertThrows(NumberFormatException.class, () -> defaultNovalnetDao.getPaymentRefInfo(INVALID_NUMBER, PAYMENT_TYPE));
	}


	@Test
	public void getNovalnetPaymentInfoShouldReturnPaymentInfoWhenItExists()
	{
		when(paymentInfoSearchResult.getResult()).thenReturn(Collections.singletonList(paymentInfoModel));
		doReturn(paymentInfoSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetPaymentInfoModel> result = defaultNovalnetDao.getNovalnetPaymentInfo(ORDER_CODE);

		assertEquals(1, result.size());
		assertSame(paymentInfoModel, result.get(0));
	}


	@Test
	public void getNovalnetPaymentInfoShouldReturnEmptyListWhenNoneExists()
	{
		when(paymentInfoSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(paymentInfoSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetPaymentInfoModel> result = defaultNovalnetDao.getNovalnetPaymentInfo(ORDER_CODE);

		assertTrue(result.isEmpty());
	}


	@Test
	public void getCallbackInfoShouldReturnCallbackInfoWhenItExists()
	{
		when(callbackSearchResult.getResult()).thenReturn(Collections.singletonList(callbackInfoModel));
		doReturn(callbackSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetCallbackInfoModel> result = defaultNovalnetDao.getCallbackInfo(TRANSACTION_ID);

		assertEquals(1, result.size());
		assertSame(callbackInfoModel, result.get(0));
	}


	@Test
	public void getCallbackInfoShouldReturnEmptyListWhenNoneExists()
	{
		when(callbackSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(callbackSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetCallbackInfoModel> result = defaultNovalnetDao.getCallbackInfo(TRANSACTION_ID);

		assertTrue(result.isEmpty());
	}


	@Test
	public void getPaymentDetailsInfoShouldReturnCallbackInfoWhenItExists()
	{
		when(paymentDetailsSearchResult.getResult()).thenReturn(Collections.singletonList(callbackInfoModel));
		doReturn(paymentDetailsSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetCallbackInfoModel> result = defaultNovalnetDao.getPaymentDetailsInfo(ORDER_CODE);

		assertEquals(1, result.size());
		assertSame(callbackInfoModel, result.get(0));
	}


	@Test
	public void getPaymentDetailsInfoShouldReturnEmptyListWhenNoneExists()
	{
		when(paymentDetailsSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(paymentDetailsSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		List<NovalnetCallbackInfoModel> result = defaultNovalnetDao.getPaymentDetailsInfo(ORDER_CODE);

		assertTrue(result.isEmpty());
	}


	@Test
	public void getOrderByTidShouldReturnOrderWhenCallbackAndOrderExist()
	{
		when(callbackInfoModel.getOrderNo()).thenReturn(ORDER_CODE);
		when(callbackSearchResult.getResult()).thenReturn(Collections.singletonList(callbackInfoModel));
		when(orderByTidSearchResult.getResult()).thenReturn(Collections.singletonList(orderModel));
		doReturn(callbackSearchResult, orderByTidSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = defaultNovalnetDao.getOrderByTid(TRANSACTION_ID);

		assertSame(orderModel, result);
	}


	@Test
	public void getOrderByTidShouldReturnNullWhenCallbackDoesNotExist()
	{
		when(callbackSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(callbackSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = defaultNovalnetDao.getOrderByTid(TRANSACTION_ID);

		assertNull(result);
	}


	@Test
	public void getOrderByTidShouldReturnNullWhenCallbackOrderNumberIsNull()
	{
		when(callbackInfoModel.getOrderNo()).thenReturn(null);
		when(callbackSearchResult.getResult()).thenReturn(Collections.singletonList(callbackInfoModel));
		doReturn(callbackSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = defaultNovalnetDao.getOrderByTid(TRANSACTION_ID);

		assertNull(result);
	}


	@Test
	public void getOrderByTidShouldReturnNullWhenCallbackOrderNumberIsEmpty()
	{
		when(callbackInfoModel.getOrderNo()).thenReturn("");
		when(callbackSearchResult.getResult()).thenReturn(Collections.singletonList(callbackInfoModel));
		doReturn(callbackSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = defaultNovalnetDao.getOrderByTid(TRANSACTION_ID);

		assertNull(result);
	}


	@Test
	public void getOrderByTidShouldReturnNullWhenOrderDoesNotExist()
	{
		when(callbackInfoModel.getOrderNo()).thenReturn(ORDER_CODE);
		when(callbackSearchResult.getResult()).thenReturn(Collections.singletonList(callbackInfoModel));
		when(orderByTidSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(callbackSearchResult, orderByTidSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = defaultNovalnetDao.getOrderByTid(TRANSACTION_ID);

		assertNull(result);
	}


	@Test
	public void getOrderByTidShouldThrowExceptionWhenTransactionIdIsInvalid()
	{
		assertThrows(NumberFormatException.class, () -> defaultNovalnetDao.getOrderByTid(INVALID_NUMBER));
	}


	@Test
	public void getStoredPaymentTokenShouldReturnTokenWhenTokenExists()
	{
		when(orderModel.getUser()).thenReturn(userModel);
		when(orderModel.getPaymentInfo()).thenReturn(paymentInfoModel);
		when(userModel.getPk()).thenReturn(customerPk);
		when(customerPk.getLongValue()).thenReturn(Long.valueOf(CUSTOMER_NO));
		when(paymentInfoModel.getPaymentProvider()).thenReturn(PAYMENT_TYPE);
		when(paymentRefInfoModel.getToken()).thenReturn(PAYMENT_TOKEN);
		when(paymentRefSearchResult.getResult()).thenReturn(Collections.singletonList(paymentRefInfoModel));
		doReturn(paymentRefSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		String result = defaultNovalnetDao.getStoredPaymentToken(orderModel);

		assertEquals(PAYMENT_TOKEN, result);
	}


	@Test
	public void getStoredPaymentTokenShouldReturnNullWhenOrderIsNull()
	{
		String result = defaultNovalnetDao.getStoredPaymentToken(null);

		assertNull(result);
	}


	@Test
	public void getStoredPaymentTokenShouldReturnNullWhenUserIsNull()
	{
		when(orderModel.getUser()).thenReturn(null);

		String result = defaultNovalnetDao.getStoredPaymentToken(orderModel);

		assertNull(result);
	}


	@Test
	public void getStoredPaymentTokenShouldReturnNullWhenPaymentInfoIsNull()
	{
		when(orderModel.getUser()).thenReturn(userModel);
		when(orderModel.getPaymentInfo()).thenReturn(null);

		String result = defaultNovalnetDao.getStoredPaymentToken(orderModel);

		assertNull(result);
	}


	@Test
	public void getStoredPaymentTokenShouldReturnNullWhenSearchResultIsEmpty()
	{
		when(orderModel.getUser()).thenReturn(userModel);
		when(orderModel.getPaymentInfo()).thenReturn(paymentInfoModel);
		when(userModel.getPk()).thenReturn(customerPk);
		when(customerPk.getLongValue()).thenReturn(Long.valueOf(CUSTOMER_NO));
		when(paymentInfoModel.getPaymentProvider()).thenReturn(PAYMENT_TYPE);
		when(paymentRefSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(paymentRefSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		String result = defaultNovalnetDao.getStoredPaymentToken(orderModel);

		assertNull(result);
	}


	@Test
	public void getStoredPaymentTokenShouldReturnNullWhenSearchResultIsNull()
	{
		when(orderModel.getUser()).thenReturn(userModel);
		when(orderModel.getPaymentInfo()).thenReturn(paymentInfoModel);
		when(userModel.getPk()).thenReturn(customerPk);
		when(customerPk.getLongValue()).thenReturn(Long.valueOf(CUSTOMER_NO));
		when(paymentInfoModel.getPaymentProvider()).thenReturn(PAYMENT_TYPE);
		doReturn(null).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		String result = defaultNovalnetDao.getStoredPaymentToken(orderModel);

		assertNull(result);
	}

	@Test
	public void getCurrencyForIsoCodeShouldReturnCurrencyWhenItExists()
	{
		when(flexibleSearchService.getModelByExample(any(CurrencyModel.class))).thenReturn(currencyModel);

		CurrencyModel result = defaultNovalnetDao.getCurrencyForIsoCode(CURRENCY_ISO);

		assertSame(currencyModel, result);
		verify(flexibleSearchService).getModelByExample(any(CurrencyModel.class));
	}


	@Test
	public void getCurrencyForIsoCodeShouldReturnNullWhenCurrencyDoesNotExist()
	{
		when(flexibleSearchService.getModelByExample(any(CurrencyModel.class))).thenReturn(null);

		CurrencyModel result = defaultNovalnetDao.getCurrencyForIsoCode(CURRENCY_ISO);

		assertNull(result);
	}
}

