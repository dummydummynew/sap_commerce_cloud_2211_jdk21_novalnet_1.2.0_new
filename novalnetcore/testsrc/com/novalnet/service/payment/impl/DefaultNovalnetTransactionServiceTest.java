/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.novalnet.service.payment.impl;

import static de.hybris.platform.basecommerce.enums.ConsignmentStatus.PICKUP_COMPLETE;
import static de.hybris.platform.basecommerce.enums.ConsignmentStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.ordersplitting.model.ConsignmentModel;
import de.hybris.platform.payment.AdapterException;
import de.hybris.platform.payment.PaymentService;
import de.hybris.platform.payment.commands.request.FollowOnRefundRequest;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.localization.Localization;
import de.hybris.platform.warehousing.returns.service.RefundAmountCalculationService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.dto.NovalnetTransactionResult;
import com.novalnet.dto.OrderPaymentCommentsData;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentService;
import com.novalnet.util.NovalnetUtils;


@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultNovalnetTransactionServiceTest
{
	private static final String FIXED_DATE = "01-01-2026, 00:00:00";

	private static final long CANCEL_SUCCESS_TID = 123456L;
	private static final long CANCEL_FAILURE_TID = 123457L;
	private static final long CANCEL_EXCEPTION_TID = 123458L;
	private static final long CAPTURE_SUCCESS_TID = 223456L;
	private static final long CAPTURE_FAILURE_TID = 223457L;
	private static final long CAPTURE_EXCEPTION_TID = 223458L;
	private static final long CAPTURE_INVOICE_TID = 223460L;

	private static final double ORDER_TOTAL = 100.00;

	private static final BigDecimal REFUND_10 = new BigDecimal("10.00");
	private static final BigDecimal REFUND_20 = new BigDecimal("20.00");
	private static final BigDecimal REFUND_25 = new BigDecimal("25.00");
	private static final BigDecimal REFUND_30 = new BigDecimal("30.00");
	private static final BigDecimal REFUND_50 = new BigDecimal("50.00");
	private static final BigDecimal REFUND_100 = new BigDecimal("100.00");
	private static final BigDecimal REFUND_150 = new BigDecimal("150.00");

	private static final String TRANSACTION_ID = "123456";
	private static final String STATUS_CONFIRMED = "CONFIRMED";
	private static final String STATUS_ON_HOLD = "ON_HOLD";
	private static final String STATUS_ACCEPTED = "ACCEPTED";
	private static final String STATUS_REJECTED = "REJECTED";

	private DefaultNovalnetTransactionService service;

	@Mock
	private NovalnetPaymentService novalnetPaymentService;

	@Mock
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Mock
	private NovalnetDao novalnetDao;

	@Mock
	private SessionService sessionService;

	@Mock
	private NovalnetOrderService novalnetOrderService;

	@Mock
	private NovalnetApiService novalnetApiService;

	@Mock
	private ConfigurationService configurationService;

	@Mock
	private PaymentService paymentService;

	@Mock
	private RefundAmountCalculationService refundAmountCalculationService;

	@Mock
	private ModelService modelService;

	@BeforeEach
	public void setUp()
	{
		service = new DefaultNovalnetTransactionService();

		ReflectionTestUtils.setField(service, "novalnetPaymentService", novalnetPaymentService);
		ReflectionTestUtils.setField(service, "novalnetEndpointConfigService", novalnetEndpointConfigService);
		ReflectionTestUtils.setField(service, "novalnetDao", novalnetDao);
		ReflectionTestUtils.setField(service, "sessionService", sessionService);
		ReflectionTestUtils.setField(service, "novalnetOrderService", novalnetOrderService);
		ReflectionTestUtils.setField(service, "novalnetApiService", novalnetApiService);
		ReflectionTestUtils.setField(service, "configurationService", configurationService);
		ReflectionTestUtils.setField(service, "paymentService", paymentService);
		ReflectionTestUtils.setField(service, "refundAmountCalculationService", refundAmountCalculationService);
		ReflectionTestUtils.setField(service, "modelService", modelService);
	}

	private MockedStatic<Localization> mockLocalization()
	{
		MockedStatic<Localization> localizationMock = mockStatic(Localization.class);
		localizationMock.when(() -> Localization.getLocalizedString(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		return localizationMock;
	}

	private MockedStatic<NovalnetUtils> mockNovalnetUtilsCurrentDate()
	{
		MockedStatic<NovalnetUtils> utilsMock = mockStatic(NovalnetUtils.class, Mockito.CALLS_REAL_METHODS);
		utilsMock.when(NovalnetUtils::getCurrentDate).thenReturn(FIXED_DATE);
		return utilsMock;
	}

	private OrderModel orderWithCode(String code)
	{
		OrderModel order = mock(OrderModel.class);
		when(order.getCode()).thenReturn(code);
		return order;
	}

	@Test
	public void shouldReturnFalseWhenCanCancelOrderIsNull()
	{
		assertThat(service.canCancel(null)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenCanCancelPaymentModeIsNull()
	{
		OrderModel order = orderWithCode("ORDER-100");
		when(order.getPaymentMode()).thenReturn(null);

		assertThat(service.canCancel(order)).isFalse();

		verify(novalnetDao, never()).getNovalnetPaymentInfo(anyString());
	}

	@Test
	public void shouldReturnFalseWhenCanCancelPaymentModeIsNotNovalnet()
	{
		OrderModel order = orderWithCode("ORDER-101");
		PaymentModeModel paymentMode = mock(PaymentModeModel.class);

		when(order.getPaymentMode()).thenReturn(paymentMode);
		when(paymentMode.getCode()).thenReturn("creditcard");

		assertThat(service.canCancel(order)).isFalse();

		verify(novalnetDao, never()).getNovalnetPaymentInfo(anyString());
	}

	@Test
	public void shouldReturnTrueWhenCanCancelPaymentIsOnHold()
	{
		OrderModel order = orderWithCode("ORDER-102");
		PaymentModeModel paymentMode = mock(PaymentModeModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentMode()).thenReturn(paymentMode);
		when(paymentMode.getCode()).thenReturn("novalnetCreditCard");
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-102")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_ON_HOLD);

		assertThat(service.canCancel(order)).isTrue();
	}

	@Test
	public void shouldReturnFalseWhenCanCancelPaymentIsNotOnHold()
	{
		OrderModel order = orderWithCode("ORDER-103");
		PaymentModeModel paymentMode = mock(PaymentModeModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentMode()).thenReturn(paymentMode);
		when(paymentMode.getCode()).thenReturn("novalnetCreditCard");
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-103")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_CONFIRMED);

		assertThat(service.canCancel(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenCanCancelPaymentInfoIsNull()
	{
		OrderModel order = orderWithCode("ORDER-104");
		PaymentModeModel paymentMode = mock(PaymentModeModel.class);

		when(order.getPaymentMode()).thenReturn(paymentMode);
		when(paymentMode.getCode()).thenReturn("novalnetCreditCard");
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-104")).thenReturn(null);

		assertThat(service.canCancel(order)).isFalse();
	}

	@Test
	public void shouldReturnNotFoundWhenCancelOrderHasNoReference()
	{
		OrderModel order = orderWithCode("ORDER-105");

		when(novalnetDao.getPaymentDetailsInfo("ORDER-105")).thenReturn(Collections.emptyList());

		NovalnetTransactionResult result = service.cancelOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.notfound");
	}

	@Test
	public void shouldReturnNotFoundWhenCancelOrderReferenceIsNull()
	{
		OrderModel order = orderWithCode("ORDER-106");

		when(novalnetDao.getPaymentDetailsInfo("ORDER-106")).thenReturn(null);

		NovalnetTransactionResult result = service.cancelOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.notfound");
	}

	@Test
	public void shouldCancelOrderAndUpdatePaymentWhenApiSucceeds()
	{
		OrderModel order = orderWithCode("ORDER-107");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-107")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CANCEL_SUCCESS_TID);
		when(novalnetEndpointConfigService.getTransactionCancelUrl()).thenReturn("cancel-url");
		when(novalnetApiService.followupSendRequest(eq("cancel-url"), anyString(), eq(store))).thenReturn(new StringBuilder(
				"{\"result\":{\"status\":\"SUCCESS\"}," + "\"transaction\":{\"status\":\"CANCELLED\",\"order_no\":\"ORDER-107\"}}"));
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-107")).thenReturn(List.of(paymentInfo));

		NovalnetTransactionResult result;

		try (MockedStatic<Localization> localization = mockLocalization();
				MockedStatic<NovalnetUtils> utils = mockNovalnetUtilsCurrentDate())
		{
			result = service.cancelOrder(order);
		}

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getMessage()).isNotNull();
		assertThat(result.getMessage()).contains(FIXED_DATE);

		verify(novalnetPaymentService).updatePaymentInfo(List.of(paymentInfo), "CANCELLED");
		verify(novalnetPaymentService).updateCancelStatus("ORDER-107");
		verify(novalnetOrderService).updateCallbackComments(anyString(), eq("ORDER-107"), eq("CANCELLED"));
	}

	@Test
	public void shouldReturnFailureWhenCancelApiReturnsFailure()
	{
		OrderModel order = orderWithCode("ORDER-108");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-108")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CANCEL_FAILURE_TID);
		when(novalnetEndpointConfigService.getTransactionCancelUrl()).thenReturn("cancel-url");
		when(novalnetApiService.followupSendRequest(eq("cancel-url"), anyString(), eq(store)))
				.thenReturn(new StringBuilder("{\"result\":{\"status\":\"FAILURE\"," + "\"status_text\":\"Cancel failed\"}}"));

		NovalnetTransactionResult result = service.cancelOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Cancel failed");

		verify(novalnetPaymentService, never()).updateCancelStatus(anyString());
	}

	@Test
	public void shouldReturnFailureWhenCancelApiThrowsException()
	{
		OrderModel order = orderWithCode("ORDER-108b");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-108b")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CANCEL_EXCEPTION_TID);
		when(novalnetEndpointConfigService.getTransactionCancelUrl()).thenReturn("cancel-url");
		when(novalnetApiService.followupSendRequest(eq("cancel-url"), anyString(), eq(store)))
				.thenThrow(new RuntimeException("network error"));

		NovalnetTransactionResult result = service.cancelOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.cancel.failed");
	}

	@Test
	public void shouldReturnFalseWhenCanCaptureOrderIsNull()
	{
		assertThat(service.canCapture(null)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenCanCapturePaymentModeIsNull()
	{
		OrderModel order = orderWithCode("ORDER-109");

		when(order.getPaymentMode()).thenReturn(null);

		assertThat(service.canCapture(order)).isFalse();

		verify(novalnetDao, never()).getNovalnetPaymentInfo(anyString());
	}

	@Test
	public void shouldReturnTrueWhenCanCapturePaymentIsOnHold()
	{
		OrderModel order = orderWithCode("ORDER-110");
		PaymentModeModel paymentMode = mock(PaymentModeModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentMode()).thenReturn(paymentMode);
		when(paymentMode.getCode()).thenReturn("novalnetInvoice");
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-110")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_ON_HOLD);

		assertThat(service.canCapture(order)).isTrue();
	}

	@Test
	public void shouldReturnFalseWhenCanCaptureStatusIsConfirmed()
	{
		OrderModel order = orderWithCode("ORDER-111");
		PaymentModeModel paymentMode = mock(PaymentModeModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentMode()).thenReturn(paymentMode);
		when(paymentMode.getCode()).thenReturn("novalnetInvoice");
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-111")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_CONFIRMED);

		assertThat(service.canCapture(order)).isFalse();
	}

	@Test
	public void shouldReturnFailureWhenCaptureOrderIsNull() throws Exception
	{
		NovalnetTransactionResult result = service.captureOrder(null);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.null");
	}


	@Test
	public void shouldReturnFailureWhenCaptureOrderStoreIsNull() throws Exception
	{
		OrderModel order = orderWithCode("ORDER-113");

		when(order.getStore()).thenReturn(null);

		NovalnetTransactionResult result = service.captureOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.capture.failed");
	}

	@Test
	public void shouldReturnNotFoundWhenCaptureOrderHasNoPaymentReference() throws Exception
	{
		OrderModel order = orderWithCode("ORDER-114");
		BaseStoreModel store = mock(BaseStoreModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-114")).thenReturn(Collections.emptyList());

		NovalnetTransactionResult result = service.captureOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.notfound");
	}

	@Test
	public void shouldCaptureOrderWithoutDueDateWhenPaymentIsNotInvoice() throws Exception
	{
		OrderModel order = orderWithCode("ORDER-115");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-115")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CAPTURE_SUCCESS_TID);
		when(novalnetEndpointConfigService.getTransactionCaptureUrl()).thenReturn("capture-url");
		when(novalnetApiService.followupSendRequest(eq("capture-url"), anyString(), eq(store)))
				.thenReturn(new StringBuilder("{\"result\":{\"status\":\"SUCCESS\"}," + "\"transaction\":{\"status\":\"CONFIRMED\","
						+ "\"order_no\":\"ORDER-115\"," + "\"payment_type\":\"CREDITCARD\"}}"));
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-115")).thenReturn(List.of(paymentInfo));
		when(novalnetPaymentService.getPaymentModel(List.of(paymentInfo))).thenReturn(paymentInfo);

		NovalnetTransactionResult result;

		try (MockedStatic<Localization> localization = mockLocalization();
				MockedStatic<NovalnetUtils> utils = mockNovalnetUtilsCurrentDate())
		{
			result = service.captureOrder(order);
		}

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getMessage()).contains(FIXED_DATE);

		verify(novalnetPaymentService).updatePaymentInfo(List.of(paymentInfo), "CAPTURE");
		verify(novalnetOrderService).updateOrderStatus("ORDER-115", paymentInfo);
	}


	@Test
	public void shouldCaptureInvoicePaymentWithAmountAndDueDate() throws Exception
	{
		OrderModel order = orderWithCode("ORDER-115b");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-115b")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CAPTURE_INVOICE_TID);
		when(novalnetEndpointConfigService.getTransactionCaptureUrl()).thenReturn("capture-url");
		when(novalnetApiService.followupSendRequest(eq("capture-url"), anyString(), eq(store)))
				.thenReturn(new StringBuilder("{\"result\":{\"status\":\"SUCCESS\"}," + "\"transaction\":{\"status\":\"CONFIRMED\","
						+ "\"order_no\":\"ORDER-115b\"," + "\"payment_type\":\"INVOICE\","
						+ "\"amount\":9999,\"due_date\":\"2026-09-10\"}}"));
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-115b")).thenReturn(List.of(paymentInfo));
		when(novalnetPaymentService.getPaymentModel(List.of(paymentInfo))).thenReturn(paymentInfo);

		NovalnetTransactionResult result;

		try (MockedStatic<Localization> localization = mockLocalization();
				MockedStatic<NovalnetUtils> utils = mockNovalnetUtilsCurrentDate())
		{
			result = service.captureOrder(order);
		}

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getMessage()).contains("99.99");
		assertThat(result.getMessage()).contains("2026-09-10");
	}

	@Test
	public void shouldReturnFailureWhenCaptureApiReturnsFailure() throws Exception
	{
		OrderModel order = orderWithCode("ORDER-116");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-116")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CAPTURE_FAILURE_TID);
		when(novalnetEndpointConfigService.getTransactionCaptureUrl()).thenReturn("capture-url");
		when(novalnetApiService.followupSendRequest(eq("capture-url"), anyString(), eq(store)))
				.thenReturn(new StringBuilder("{\"result\":{\"status\":\"FAILURE\"," + "\"status_text\":\"Capture failed\"}}"));

		NovalnetTransactionResult result = service.captureOrder(order);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Capture failed");
	}


	@Test
	public void shouldThrowExceptionWhenCaptureApiThrowsException() throws Exception
	{
		OrderModel order = orderWithCode("ORDER-116b");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetCallbackInfoModel callbackInfo = mock(NovalnetCallbackInfoModel.class);

		when(order.getStore()).thenReturn(store);
		when(novalnetDao.getPaymentDetailsInfo("ORDER-116b")).thenReturn(List.of(callbackInfo));
		when(callbackInfo.getOrginalTid()).thenReturn(CAPTURE_EXCEPTION_TID);
		when(novalnetEndpointConfigService.getTransactionCaptureUrl()).thenReturn("capture-url");
		when(novalnetApiService.followupSendRequest(eq("capture-url"), anyString(), eq(store)))
				.thenThrow(new RuntimeException("network error"));

		assertThatThrownBy(() -> service.captureOrder(order)).isInstanceOf(RuntimeException.class).hasMessage("network error");
	}

	@Test
	public void shouldReturnFalseWhenIsReturnableOrderIsNull()
	{
		assertThat(service.isReturnable(null)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsReturnableConsignmentsAreNull()
	{
		OrderModel order = orderWithCode("ORDER-117");

		when(order.getConsignments()).thenReturn(null);
		when(order.getEntries()).thenReturn(Collections.emptyList());

		assertThat(service.isReturnable(order)).isFalse();
	}


	@Test
	public void shouldReturnFalseWhenIsReturnableEntriesAreNull()
	{
		OrderModel order = orderWithCode("ORDER-118");

		when(order.getConsignments()).thenReturn(Collections.emptySet());
		when(order.getEntries()).thenReturn(null);

		assertThat(service.isReturnable(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsReturnableHasNoConsignments()
	{
		OrderModel order = orderWithCode("ORDER-118b");

		when(order.getConsignments()).thenReturn(Collections.emptySet());
		when(order.getEntries()).thenReturn(Collections.emptyList());

		assertThat(service.isReturnable(order)).isFalse();
	}

	@Test
	public void shouldReturnTrueWhenIsReturnableConsignmentIsShipped()
	{
		OrderModel order = orderWithCode("ORDER-119");
		ConsignmentModel consignment = mock(ConsignmentModel.class);

		when(order.getConsignments()).thenReturn(Set.of(consignment));
		when(order.getEntries()).thenReturn(Collections.emptyList());
		when(consignment.getStatus()).thenReturn(SHIPPED);

		assertThat(service.isReturnable(order)).isTrue();
	}

	@Test
	public void shouldReturnTrueWhenIsReturnableConsignmentIsPickupComplete()
	{
		OrderModel order = orderWithCode("ORDER-120");
		ConsignmentModel consignment = mock(ConsignmentModel.class);

		when(order.getConsignments()).thenReturn(Set.of(consignment));
		when(order.getEntries()).thenReturn(Collections.emptyList());
		when(consignment.getStatus()).thenReturn(PICKUP_COMPLETE);

		assertThat(service.isReturnable(order)).isTrue();
	}

	@Test
	public void shouldReturnFalseWhenIsReturnableConsignmentStatusIsNull()
	{
		OrderModel order = orderWithCode("ORDER-121");
		ConsignmentModel consignment = mock(ConsignmentModel.class);

		when(order.getConsignments()).thenReturn(Set.of(consignment));
		when(order.getEntries()).thenReturn(Collections.emptyList());
		when(consignment.getStatus()).thenReturn(null);

		assertThat(service.isReturnable(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsRefundablePaymentInfoIsMissing()
	{
		OrderModel order = orderWithCode("ORDER-122");

		when(order.getPaymentInfo()).thenReturn(null);

		assertThat(service.isRefundable(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsRefundablePaymentInfoListIsNull()
	{
		OrderModel order = orderWithCode("ORDER-123");
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentInfo()).thenReturn(paymentInfo);
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-123")).thenReturn(null);

		assertThat(service.isRefundable(order)).isFalse();
	}

	@Test
	public void shouldReturnTrueWhenIsRefundablePaymentStatusIsConfirmed()
	{
		OrderModel order = orderWithCode("ORDER-125");
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentInfo()).thenReturn(paymentInfo);
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-125")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_CONFIRMED);

		assertThat(service.isRefundable(order)).isTrue();
	}

	@Test
	public void shouldReturnFalseWhenIsRefundablePaymentStatusIsNotConfirmed()
	{
		OrderModel order = orderWithCode("ORDER-126");
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentInfo()).thenReturn(paymentInfo);
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-126")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_ON_HOLD);

		assertThat(service.isRefundable(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsFullyRefundedHasNoTransactions()
	{
		OrderModel order = orderWithCode("ORDER-128");

		when(order.getPaymentTransactions()).thenReturn(Collections.emptyList());

		assertThat(service.isFullyRefunded(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsFullyRefundedAmountIsLessThanTotal()
	{
		OrderModel order = orderWithCode("ORDER-129");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel entry = mock(PaymentTransactionEntryModel.class);

		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(transaction.getEntries()).thenReturn(List.of(entry));
		when(entry.getType()).thenReturn(PaymentTransactionType.REFUND_FOLLOW_ON);
		when(entry.getTransactionStatus()).thenReturn(STATUS_ACCEPTED);
		when(entry.getAmount()).thenReturn(new BigDecimal("50.00"));
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);

		assertThat(service.isFullyRefunded(order)).isFalse();
	}

	@Test
	public void shouldReturnTrueWhenIsFullyRefundedAmountEqualsTotal()
	{
		OrderModel order = orderWithCode("ORDER-130");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel entry = mock(PaymentTransactionEntryModel.class);

		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(transaction.getEntries()).thenReturn(List.of(entry));
		when(entry.getType()).thenReturn(PaymentTransactionType.REFUND_FOLLOW_ON);
		when(entry.getTransactionStatus()).thenReturn(STATUS_ACCEPTED);
		when(entry.getAmount()).thenReturn(REFUND_100);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);

		assertThat(service.isFullyRefunded(order)).isTrue();
	}

	@Test
	public void shouldReturnFalseWhenIsFullyRefundedRefundEntryIsRejected()
	{
		OrderModel order = orderWithCode("ORDER-131");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel entry = mock(PaymentTransactionEntryModel.class);

		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(transaction.getEntries()).thenReturn(List.of(entry));
		when(entry.getType()).thenReturn(PaymentTransactionType.REFUND_FOLLOW_ON);
		when(entry.getTransactionStatus()).thenReturn(STATUS_REJECTED);
		when(entry.getAmount()).thenReturn(REFUND_100);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);

		assertThat(service.isFullyRefunded(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenIsFullyRefundedEntryIsNotRefund()
	{
		OrderModel order = orderWithCode("ORDER-132");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel entry = mock(PaymentTransactionEntryModel.class);

		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(transaction.getEntries()).thenReturn(List.of(entry));
		when(entry.getType()).thenReturn(PaymentTransactionType.CAPTURE);
		when(entry.getTransactionStatus()).thenReturn(STATUS_ACCEPTED);
		when(entry.getAmount()).thenReturn(REFUND_100);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);

		assertThat(service.isFullyRefunded(order)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenCanCreateReturnRequestOrderIsNull()
	{
		assertThat(service.canCreateReturnRequest(null)).isFalse();
	}

	@Test
	public void shouldReturnFalseWhenCanCreateReturnRequestOrderIsFullyRefunded()
	{
		OrderModel order = orderWithCode("ORDER-133");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel entry = mock(PaymentTransactionEntryModel.class);

		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(transaction.getEntries()).thenReturn(List.of(entry));
		when(entry.getType()).thenReturn(PaymentTransactionType.REFUND_FOLLOW_ON);
		when(entry.getTransactionStatus()).thenReturn(STATUS_ACCEPTED);
		when(entry.getAmount()).thenReturn(REFUND_100);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);

		assertThat(service.canCreateReturnRequest(order)).isFalse();
	}

	@Test
	public void shouldReturnTrueWhenCanCreateReturnRequestIsReturnableAndRefundable()
	{
		OrderModel order = orderWithCode("ORDER-134");
		ConsignmentModel consignment = mock(ConsignmentModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentTransactions()).thenReturn(Collections.emptyList());
		when(order.getConsignments()).thenReturn(Set.of(consignment));
		when(order.getEntries()).thenReturn(Collections.emptyList());
		when(consignment.getStatus()).thenReturn(SHIPPED);
		when(order.getPaymentInfo()).thenReturn(paymentInfo);
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-134")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_CONFIRMED);

		assertThat(service.canCreateReturnRequest(order)).isTrue();
	}

	@Test
	public void shouldReturnFalseWhenCanCreateReturnRequestIsNotReturnable()
	{
		OrderModel order = orderWithCode("ORDER-135");
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(order.getPaymentTransactions()).thenReturn(Collections.emptyList());
		when(order.getConsignments()).thenReturn(Collections.emptySet());
		when(order.getEntries()).thenReturn(Collections.emptyList());
		when(order.getPaymentInfo()).thenReturn(paymentInfo);
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-135")).thenReturn(List.of(paymentInfo));
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(STATUS_CONFIRMED);

		assertThat(service.canCreateReturnRequest(order)).isFalse();
	}

	@Test
	public void shouldReturnNotFoundWhenRefundRequestIsNull()
	{
		NovalnetTransactionResult result = service.refund(null);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.notfound");
	}

	@Test
	public void shouldReturnNotFoundWhenRefundOrderIsNull()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);

		when(returnRequest.getOrder()).thenReturn(null);

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.notfound");
	}

	@Test
	public void shouldReturnNotFoundWhenRefundHasNoPaymentTransactions()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-134b");

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(Collections.emptyList());

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.notfound");
	}

	@Test
	public void shouldReturnInvalidAmountWhenRefundAmountIsInvalid()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-135b");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(refundAmountCalculationService.getCustomRefundAmount(returnRequest)).thenReturn(null);
		when(refundAmountCalculationService.getOriginalRefundAmount(returnRequest)).thenReturn(BigDecimal.ZERO);

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.refund.amount.invalid");
		verify(paymentService, never()).refundFollowOn(any(), any());
	}

	@Test
	public void shouldReturnSuccessWhenRefundCustomAmountIsAccepted()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-136");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel refundEntry = mock(PaymentTransactionEntryModel.class);

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(refundAmountCalculationService.getCustomRefundAmount(returnRequest)).thenReturn(REFUND_25);
		when(refundEntry.getTransactionStatus()).thenReturn(STATUS_ACCEPTED);
		when(paymentService.refundFollowOn(transaction, REFUND_25)).thenReturn(refundEntry);

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.refund.success");
		verify(paymentService).refundFollowOn(transaction, REFUND_25);
	}

	@Test
	public void shouldReturnSuccessWhenRefundCustomAmountIsZero()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-137");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel refundEntry = mock(PaymentTransactionEntryModel.class);

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(refundAmountCalculationService.getCustomRefundAmount(returnRequest)).thenReturn(BigDecimal.ZERO);
		when(refundAmountCalculationService.getOriginalRefundAmount(returnRequest)).thenReturn(REFUND_30);
		when(refundEntry.getTransactionStatus()).thenReturn(STATUS_ACCEPTED);
		when(paymentService.refundFollowOn(transaction, REFUND_30)).thenReturn(refundEntry);

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isTrue();
		verify(paymentService).refundFollowOn(transaction, REFUND_30);
	}

	@Test
	public void shouldReturnFailureWhenRefundEntryIsNull()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-138");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(refundAmountCalculationService.getCustomRefundAmount(returnRequest)).thenReturn(REFUND_20);
		when(paymentService.refundFollowOn(transaction, REFUND_20)).thenReturn(null);

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.refund.failed");
	}

	@Test
	public void shouldReturnFailureWhenRefundEntryIsRejected()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-139");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);
		PaymentTransactionEntryModel refundEntry = mock(PaymentTransactionEntryModel.class);

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(refundAmountCalculationService.getCustomRefundAmount(returnRequest)).thenReturn(REFUND_20);
		when(refundEntry.getTransactionStatus()).thenReturn(STATUS_REJECTED);
		when(paymentService.refundFollowOn(transaction, REFUND_20)).thenReturn(refundEntry);

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.refund.failed");
	}

	@Test
	public void shouldReturnFailureWhenRefundThrowsAdapterException()
	{
		ReturnRequestModel returnRequest = mock(ReturnRequestModel.class);
		OrderModel order = orderWithCode("ORDER-140");
		PaymentTransactionModel transaction = mock(PaymentTransactionModel.class);

		when(returnRequest.getOrder()).thenReturn(order);
		when(order.getPaymentTransactions()).thenReturn(List.of(transaction));
		when(refundAmountCalculationService.getCustomRefundAmount(returnRequest)).thenReturn(REFUND_20);
		when(paymentService.refundFollowOn(transaction, REFUND_20)).thenThrow(new AdapterException("Refund failed"));

		NovalnetTransactionResult result = service.refund(returnRequest);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("novalnet.order.refund.failed");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundRequestIsNull() throws Exception
	{
		NovalnetTransactionResult result = service.processRefund(null);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Refund request is null");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundTidIsMissing() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);

		when(request.getRequestId()).thenReturn(null);
		when(request.getTotalAmount()).thenReturn(REFUND_10);

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Refund TID is missing");
	}


	@Test
	public void shouldReturnFailureWhenProcessRefundTidIsBlank() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);

		when(request.getRequestId()).thenReturn("   ");
		when(request.getTotalAmount()).thenReturn(REFUND_10);

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Refund TID is missing");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundAmountIsZero() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(BigDecimal.ZERO);

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Invalid refund amount");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundOrderIsNotFound() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_10);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(null);

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Order not found");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundAmountExceedsOrderTotal() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-141");

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_150);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Refund amount exceeds order total");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundBaseStoreIsMissing() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-142");

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_50);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);
		when(order.getStore()).thenReturn(null);

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("BaseStore is missing");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundApiReturnsFailure() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-143");
		BaseStoreModel store = mock(BaseStoreModel.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_50);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);
		when(order.getStore()).thenReturn(store);
		when(novalnetEndpointConfigService.getTransactionRefundUrl()).thenReturn("refund-url");
		when(novalnetApiService.followupSendRequest(eq("refund-url"), anyString(), eq(store)))
				.thenReturn(new StringBuilder("{\"result\":{\"status\":\"FAILURE\"," + "\"status_text\":\"Refund failed\"}}"));

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Refund failed");
	}

	@Test
	public void shouldThrowExceptionWhenProcessRefundApiThrowsException() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-144");
		BaseStoreModel store = mock(BaseStoreModel.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_50);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);
		when(order.getStore()).thenReturn(store);
		when(novalnetEndpointConfigService.getTransactionRefundUrl()).thenReturn("refund-url");
		when(novalnetApiService.followupSendRequest(eq("refund-url"), anyString(), eq(store)))
				.thenThrow(new RuntimeException("API error"));

		assertThatThrownBy(() -> service.processRefund(request)).isInstanceOf(RuntimeException.class).hasMessage("API error");
	}

	@Test
	public void shouldReturnFailureWhenProcessRefundResponseHasNoRefundObject() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-145");
		BaseStoreModel store = mock(BaseStoreModel.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_50);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);
		when(order.getStore()).thenReturn(store);
		when(novalnetEndpointConfigService.getTransactionRefundUrl()).thenReturn("refund-url");
		when(novalnetApiService.followupSendRequest(eq("refund-url"), anyString(), eq(store)))
				.thenReturn(new StringBuilder("{\"result\":{\"status\":\"SUCCESS\"}," + "\"transaction\":{\"status\":\"CONFIRMED\","
						+ "\"order_no\":\"ORDER-145\",\"amount\":10000}}"));

		NovalnetTransactionResult result = service.processRefund(request);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getMessage()).isEqualTo("Refund response is invalid");
	}

	@Test
	public void shouldCancelOrderAndRefreshModelWhenProcessRefundIsFull() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-146");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_100);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);
		when(order.getStore()).thenReturn(store);
		when(novalnetEndpointConfigService.getTransactionRefundUrl()).thenReturn("refund-url");
		when(novalnetApiService.followupSendRequest(eq("refund-url"), anyString(), eq(store))).thenReturn(new StringBuilder(
				"{\"result\":{\"status\":\"SUCCESS\"}," + "\"transaction\":{\"status\":\"CONFIRMED\",\"order_no\":\"ORDER-146\","
						+ "\"amount\":10000,\"currency\":\"EUR\"," + "\"refund\":{\"tid\":999888,\"amount\":10000}}}"));
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-146")).thenReturn(List.of(paymentInfo));

		NovalnetTransactionResult result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.processRefund(request);
		}

		assertThat(result.isSuccess()).isTrue();

		verify(novalnetPaymentService).updatePaymentInfo(List.of(paymentInfo), "REFUND");
		verify(novalnetOrderService).updateCancelStatus("ORDER-146");
		verify(modelService).refresh(order);
	}

	@Test
	public void shouldNotCancelOrderWhenProcessRefundIsPartial() throws Exception
	{
		FollowOnRefundRequest request = mock(FollowOnRefundRequest.class);
		OrderModel order = orderWithCode("ORDER-147");
		BaseStoreModel store = mock(BaseStoreModel.class);
		NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);

		when(request.getRequestId()).thenReturn(TRANSACTION_ID);
		when(request.getTotalAmount()).thenReturn(REFUND_50);
		when(novalnetDao.getOrderByTid(TRANSACTION_ID)).thenReturn(order);
		when(order.getTotalPrice()).thenReturn(ORDER_TOTAL);
		when(order.getStore()).thenReturn(store);
		when(novalnetEndpointConfigService.getTransactionRefundUrl()).thenReturn("refund-url");
		when(novalnetApiService.followupSendRequest(eq("refund-url"), anyString(), eq(store))).thenReturn(new StringBuilder(
				"{\"result\":{\"status\":\"SUCCESS\"}," + "\"transaction\":{\"status\":\"CONFIRMED\",\"order_no\":\"ORDER-147\","
						+ "\"amount\":10000,\"currency\":\"EUR\"," + "\"refund\":{\"tid\":999889,\"amount\":5000}}}"));
		when(novalnetDao.getNovalnetPaymentInfo("ORDER-147")).thenReturn(List.of(paymentInfo));

		NovalnetTransactionResult result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.processRefund(request);
		}

		assertThat(result.isSuccess()).isTrue();

		verify(novalnetPaymentService).updatePaymentInfo(List.of(paymentInfo), "PARTIAL_REFUND");
		verify(novalnetOrderService, never()).updateCancelStatus(anyString());
		verify(modelService, never()).refresh(any());
	}

	@Test
	public void shouldReturnEmptyBankDetailsForUnsupportedPayment()
	{
		CartData cartData = mock(CartData.class);
		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);

		when(novalnetPaymentService.getPaymentName("novalnetCreditCard")).thenReturn("Credit Card");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetCreditCard", transaction, cartData);
		}

		assertThat(result).isNotNull();
		assertThat(result.getBankDetails()).isEmpty();

		verify(sessionService).removeAttribute("novalnetZeroAmountBooking");
	}

	@Test
	public void shouldIncludeTestOrderTextWhenPaymentIsInTestMode()
	{
		CartData cartData = mock(CartData.class);
		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "1");
		transaction.put("tid", TRANSACTION_ID);

		when(novalnetPaymentService.getPaymentName("novalnetCreditCard")).thenReturn("Credit Card");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetCreditCard", transaction, cartData);
		}

		assertThat(result.getOrderComments()).contains("novalnet.testOrderText");
	}

	@Test
	public void shouldIncludeZeroAmountTextWhenBookingAmountIsZero()
	{
		CartData cartData = mock(CartData.class);
		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);

		when(novalnetPaymentService.getPaymentName("novalnetCreditCard")).thenReturn("Credit Card");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.TRUE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetCreditCard", transaction, cartData);
		}

		assertThat(result.getOrderComments()).contains("novalnet.zeroAmountBooking");
	}

	@Test
	public void shouldIncludeBankDetailsForInvoicePayment()
	{
		CartData cartData = mock(CartData.class);
		PriceData priceData = mock(PriceData.class);

		when(cartData.getTotalPriceWithTax()).thenReturn(priceData);
		when(priceData.getFormattedValue()).thenReturn("100.00");

		JSONObject bankDetails = new JSONObject();

		bankDetails.put("account_holder", "Test Customer");
		bankDetails.put("iban", "DE123456789");
		bankDetails.put("bic", "TESTDEFF");
		bankDetails.put("bank_name", "Test Bank");
		bankDetails.put("bank_place", "Berlin");

		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("status", STATUS_CONFIRMED);
		transaction.put("status_code", "100");
		transaction.put("due_date", "2026-09-10");
		transaction.put("bank_details", bankDetails);

		when(novalnetPaymentService.getPaymentName("novalnetInvoice")).thenReturn("Invoice");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetInvoice", transaction, cartData);
		}

		assertThat(result.getBankDetails()).contains("Test Customer");
		assertThat(result.getBankDetails()).contains("DE123456789");
		assertThat(result.getBankDetails()).contains("TESTDEFF");
		assertThat(result.getBankDetails()).contains("Test Bank");
		assertThat(result.getBankDetails()).contains("Berlin");
		assertThat(result.getBankDetails()).contains(TRANSACTION_ID);
	}

	@Test
	public void shouldOmitDueDateWhenInvoicePaymentIsOnHold()
	{
		CartData cartData = mock(CartData.class);
		PriceData priceData = mock(PriceData.class);

		when(cartData.getTotalPriceWithTax()).thenReturn(priceData);
		when(priceData.getFormattedValue()).thenReturn("100.00");

		JSONObject bankDetails = new JSONObject();

		bankDetails.put("account_holder", "Test Customer");
		bankDetails.put("iban", "DE123456789");
		bankDetails.put("bic", "TESTDEFF");
		bankDetails.put("bank_name", "Test Bank");
		bankDetails.put("bank_place", "Berlin");

		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("status", STATUS_ON_HOLD);
		transaction.put("status_code", "100");
		transaction.put("due_date", "2026-09-10");
		transaction.put("bank_details", bankDetails);

		when(novalnetPaymentService.getPaymentName("novalnetInvoice")).thenReturn("Invoice");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetInvoice", transaction, cartData);
		}

		assertThat(result.getBankDetails()).doesNotContain("10-09-2026");
	}

	@Test
	public void shouldReturnStatus75TextForGuaranteedInvoice()
	{
		CartData cartData = mock(CartData.class);
		PriceData priceData = mock(PriceData.class);

		when(cartData.getTotalPriceWithTax()).thenReturn(priceData);
		when(priceData.getFormattedValue()).thenReturn("100.00");

		JSONObject bankDetails = new JSONObject();

		bankDetails.put("account_holder", "Test Customer");
		bankDetails.put("iban", "DE123456789");
		bankDetails.put("bic", "TESTDEFF");
		bankDetails.put("bank_name", "Test Bank");
		bankDetails.put("bank_place", "Berlin");

		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("status", STATUS_ON_HOLD);
		transaction.put("status_code", "75");
		transaction.put("bank_details", bankDetails);

		when(novalnetPaymentService.getPaymentName("novalnetGuaranteedInvoice")).thenReturn("Guaranteed Invoice");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetGuaranteedInvoice", transaction, cartData);
		}

		assertThat(result.getBankDetails().replace("<br>", "")).isEqualTo("novalnet.status75");
	}

	@Test
	public void shouldAppendQrImageWhenBankDetailsContainQrImage()
	{
		CartData cartData = mock(CartData.class);
		PriceData priceData = mock(PriceData.class);

		when(cartData.getTotalPriceWithTax()).thenReturn(priceData);
		when(priceData.getFormattedValue()).thenReturn("100.00");

		JSONObject bankDetails = new JSONObject();

		bankDetails.put("account_holder", "Test Customer");
		bankDetails.put("iban", "DE123456789");
		bankDetails.put("bic", "TESTDEFF");
		bankDetails.put("bank_name", "Test Bank");
		bankDetails.put("bank_place", "Berlin");
		bankDetails.put("qr_image", "https://example.com/qr.png");

		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("status", STATUS_CONFIRMED);
		transaction.put("status_code", "100");
		transaction.put("bank_details", bankDetails);

		when(novalnetPaymentService.getPaymentName("novalnetPrepayment")).thenReturn("Prepayment");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetPrepayment", transaction, cartData);
		}

		assertThat(result.getBankDetails()).contains("<img alt='nn_qr_code' src='https://example.com/qr.png'>");
	}

	@Test
	public void shouldIncludeReferenceDetailsForMultibancoPayment()
	{
		CartData cartData = mock(CartData.class);
		PriceData priceData = mock(PriceData.class);

		when(cartData.getTotalPriceWithTax()).thenReturn(priceData);
		when(priceData.getFormattedValue()).thenReturn("100.00");

		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("partner_payment_reference", "MB-REF-001");
		transaction.put("service_supplier_id", "12345");

		when(novalnetPaymentService.getPaymentName("novalnetMultibanco")).thenReturn("Multibanco");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetMultibanco", transaction, cartData);
		}

		assertThat(result.getBankDetails()).contains("MB-REF-001");
		assertThat(result.getBankDetails()).contains("12345");
	}

	@Test
	public void shouldReturnEmptyBankDetailsWhenMultibancoReferenceIsMissing()
	{
		CartData cartData = mock(CartData.class);
		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);

		when(novalnetPaymentService.getPaymentName("novalnetMultibanco")).thenReturn("Multibanco");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetMultibanco", transaction, cartData);
		}

		assertThat(result.getBankDetails()).isEmpty();
	}


	@Test
	public void shouldReturnStatus75TextForGuaranteedSepaPayment()
	{
		CartData cartData = mock(CartData.class);
		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("status_code", "75");

		when(novalnetPaymentService.getPaymentName("novalnetGuaranteedDirectDebitSepa")).thenReturn("Guaranteed SEPA");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetGuaranteedDirectDebitSepa", transaction, cartData);
		}

		assertThat(result.getBankDetails().replace("<br>", "")).isEqualTo("novalnet.sepa.status75");
	}

	@Test
	public void shouldReturnEmptyBankDetailsWhenGuaranteedSepaStatusIsNot75()
	{
		CartData cartData = mock(CartData.class);
		JSONObject transaction = new JSONObject();

		transaction.put("test_mode", "0");
		transaction.put("tid", TRANSACTION_ID);
		transaction.put("status_code", "100");

		when(novalnetPaymentService.getPaymentName("novalnetGuaranteedDirectDebitSepa")).thenReturn("Guaranteed SEPA");
		when(sessionService.getAttribute("novalnetZeroAmountBooking")).thenReturn(Boolean.FALSE);

		OrderPaymentCommentsData result;

		try (MockedStatic<Localization> localization = mockLocalization())
		{
			result = service.buildOrderAndPaymentComments("novalnetGuaranteedDirectDebitSepa", transaction, cartData);
		}

		assertThat(result.getBankDetails()).isEmpty();
	}
}

