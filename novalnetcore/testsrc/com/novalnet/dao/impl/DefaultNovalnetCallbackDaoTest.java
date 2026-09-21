/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetCallbackDaoTest
{
	private static final String ORDER_CODE = "100001";
	private static final String TRANSACTION_ID = "123456789";
	private static final String CART_CODE = "cart-100001";

	@Mock
	private FlexibleSearchService flexibleSearchService;

	@Mock
	private SearchResult<OrderModel> orderSearchResult;

	@Mock
	private SearchResult<OrderModel> cartOrderSearchResult;

	@Mock
	private NovalnetPaymentInfoModel paymentInfoModel;

	@Mock
	private NovalnetCallbackInfoModel callbackInfoModel;

	@Mock
	private OrderModel orderModel;

	@InjectMocks
	private DefaultNovalnetCallbackDao dao;


	@Test
	public void findOrderByCodeShouldReturnOrderWhenOrderExists()
	{
		when(orderSearchResult.getResult()).thenReturn(Collections.singletonList(orderModel));
		doReturn(orderSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = dao.findOrderByCode(ORDER_CODE);

		assertSame(orderModel, result);
	}


	@Test
	public void findOrderByCodeShouldReturnNullWhenOrderDoesNotExist()
	{
		when(orderSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(orderSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		OrderModel result = dao.findOrderByCode(ORDER_CODE);

		assertNull(result);
		verify(flexibleSearchService).search(any(FlexibleSearchQuery.class));
	}

	@Test
	public void getLatestNovalnetPaymentInfoShouldReturnPaymentInfoWhenExists()
	{
		when(flexibleSearchService.searchUnique(any(FlexibleSearchQuery.class))).thenReturn(paymentInfoModel);

		NovalnetPaymentInfoModel result = dao.getLatestNovalnetPaymentInfo(ORDER_CODE);

		assertSame(paymentInfoModel, result);
		verify(flexibleSearchService).searchUnique(any(FlexibleSearchQuery.class));
	}


	@Test
	public void getLatestNovalnetPaymentInfoShouldReturnNullWhenNotExists()
	{
		when(flexibleSearchService.searchUnique(any(FlexibleSearchQuery.class))).thenReturn(null);

		NovalnetPaymentInfoModel result = dao.getLatestNovalnetPaymentInfo(ORDER_CODE);

		assertNull(result);
		verify(flexibleSearchService).searchUnique(any(FlexibleSearchQuery.class));
	}


	@Test
	public void findCallbackInfoByOriginalTidShouldReturnCallbackInfoWhenExists()
	{
		when(flexibleSearchService.searchUnique(any(FlexibleSearchQuery.class))).thenReturn(callbackInfoModel);

		NovalnetCallbackInfoModel result = dao.findCallbackInfoByOriginalTid(TRANSACTION_ID);

		assertSame(callbackInfoModel, result);
		verify(flexibleSearchService).searchUnique(any(FlexibleSearchQuery.class));
	}


	@Test
	public void findCallbackInfoByOriginalTidShouldReturnNullWhenNotExists()
	{
		when(flexibleSearchService.searchUnique(any(FlexibleSearchQuery.class))).thenReturn(null);

		NovalnetCallbackInfoModel result = dao.findCallbackInfoByOriginalTid(TRANSACTION_ID);

		assertNull(result);
		verify(flexibleSearchService).searchUnique(any(FlexibleSearchQuery.class));
	}


	@Test
	public void isOrderCreatedForCartShouldReturnTrueWhenOrderExists()
	{
		when(cartOrderSearchResult.getResult()).thenReturn(Collections.singletonList(orderModel));
		doReturn(cartOrderSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		boolean result = dao.isOrderCreatedForCart(CART_CODE);

		assertTrue(result);
		verify(flexibleSearchService).search(any(FlexibleSearchQuery.class));
	}


	@Test
	public void isOrderCreatedForCartShouldReturnFalseWhenOrderDoesNotExist()
	{
		when(cartOrderSearchResult.getResult()).thenReturn(Collections.emptyList());
		doReturn(cartOrderSearchResult).when(flexibleSearchService).search(any(FlexibleSearchQuery.class));

		boolean result = dao.isOrderCreatedForCart(CART_CODE);

		assertFalse(result);
		verify(flexibleSearchService).search(any(FlexibleSearchQuery.class));
	}
}

