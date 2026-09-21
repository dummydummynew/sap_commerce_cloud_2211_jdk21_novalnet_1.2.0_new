/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.order.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.util.localization.Localization;

import java.util.Locale;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.novalnet.dao.NovalnetCallbackDao;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetCallbackOrderServiceTest
{
	private static final String ORDER_CODE = "ORDER-10001";
	private static final String PAYMENT_STATUS_CONFIRMED = "CONFIRMED";
	private static final String PAYMENT_STATUS_PENDING = "PENDING";
	private static final String PAYMENT_STATUS_ON_HOLD = "ON_HOLD";
	private static final String PAYMENT_STATUS_FAILURE = "FAILURE";
	private static final String PAYMENT_STATUS_DEACTIVATED = "DEACTIVATED";

	private static final String ORIGINAL_TID = "123456789";
	private static final Long CALLBACK_TID = 987654321L;
	private static final Long ORIGINAL_TID_NUMBER = 123L;
	private static final String TRANSACTION_STATUS = "CONFIRMED";
	private static final String LOCALIZED_LABEL_PLACEHOLDER = "Label";

	private static final Integer PAID_AMOUNT = 1999;
	private static final Integer SUCCESS_STATUS_CODE = 100;
	private static final int TEST_MODE_ENABLED = 1;

	private static final String PAID_AMOUNT_STRING = "1999";
	private static final String ZERO_AMOUNT = "0";
	private static final String ZERO_AMOUNT_DISPLAY = "0.00";
	private static final String TRANSACTION_AMOUNT_DISPLAY = "19.99";

	private DefaultNovalnetCallbackOrderService service;
	private ModelService modelService;
	private NovalnetCallbackDao novalnetCallbackDao;
	private CommonI18NService commonI18NService;
	private OrderModel order;
	private NovalnetPaymentInfoModel paymentInfo;
	private NovalnetCallbackInfoModel callbackInfo;
	private LanguageModel language;
	private MockedStatic<Localization> localizationMockedStatic;

	@BeforeEach
	public void setUp()
	{
		service = new DefaultNovalnetCallbackOrderService();

		modelService = mock(ModelService.class);
		novalnetCallbackDao = mock(NovalnetCallbackDao.class);
		commonI18NService = mock(CommonI18NService.class);
		order = mock(OrderModel.class);
		paymentInfo = mock(NovalnetPaymentInfoModel.class);
		callbackInfo = mock(NovalnetCallbackInfoModel.class);
		language = mock(LanguageModel.class);

		ReflectionTestUtils.setField(service, "modelService", modelService);
		ReflectionTestUtils.setField(service, "novalnetCallbackDao", novalnetCallbackDao);
		ReflectionTestUtils.setField(service, "commonI18NService", commonI18NService);

		localizationMockedStatic = Mockito.mockStatic(Localization.class, invocation -> {
			StringBuilder builder = new StringBuilder(LOCALIZED_LABEL_PLACEHOLDER);

			for (final Object argument : invocation.getArguments())
			{
				if (argument instanceof Object[])
				{
					for (final Object param : (Object[]) argument)
					{
						builder.append(' ').append(param);
					}
				}
			}

			return builder.toString();
		});
	}


	@AfterEach
	public void tearDown()
	{
		localizationMockedStatic.close();
	}

	@Test
	public void updateOrderStatusPendingPaymentSetsPaymentNotCaptured()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PAYMENT_STATUS_PENDING);

		service.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}

	@Test
	public void updateOrderStatusOnHoldPaymentSetsPaymentAuthorized()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PAYMENT_STATUS_ON_HOLD);

		service.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.PAYMENT_AUTHORIZED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}

	@Test
	public void updateOrderStatusConfirmedPaymentCompletesOrderAndMarksPaid()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PAYMENT_STATUS_CONFIRMED);

		service.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.COMPLETED);
		verify(order).setPaymentStatus(PaymentStatus.PAID);
		verify(modelService).save(order);
	}

	@Test
	public void updateOrderStatusFailurePaymentCancelsOrder()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PAYMENT_STATUS_FAILURE);

		service.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.CANCELLED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}

	@Test
	public void updateOrderStatusDeactivatedPaymentCancelsOrder()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PAYMENT_STATUS_DEACTIVATED);

		service.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.CANCELLED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}

	@Test
	public void updateOrderStatusNullOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateOrderStatus(null, paymentInfo, order)).isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateOrderStatusBlankOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateOrderStatus("   ", paymentInfo, order))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateOrderStatusNullPaymentInfoThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateOrderStatus(ORDER_CODE, null, order)).isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateOrderStatusInvalidGatewayStatusThrowsRequestParameterException()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn("INVALID_STATUS");

		assertThatThrownBy(() -> service.updateOrderStatus(ORDER_CODE, paymentInfo, order))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCancelStatusValidOrderCancelsOrder()
	{
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		service.updateCancelStatus(ORDER_CODE);

		verify(order).setStatus(OrderStatus.CANCELLED);
		verify(modelService).save(order);
	}

	@Test
	public void updateCancelStatusNullOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCancelStatus(null)).isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void updateCancelStatusBlankOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCancelStatus(" ")).isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void updatePartPaidStatusValidOrderSetsPartPaid()
	{
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		service.updatePartPaidStatus(ORDER_CODE);

		verify(order).setPaymentStatus(PaymentStatus.PARTPAID);
		verify(modelService).save(order);
	}

	@Test
	public void updatePartPaidStatusNullOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updatePartPaidStatus(null)).isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void updatePartPaidStatusBlankOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updatePartPaidStatus("")).isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void updatePaymentInfoValidParametersUpdatesGatewayStatus()
	{
		when(novalnetCallbackDao.getLatestNovalnetPaymentInfo(ORDER_CODE)).thenReturn(paymentInfo);

		service.updatePaymentInfo(ORDER_CODE, TRANSACTION_STATUS);

		verify(paymentInfo).setPaymentGatewayStatus(TRANSACTION_STATUS);
		verify(modelService).save(paymentInfo);
	}


	@Test
	public void updatePaymentInfoNullOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updatePaymentInfo(null, TRANSACTION_STATUS)).isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updatePaymentInfoBlankOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updatePaymentInfo(" ", TRANSACTION_STATUS)).isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updatePaymentInfoNullGatewayStatusThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updatePaymentInfo(ORDER_CODE, null)).isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updatePaymentInfoBlankGatewayStatusThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updatePaymentInfo(ORDER_CODE, "")).isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCallbackInfoValidParametersUpdatesCallbackData()
	{
		when(novalnetCallbackDao.findCallbackInfoByOriginalTid(ORIGINAL_TID)).thenReturn(callbackInfo);

		service.updateCallbackInfo(CALLBACK_TID, ORIGINAL_TID, PAID_AMOUNT);

		verify(callbackInfo).setCallbackTid(CALLBACK_TID);
		verify(callbackInfo).setPaidAmount(PAID_AMOUNT);
		verify(modelService).save(callbackInfo);
	}

	@Test
	public void updateCallbackInfoNullOriginalTidThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCallbackInfo(ORIGINAL_TID_NUMBER, null, SUCCESS_STATUS_CODE))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCallbackInfoBlankOriginalTidThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCallbackInfo(ORIGINAL_TID_NUMBER, " ", SUCCESS_STATUS_CODE))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCallbackCommentsExistingNotesAppendsCommentAndUpdatesStatus()
	{
		String existingNotes = "Existing note";
		String comments = "New callback comment";

		when(novalnetCallbackDao.getLatestNovalnetPaymentInfo(ORDER_CODE)).thenReturn(paymentInfo);
		when(paymentInfo.getOrderHistoryNotes()).thenReturn(existingNotes);
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		OrderHistoryEntryModel entry = mock(OrderHistoryEntryModel.class);

		when(modelService.create(OrderHistoryEntryModel.class)).thenReturn(entry);

		service.updateCallbackComments(comments, ORDER_CODE, TRANSACTION_STATUS, "Callback received");

		verify(paymentInfo).setOrderHistoryNotes(existingNotes + "<br/><br/>" + comments);
		verify(paymentInfo).setPaymentGatewayStatus(TRANSACTION_STATUS);
		verify(entry).setOrder(order);
		verify(entry).setDescription("Callback received");
		verify(modelService).saveAll(paymentInfo, entry);
	}

	/**
	 * Verifies that callback comments create new notes when no existing notes are available.
	 */
	@Test
	public void updateCallbackCommentsNullExistingNotesCreatesNotes()
	{
		when(novalnetCallbackDao.getLatestNovalnetPaymentInfo(ORDER_CODE)).thenReturn(paymentInfo);
		when(paymentInfo.getOrderHistoryNotes()).thenReturn(null);
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		OrderHistoryEntryModel entry = mock(OrderHistoryEntryModel.class);

		when(modelService.create(OrderHistoryEntryModel.class)).thenReturn(entry);

		service.updateCallbackComments("Payment confirmed", ORDER_CODE, TRANSACTION_STATUS, "Payment callback");

		verify(paymentInfo).setOrderHistoryNotes("<br/><br/>Payment confirmed");
		verify(paymentInfo).setPaymentGatewayStatus(TRANSACTION_STATUS);
		verify(modelService).saveAll(paymentInfo, entry);
	}

	@Test
	public void updateCallbackCommentsNullOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCallbackComments("Comment", null, TRANSACTION_STATUS, "Entry"))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCallbackCommentsBlankOrderCodeThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCallbackComments("Comment", " ", TRANSACTION_STATUS, "Entry"))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCallbackCommentsNullTransactionStatusThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCallbackComments("Comment", ORDER_CODE, null, "Entry"))
				.isInstanceOf(RequestParameterException.class);
	}

	@Test
	public void updateCallbackCommentsBlankTransactionStatusThrowsRequestParameterException()
	{
		assertThatThrownBy(() -> service.updateCallbackComments("Comment", ORDER_CODE, "", "Entry"))
				.isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void setSessionLanguageValidOrderSetsCurrentLanguage()
	{
		when(order.getLanguage()).thenReturn(language);

		service.setSessionLanguage(order);

		verify(commonI18NService).setCurrentLanguage(language);
	}


	@Test
	public void getLocaleValidOrderReturnsExpectedLocale()
	{
		Locale expectedLocale = Locale.GERMANY;

		when(order.getLanguage()).thenReturn(language);
		when(commonI18NService.getLocaleForLanguage(language)).thenReturn(expectedLocale);

		Locale result = service.getLocale(order);

		assertThat(result).isEqualTo(expectedLocale);
		verify(commonI18NService).getLocaleForLanguage(language);
	}


	@Test
	public void getCurrencyValidCodeReturnsCurrency()
	{
		String currencyCode = "EUR";
		CurrencyModel currency = mock(CurrencyModel.class);

		when(commonI18NService.getCurrency(currencyCode)).thenReturn(currency);

		CurrencyModel result = service.getCurrency(currencyCode);

		assertThat(result).isEqualTo(currency);
		verify(commonI18NService).getCurrency(currencyCode);
	}


	@Test
	public void getLabelUnknownKeyReturnsLocalizedPlaceholder()
	{
		String key = "novalnet.test.key";

		String result = service.getLabel(key);

		assertThat(result).isEqualTo(LOCALIZED_LABEL_PLACEHOLDER);
		localizationMockedStatic.verify(() -> Localization.getLocalizedString(key));
	}


	@Test
	public void buildOrderHistoryNotesBasicTransactionIncludesPaymentInformation()
	{
		JSONObject transaction = createBasicTransaction();

		String result = service.buildOrderHistoryNotes(transaction, TRANSACTION_AMOUNT_DISPLAY, "Credit Card");

		assertThat(result).isNotNull();
		assertThat(result).contains("Credit Card");
		assertThat(result).contains(ORIGINAL_TID);
	}


	@Test
	public void buildOrderHistoryNotesZeroAmountTransactionHandlesZeroAmount()
	{
		JSONObject transaction = createBasicTransaction();

		transaction.put("amount", ZERO_AMOUNT);

		String result = service.buildOrderHistoryNotes(transaction, ZERO_AMOUNT_DISPLAY, "Credit Card");

		assertThat(result).isNotNull();
	}


	@Test
	public void buildOrderHistoryNotesTestModeTransactionCompletesSuccessfully()
	{
		JSONObject transaction = createBasicTransaction();

		transaction.put("test_mode", String.valueOf(TEST_MODE_ENABLED));

		String result = service.buildOrderHistoryNotes(transaction, TRANSACTION_AMOUNT_DISPLAY, "Credit Card");

		assertThat(result).isNotNull();
	}


	@Test
	public void buildOrderHistoryNotesBankDetailsIncludesBankInformation()
	{
		JSONObject transaction = createBasicTransaction();
		JSONObject bankDetails = new JSONObject();

		bankDetails.put("account_holder", "John Doe");
		bankDetails.put("iban", "DE123456789");
		bankDetails.put("bic", "TESTBIC");
		bankDetails.put("bank_name", "Test Bank");
		bankDetails.put("bank_place", "Berlin");

		transaction.put("bank_details", bankDetails);

		String result = service.buildOrderHistoryNotes(transaction, TRANSACTION_AMOUNT_DISPLAY, "SEPA");

		assertThat(result).isNotNull();
		assertThat(result).contains("John Doe");
		assertThat(result).contains("DE123456789");
		assertThat(result).contains("TESTBIC");
	}

	@Test
	public void buildOrderHistoryNotesQrImageIncludesQrImage()
	{
		JSONObject transaction = createBasicTransaction();
		JSONObject bankDetails = new JSONObject();
		String qrImageUrl = "https://example.com/qr.png";

		bankDetails.put("account_holder", "John Doe");
		bankDetails.put("iban", "DE123456789");
		bankDetails.put("qr_image", qrImageUrl);

		transaction.put("bank_details", bankDetails);

		String result = service.buildOrderHistoryNotes(transaction, TRANSACTION_AMOUNT_DISPLAY, "SEPA");

		assertThat(result).isNotNull();
		assertThat(result).contains(qrImageUrl);
		assertThat(result).contains("img");
	}

	@Test
	public void buildOrderHistoryNotesGuaranteePendingStatusIncludesGuaranteeNote()
	{
		JSONObject transaction = createBasicTransaction();

		transaction.put("status", PAYMENT_STATUS_ON_HOLD);
		transaction.put("status_code", "75");

		String result = service.buildOrderHistoryNotes(transaction, TRANSACTION_AMOUNT_DISPLAY, "Guaranteed Invoice");

		assertThat(result).isNotNull();
	}

	@Test
	public void buildOrderHistoryNotesPartnerPaymentReferenceIncludesReference()
	{
		JSONObject transaction = createBasicTransaction();

		transaction.put("partner_payment_reference", "PARTNER-REF-123");

		String result = service.buildOrderHistoryNotes(transaction, TRANSACTION_AMOUNT_DISPLAY, "Multibanco");

		assertThat(result).isNotNull();
		assertThat(result).contains("PARTNER-REF-123");
	}

	private JSONObject createBasicTransaction()
	{
		JSONObject transaction = new JSONObject();

		transaction.put("tid", ORIGINAL_TID);
		transaction.put("amount", PAID_AMOUNT_STRING);
		transaction.put("status", TRANSACTION_STATUS);
		transaction.put("status_code", String.valueOf(SUCCESS_STATUS_CODE));

		return transaction;
	}
}

