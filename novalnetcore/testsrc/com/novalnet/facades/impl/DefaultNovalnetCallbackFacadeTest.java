/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.facades.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.service.payment.NovalnetCallbackService;

import de.novalnet.beans.NnCallbackRequestData;

import jakarta.servlet.http.HttpServletRequest;



@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetCallbackFacadeTest
{
	private static final String EXPECTED_RESPONSE = "SUCCESS";
	private static final String ERROR_MESSAGE = "Novalnet callback processing failed";

	@Mock
	private NovalnetCallbackService novalnetCallbackService;

	@Mock
	private NnCallbackRequestData nnCallbackRequestData;

	@Mock
	private HttpServletRequest httpServletRequest;

	@InjectMocks
	private DefaultNovalnetCallbackFacade novalnetCallbackFacade;


	@Test
	public void shouldReturnServiceResponseWhenProcessCallbackSucceeds()
	{
		given(novalnetCallbackService.processCallback(nnCallbackRequestData, httpServletRequest)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.processCallback(nnCallbackRequestData, httpServletRequest);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).processCallback(nnCallbackRequestData, httpServletRequest);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenProcessCallbackFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService).processCallback(nnCallbackRequestData,
				httpServletRequest);

		assertThatThrownBy(() -> novalnetCallbackFacade.processCallback(nnCallbackRequestData, httpServletRequest))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).processCallback(nnCallbackRequestData, httpServletRequest);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandleTransactionCaptureSucceeds()
	{
		given(novalnetCallbackService.handleTransactionCapture(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleTransactionCapture(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleTransactionCapture(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandleTransactionCaptureFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService)
				.handleTransactionCapture(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleTransactionCapture(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleTransactionCapture(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandleTransactionCancelSucceeds()
	{
		given(novalnetCallbackService.handleTransactionCancel(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleTransactionCancel(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleTransactionCancel(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandleTransactionCancelFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService)
				.handleTransactionCancel(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleTransactionCancel(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleTransactionCancel(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandleTransactionUpdateSucceeds()
	{
		given(novalnetCallbackService.handleTransactionUpdate(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleTransactionUpdate(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleTransactionUpdate(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandleTransactionUpdateFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService)
				.handleTransactionUpdate(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleTransactionUpdate(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleTransactionUpdate(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandlePaymentSucceeds()
	{
		given(novalnetCallbackService.handlePayment(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handlePayment(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handlePayment(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandlePaymentFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService).handlePayment(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handlePayment(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handlePayment(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandleCreditSucceeds()
	{
		given(novalnetCallbackService.handleCredit(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleCredit(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleCredit(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandleCreditFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService).handleCredit(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleCredit(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleCredit(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandleRefundSucceeds()
	{
		given(novalnetCallbackService.handleRefund(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleRefund(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleRefund(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandleRefundFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService).handleRefund(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleRefund(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleRefund(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldReturnServiceResponseWhenHandleReminderSucceeds()
	{
		given(novalnetCallbackService.handleReminder(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleReminder(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleReminder(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}


	@Test
	public void shouldPropagateExceptionWhenHandleReminderFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService).handleReminder(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleReminder(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleReminder(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}

	@Test
	public void shouldReturnServiceResponseWhenHandleCollectionSucceeds()
	{
		given(novalnetCallbackService.handleCollection(nnCallbackRequestData)).willReturn(EXPECTED_RESPONSE);

		String result = novalnetCallbackFacade.handleCollection(nnCallbackRequestData);

		assertThat(result).isEqualTo(EXPECTED_RESPONSE);
		verify(novalnetCallbackService).handleCollection(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}

	@Test
	public void shouldPropagateExceptionWhenHandleCollectionFails()
	{
		willThrow(new IllegalStateException(ERROR_MESSAGE)).given(novalnetCallbackService).handleCollection(nnCallbackRequestData);

		assertThatThrownBy(() -> novalnetCallbackFacade.handleCollection(nnCallbackRequestData))
				.isInstanceOf(IllegalStateException.class).hasMessage(ERROR_MESSAGE);

		verify(novalnetCallbackService).handleCollection(nnCallbackRequestData);
		verifyNoMoreInteractions(novalnetCallbackService);
	}
}

