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
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.service.payment.NovalnetTransactionBookingService;
import com.novalnet.service.payment.NovalnetTransactionService;



@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetTransactionFacadeTest
{
	private static final String ERROR_MESSAGE = "Novalnet transaction processing failed";

	@Mock
	private NovalnetTransactionService novalnetTransactionService;

	@Mock
	private NovalnetTransactionBookingService novalnetTransactionBookingService;

	@Mock
	private OrderModel orderModel;

	@Mock
	private ReturnRequestModel returnRequestModel;

	@Mock
	private FollowOnRefundRequest followOnRefundRequest;

	@Mock
	private NovalnetTransactionResult novalnetTransactionResult;

	@InjectMocks
	private DefaultNovalnetTransactionFacade novalnetTransactionFacade;


	@Test
	public void shouldReturnServiceResultWhenCancelOrderSucceeds()
	{
		given(novalnetTransactionService.cancelOrder(orderModel)).willReturn(novalnetTransactionResult);

		NovalnetTransactionResult result = novalnetTransactionFacade.cancelOrder(orderModel);

		assertThat(result).isEqualTo(novalnetTransactionResult);
		verify(novalnetTransactionService).cancelOrder(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldPropagateExceptionWhenCancelOrderFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetTransactionService).cancelOrder(orderModel);

		assertThatThrownBy(() -> novalnetTransactionFacade.cancelOrder(orderModel)).isInstanceOf(IllegalStateException.class)
				.hasMessage(ERROR_MESSAGE);

		verify(novalnetTransactionService).cancelOrder(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}

	@Test
	public void shouldReturnTrueWhenCanCancelIsAllowed()
	{
		given(novalnetTransactionService.canCancel(orderModel)).willReturn(true);

		boolean result = novalnetTransactionFacade.canCancel(orderModel);

		assertThat(result).isTrue();
		verify(novalnetTransactionService).canCancel(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}

	@Test
	public void shouldReturnFalseWhenCanCancelIsNotAllowed()
	{
		given(novalnetTransactionService.canCancel(orderModel)).willReturn(false);

		boolean result = novalnetTransactionFacade.canCancel(orderModel);

		assertThat(result).isFalse();
		verify(novalnetTransactionService).canCancel(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnServiceResultWhenCaptureOrderSucceeds() throws JsonProcessingException
	{
		given(novalnetTransactionService.captureOrder(orderModel)).willReturn(novalnetTransactionResult);

		NovalnetTransactionResult result = novalnetTransactionFacade.captureOrder(orderModel);

		assertThat(result).isEqualTo(novalnetTransactionResult);
		verify(novalnetTransactionService).captureOrder(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldPropagateExceptionWhenCaptureOrderFails() throws JsonProcessingException
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetTransactionService).captureOrder(orderModel);

		assertThatThrownBy(() -> novalnetTransactionFacade.captureOrder(orderModel)).isInstanceOf(IllegalStateException.class)
				.hasMessage(ERROR_MESSAGE);

		verify(novalnetTransactionService).captureOrder(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnTrueWhenCanCaptureIsAllowed()
	{
		given(novalnetTransactionService.canCapture(orderModel)).willReturn(true);

		boolean result = novalnetTransactionFacade.canCapture(orderModel);

		assertThat(result).isTrue();
		verify(novalnetTransactionService).canCapture(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnFalseWhenCanCaptureIsNotAllowed()
	{
		given(novalnetTransactionService.canCapture(orderModel)).willReturn(false);

		boolean result = novalnetTransactionFacade.canCapture(orderModel);

		assertThat(result).isFalse();
		verify(novalnetTransactionService).canCapture(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnTrueWhenCanCreateReturnRequestIsAllowed()
	{
		given(novalnetTransactionService.canCreateReturnRequest(orderModel)).willReturn(true);

		boolean result = novalnetTransactionFacade.canCreateReturnRequest(orderModel);

		assertThat(result).isTrue();
		verify(novalnetTransactionService).canCreateReturnRequest(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnFalseWhenCanCreateReturnRequestIsNotAllowed()
	{
		given(novalnetTransactionService.canCreateReturnRequest(orderModel)).willReturn(false);

		boolean result = novalnetTransactionFacade.canCreateReturnRequest(orderModel);

		assertThat(result).isFalse();
		verify(novalnetTransactionService).canCreateReturnRequest(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnTrueWhenOrderIsReturnable()
	{
		given(novalnetTransactionService.isReturnable(orderModel)).willReturn(true);

		boolean result = novalnetTransactionFacade.isReturnable(orderModel);

		assertThat(result).isTrue();
		verify(novalnetTransactionService).isReturnable(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnFalseWhenOrderIsNotReturnable()
	{
		given(novalnetTransactionService.isReturnable(orderModel)).willReturn(false);

		boolean result = novalnetTransactionFacade.isReturnable(orderModel);

		assertThat(result).isFalse();
		verify(novalnetTransactionService).isReturnable(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnTrueWhenOrderIsRefundable()
	{
		given(novalnetTransactionService.isRefundable(orderModel)).willReturn(true);

		boolean result = novalnetTransactionFacade.isRefundable(orderModel);

		assertThat(result).isTrue();
		verify(novalnetTransactionService).isRefundable(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnFalseWhenOrderIsNotRefundable()
	{
		given(novalnetTransactionService.isRefundable(orderModel)).willReturn(false);

		boolean result = novalnetTransactionFacade.isRefundable(orderModel);

		assertThat(result).isFalse();
		verify(novalnetTransactionService).isRefundable(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnTrueWhenOrderIsFullyRefunded()
	{
		given(novalnetTransactionService.isFullyRefunded(orderModel)).willReturn(true);

		boolean result = novalnetTransactionFacade.isFullyRefunded(orderModel);

		assertThat(result).isTrue();
		verify(novalnetTransactionService).isFullyRefunded(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnFalseWhenOrderIsNotFullyRefunded()
	{
		given(novalnetTransactionService.isFullyRefunded(orderModel)).willReturn(false);

		boolean result = novalnetTransactionFacade.isFullyRefunded(orderModel);

		assertThat(result).isFalse();
		verify(novalnetTransactionService).isFullyRefunded(orderModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnServiceResultWhenRefundSucceeds()
	{
		given(novalnetTransactionService.refund(returnRequestModel)).willReturn(novalnetTransactionResult);

		NovalnetTransactionResult result = novalnetTransactionFacade.refund(returnRequestModel);

		assertThat(result).isEqualTo(novalnetTransactionResult);
		verify(novalnetTransactionService).refund(returnRequestModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldPropagateExceptionWhenRefundFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetTransactionService).refund(returnRequestModel);

		assertThatThrownBy(() -> novalnetTransactionFacade.refund(returnRequestModel)).isInstanceOf(IllegalStateException.class)
				.hasMessage(ERROR_MESSAGE);

		verify(novalnetTransactionService).refund(returnRequestModel);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldReturnServiceResultWhenProcessRefundSucceeds() throws JsonProcessingException
	{
		given(novalnetTransactionService.processRefund(followOnRefundRequest)).willReturn(novalnetTransactionResult);

		NovalnetTransactionResult result = novalnetTransactionFacade.processRefund(followOnRefundRequest);

		assertThat(result).isEqualTo(novalnetTransactionResult);
		verify(novalnetTransactionService).processRefund(followOnRefundRequest);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldPropagateExceptionWhenProcessRefundFails() throws JsonProcessingException
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetTransactionService).processRefund(followOnRefundRequest);

		assertThatThrownBy(() -> novalnetTransactionFacade.processRefund(followOnRefundRequest))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetTransactionService).processRefund(followOnRefundRequest);
		verifyNoMoreInteractions(novalnetTransactionService);
	}


	@Test
	public void shouldDelegateToBookingServiceWhenBookTransactionSucceeds() throws InterceptorException
	{
		willDoNothing().given(novalnetTransactionBookingService).bookTransaction(orderModel);
		assertThatCode(() -> novalnetTransactionFacade.bookTransaction(orderModel)).doesNotThrowAnyException();
		verify(novalnetTransactionBookingService).bookTransaction(orderModel);
		verifyNoMoreInteractions(novalnetTransactionBookingService);
	}


	@Test
	public void shouldPropagateInterceptorExceptionWhenBookTransactionFails() throws InterceptorException
	{
		willThrow(new InterceptorException(ERROR_MESSAGE)).given(novalnetTransactionBookingService).bookTransaction(orderModel);

		assertThatThrownBy(() -> novalnetTransactionFacade.bookTransaction(orderModel)).isInstanceOf(InterceptorException.class)
				.hasMessageContaining(ERROR_MESSAGE);

		verify(novalnetTransactionBookingService).bookTransaction(orderModel);
		verifyNoMoreInteractions(novalnetTransactionBookingService);
	}
}

