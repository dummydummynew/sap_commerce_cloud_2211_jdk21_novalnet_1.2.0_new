/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.order.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedInvoicePaymentModeModel;
import com.novalnet.model.NovalnetInvoicePaymentModeModel;
import com.novalnet.model.NovalnetPayPalPaymentModeModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPrepaymentPaymentModeModel;
import com.novalnet.model.NovalnetPrzelewy24PaymentModeModel;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetOrderServiceTest
{
	private static final String ORDER_CONFIRMED = "ORDER001";
	private static final String ORDER_ON_HOLD = "ORDER002";
	private static final String ORDER_PENDING = "ORDER003";
	private static final String ORDER_INVOICE = "ORDER004";
	private static final String ORDER_CANCEL = "ORDER005";
	private static final String ORDER_PART_PAID = "ORDER006";
	private static final String ORDER_CALLBACK_INVOICE = "ORDER007";
	private static final String ORDER_CALLBACK_PREPAYMENT = "ORDER008";
	private static final String ORDER_GET = "ORDER009";
	private static final String ORDER_CALLBACK = "ORDER010";
	private static final String ORDER_GUARANTEED_INVOICE = "ORDER011";
	private static final String UNKNOWN_ORDER = "UNKNOWN_ORDER";
	private static final String UNKNOWN_PAYMENT = "unknownPayment";

	private static final String CREDIT_CARD_PAYMENT = "novalnetCreditCard";
	private static final String PAYPAL_PAYMENT = "novalnetPayPal";
	private static final String INVOICE_PAYMENT = "novalnetInvoice";
	private static final String GUARANTEED_INVOICE_PAYMENT = "novalnetGuaranteedInvoice";
	private static final String PREPAYMENT_PAYMENT = "novalnetPrepayment";
	private static final String PRZELEWY24_PAYMENT = "novalnetPrzelewy24";

	private static final String CONFIRMED_STATUS = "CONFIRMED";
	private static final String ON_HOLD_STATUS = "ON_HOLD";
	private static final String PENDING_STATUS = "PENDING";

	private static final String PREVIOUS_COMMENT = "Previous comment";
	private static final String PREVIOUS_NOTE = "Previous";
	private static final String CALLBACK_COMMENT = "New callback comment";
	private static final String CALLBACK_TEXT = "Callback text";
	private static final String SHORT_COMMENT = "Short comment";

	private DefaultNovalnetOrderService service;

	@Mock
	private ModelService modelService;

	@Mock
	private FlexibleSearchService flexibleSearchService;

	@Mock
	private BaseStoreService baseStoreService;

	@Mock
	private PaymentModeService paymentModeService;

	@Mock
	private NovalnetDao novalnetDao;

	@Mock
	private OrderModel orderModel;

	@Mock
	private BaseStoreModel baseStore;

	@Mock
	private NovalnetPaymentInfoModel paymentInfoModel;

	@Mock
	private PK paymentInfoPk;

	@Mock
	private PK orderPk;

	@BeforeEach
	public void setUp()
	{
		service = new DefaultNovalnetOrderService();

		ReflectionTestUtils.setField(service, "modelService", modelService);
		ReflectionTestUtils.setField(service, "flexibleSearchService", flexibleSearchService);
		ReflectionTestUtils.setField(service, "baseStoreService", baseStoreService);
		ReflectionTestUtils.setField(service, "paymentModeService", paymentModeService);
		ReflectionTestUtils.setField(service, "novalnetDao", novalnetDao);
	}

	@Test
	public void shouldUpdateOrderAsPaidWhenConfirmedPayment()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_CONFIRMED)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
		when(paymentInfoModel.getPaymentProvider()).thenReturn(CREDIT_CARD_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(CONFIRMED_STATUS);

		final NovalnetCreditCardPaymentModeModel paymentMode = mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		service.updateOrderStatus(ORDER_CONFIRMED, paymentInfoModel);

		verify(orderModel).setStatus(OrderStatus.COMPLETED);
		verify(orderModel).setPaymentStatus(PaymentStatus.PAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldUpdateOrderAsNotPaidWhenOnHoldPayment()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_ON_HOLD)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
		when(paymentInfoModel.getPaymentProvider()).thenReturn(CREDIT_CARD_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(ON_HOLD_STATUS);

		final NovalnetCreditCardPaymentModeModel paymentMode = mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		service.updateOrderStatus(ORDER_ON_HOLD, paymentInfoModel);

		verify(orderModel).setStatus(OrderStatus.PAYMENT_AUTHORIZED);
		verify(orderModel).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldUpdateOrderAsNotCapturedWhenPendingPayment()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_PENDING)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
		when(paymentInfoModel.getPaymentProvider()).thenReturn(PAYPAL_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(PENDING_STATUS);

		final NovalnetPayPalPaymentModeModel paymentMode = mock(NovalnetPayPalPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(PAYPAL_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		service.updateOrderStatus(ORDER_PENDING, paymentInfoModel);

		verify(orderModel).setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
		verify(orderModel).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldKeepOrderNotPaidWhenInvoicePayment()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_INVOICE)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
		when(paymentInfoModel.getPaymentProvider()).thenReturn(INVOICE_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(CONFIRMED_STATUS);

		final NovalnetInvoicePaymentModeModel paymentMode = mock(NovalnetInvoicePaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(INVOICE_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		service.updateOrderStatus(ORDER_INVOICE, paymentInfoModel);

		verify(orderModel).setStatus(OrderStatus.COMPLETED);
		verify(orderModel).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldCancelOrderWhenUpdateCancelStatus()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_CANCEL)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);

		service.updateCancelStatus(ORDER_CANCEL);

		verify(orderModel).setStatus(OrderStatus.CANCELLED);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldSetPartPaidWhenUpdatePartPaidStatus()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_PART_PAID)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);

		service.updatePartPaidStatus(ORDER_PART_PAID);

		verify(orderModel).setPaymentStatus(PaymentStatus.PARTPAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldSetConfiguredStatusWhenInvoiceCallback()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_CALLBACK_INVOICE)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);

		final NovalnetInvoicePaymentModeModel paymentMode = mock(NovalnetInvoicePaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(INVOICE_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetCallbackOrderStatus()).thenReturn(OrderStatus.PAYMENT_AUTHORIZED);

		service.updateCallbackOrderStatus(ORDER_CALLBACK_INVOICE, INVOICE_PAYMENT);

		verify(orderModel).setStatus(OrderStatus.PAYMENT_AUTHORIZED);
		verify(orderModel).setPaymentStatus(PaymentStatus.PAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldSetConfiguredStatusWhenPrepaymentCallback()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_CALLBACK_PREPAYMENT)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);

		final NovalnetPrepaymentPaymentModeModel paymentMode = mock(NovalnetPrepaymentPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(PREPAYMENT_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetCallbackOrderStatus()).thenReturn(OrderStatus.COMPLETED);

		service.updateCallbackOrderStatus(ORDER_CALLBACK_PREPAYMENT, PREPAYMENT_PAYMENT);

		verify(orderModel).setStatus(OrderStatus.COMPLETED);
		verify(orderModel).setPaymentStatus(PaymentStatus.PAID);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldReturnConfiguredStatusWhenCreditCardConfirmed()
	{
		when(paymentInfoModel.getPaymentProvider()).thenReturn(CREDIT_CARD_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(CONFIRMED_STATUS);

		final NovalnetCreditCardPaymentModeModel paymentMode = mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		final OrderStatus result = service.getOrderStatus(paymentInfoModel, baseStore);

		assertThat(result).isEqualTo(OrderStatus.COMPLETED);
	}

	@Test
	public void shouldReturnPaymentAuthorizedWhenCreditCardOnHold()
	{
		when(paymentInfoModel.getPaymentProvider()).thenReturn(CREDIT_CARD_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(ON_HOLD_STATUS);

		final NovalnetCreditCardPaymentModeModel paymentMode = mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(CREDIT_CARD_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		final OrderStatus result = service.getOrderStatus(paymentInfoModel, baseStore);

		assertThat(result).isEqualTo(OrderStatus.PAYMENT_AUTHORIZED);
	}

	@Test
	public void shouldReturnPaymentNotCapturedWhenGuaranteedInvoicePending()
	{
		when(paymentInfoModel.getPaymentProvider()).thenReturn(GUARANTEED_INVOICE_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(PENDING_STATUS);

		final NovalnetGuaranteedInvoicePaymentModeModel paymentMode = mock(NovalnetGuaranteedInvoicePaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(GUARANTEED_INVOICE_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		final OrderStatus result = service.getOrderStatus(paymentInfoModel, baseStore);

		assertThat(result).isEqualTo(OrderStatus.PAYMENT_NOT_CAPTURED);
	}

	@Test
	public void shouldReturnPaymentNotCapturedWhenPrzelewy24Pending()
	{
		when(paymentInfoModel.getPaymentProvider()).thenReturn(PRZELEWY24_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(PENDING_STATUS);

		final NovalnetPrzelewy24PaymentModeModel paymentMode = mock(NovalnetPrzelewy24PaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(PRZELEWY24_PAYMENT)).thenReturn(paymentMode);
		when(paymentMode.getNovalnetOrderSuccessStatus()).thenReturn(OrderStatus.COMPLETED);

		final OrderStatus result = service.getOrderStatus(paymentInfoModel, baseStore);

		assertThat(result).isEqualTo(OrderStatus.PAYMENT_NOT_CAPTURED);
	}

	@Test
	public void shouldReturnCompletedWhenUnknownPaymentMethod()
	{
		when(paymentInfoModel.getPaymentProvider()).thenReturn(UNKNOWN_PAYMENT);
		when(paymentInfoModel.getPaymentGatewayStatus()).thenReturn(CONFIRMED_STATUS);

		final OrderStatus result = service.getOrderStatus(paymentInfoModel, baseStore);

		assertThat(result).isEqualTo(OrderStatus.COMPLETED);
	}

	@Test
	public void shouldReturnOrderWhenOrderExists()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_GET)).thenReturn(Collections.singletonList(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);

		final OrderModel result = service.getOrder(ORDER_GET);

		assertThat(result).isEqualTo(orderModel);
	}



	@Test
	public void shouldUpdatePaymentInfoAndHistoryWhenCallbackComments()
	{
		final NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);
		final OrderHistoryEntryModel orderEntry = mock(OrderHistoryEntryModel.class);

		when(paymentInfo.getPk()).thenReturn(paymentInfoPk);
		when(orderModel.getPk()).thenReturn(orderPk);
		when(novalnetDao.getNovalnetPaymentInfo(ORDER_CALLBACK)).thenReturn(Collections.singletonList(paymentInfo));
		when(modelService.get(paymentInfoPk)).thenReturn(paymentInfo);
		when(paymentInfo.getOrderHistoryNotes()).thenReturn(PREVIOUS_COMMENT);
		when(novalnetDao.getOrderInfoModel(ORDER_CALLBACK)).thenReturn(Collections.singletonList(orderModel));
		when(modelService.get(orderPk)).thenReturn(orderModel);
		when(modelService.create(OrderHistoryEntryModel.class)).thenReturn(orderEntry);

		service.updateCallbackComments(CALLBACK_COMMENT, ORDER_CALLBACK, CONFIRMED_STATUS);

		verify(paymentInfo).setOrderHistoryNotes(PREVIOUS_COMMENT + "<br><br>" + CALLBACK_COMMENT);
		verify(paymentInfo).setPaymentGatewayStatus(CONFIRMED_STATUS);
		verify(orderEntry).setOrder(orderModel);
		verify(orderEntry).setDescription(CALLBACK_COMMENT);
		verify(modelService).saveAll(paymentInfo, orderEntry);
	}

	@Test
	public void shouldUpdatePaymentInfoAndHistoryWhenGuaranteedInvoiceCallbackComments()
	{
		final NovalnetPaymentInfoModel paymentInfo = mock(NovalnetPaymentInfoModel.class);
		final OrderHistoryEntryModel orderEntry = mock(OrderHistoryEntryModel.class);

		when(paymentInfo.getPk()).thenReturn(paymentInfoPk);
		when(orderModel.getPk()).thenReturn(orderPk);
		when(novalnetDao.getNovalnetPaymentInfo(ORDER_GUARANTEED_INVOICE)).thenReturn(Collections.singletonList(paymentInfo));
		when(modelService.get(paymentInfoPk)).thenReturn(paymentInfo);
		when(paymentInfo.getOrderHistoryNotes()).thenReturn(PREVIOUS_NOTE);
		when(novalnetDao.getOrderInfoModel(ORDER_GUARANTEED_INVOICE)).thenReturn(Collections.singletonList(orderModel));
		when(modelService.get(orderPk)).thenReturn(orderModel);
		when(modelService.create(OrderHistoryEntryModel.class)).thenReturn(orderEntry);

		service.updateGuaranteedInvoiceCallbackComments(CALLBACK_TEXT, SHORT_COMMENT, ORDER_GUARANTEED_INVOICE, ON_HOLD_STATUS);

		verify(paymentInfo).setOrderHistoryNotes(PREVIOUS_NOTE + "<br><br>" + CALLBACK_TEXT);
		verify(paymentInfo).setPaymentGatewayStatus(ON_HOLD_STATUS);
		verify(orderEntry).setOrder(orderModel);
		verify(orderEntry).setDescription(SHORT_COMMENT);
		verify(modelService).saveAll(paymentInfo, orderEntry);
	}
}