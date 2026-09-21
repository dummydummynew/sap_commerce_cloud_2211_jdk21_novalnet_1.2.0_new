/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.dao.NovalnetCallbackDao;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.order.impl.DefaultNovalnetCallbackOrderService;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetCallbackServiceTest
{
	private static final String ORDER_CODE = "100001";
	private static final String PAYMENT_GATEWAY_STATUS = "CONFIRMED";
	private static final String PENDING_STATUS = "PENDING";
	private static final String ON_HOLD_STATUS = "ON_HOLD";
	private static final String FAILURE_STATUS = "FAILURE";
	private static final String DEACTIVATED_STATUS = "DEACTIVATED";
	private static final String INVALID_STATUS = "INVALID_STATUS";

	private static final String ORIGINAL_TID = "12345678";
	private static final String PAYMENT_NAME = "Invoice";
	private static final String AMOUNT = "100.00";

	private static final String EMPTY_VALUE = "";
	private static final String BLANK_VALUE = " ";

	private static final long CALLBACK_TID = 987654321L;
	private static final int PAID_AMOUNT = 5000;

	private static final String EXISTING_NOTES = "Existing notes";
	private static final String NEW_CALLBACK = "New callback";
	private static final String CALLBACK_ENTRY = "Callback entry";
	private static final String EXPECTED_EXISTING_NOTES = "Existing notes<br/><br/>New callback";
	private static final String EXPECTED_EMPTY_NOTES = "<br/><br/>New callback";

	private static final String CURRENCY_CODE = "EUR";

	private static final String TID_KEY = "tid";
	private static final String AMOUNT_KEY = "amount";
	private static final String TEST_MODE_KEY = "test_mode";
	private static final String STATUS_CODE_KEY = "status_code";
	private static final String STATUS_KEY = "status";
	private static final String DUE_DATE_KEY = "due_date";
	private static final String BANK_DETAILS_KEY = "bank_details";
	private static final String PARTNER_PAYMENT_REFERENCE_KEY = "partner_payment_reference";

	private static final String TRANSACTION_AMOUNT = "10000";
	private static final String ZERO_TRANSACTION_AMOUNT = "0";
	private static final String TEST_MODE_DISABLED = "0";
	private static final String TEST_MODE_ENABLED = "1";
	private static final String SUCCESS_STATUS_CODE = "100";
	private static final String GUARANTEE_PENDING_STATUS_CODE = "75";

	private static final String ACCOUNT_HOLDER = "account_holder";
	private static final String ACCOUNT_HOLDER_NAME = "John Doe";
	private static final String IBAN = "iban";
	private static final String IBAN_VALUE = "DE123456789";
	private static final String BIC = "bic";
	private static final String BIC_VALUE = "ABCDEF";
	private static final String BANK_NAME = "bank_name";
	private static final String BANK_NAME_VALUE = "Test Bank";
	private static final String BANK_PLACE = "bank_place";
	private static final String BANK_PLACE_VALUE = "Berlin";
	private static final String DUE_DATE_VALUE = "15-09-2026";

	private static final String PARTNER_PAYMENT_REFERENCE_VALUE = "PARTNER123";

	private static final String PAYMENT_LABEL = "Payment";
	private static final String TRANSACTION_ID_LABEL = "Transaction ID";
	private static final String ZERO_AMOUNT_MESSAGE = "Zero amount transaction";
	private static final String TEST_TRANSACTION_MESSAGE = "Test transaction";
	private static final String GUARANTEE_PENDING_MESSAGE = "Guarantee payment pending";

	private static final String TEST_LOCALIZATION_KEY = "test.key";
	private static final String TEST_LOCALIZATION_VALUE = "Test Label";

	@InjectMocks
	private DefaultNovalnetCallbackOrderService testObj;

	@Mock
	private ModelService modelService;

	@Mock
	private NovalnetCallbackDao novalnetCallbackDao;

	@Mock
	private CommonI18NService commonI18NService;

	@Mock
	private OrderModel order;

	@Mock
	private NovalnetPaymentInfoModel paymentInfo;

	@Mock
	private NovalnetCallbackInfoModel callbackInfo;

	@Mock
	private LanguageModel language;

	@Mock
	private CurrencyModel currency;

	@Test
	public void shouldUpdateOrderToPaymentNotCapturedWhenStatusIsPending()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PENDING_STATUS);

		testObj.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}


	@Test
	public void shouldUpdateOrderToPaymentAuthorizedWhenStatusIsOnHold()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(ON_HOLD_STATUS);

		testObj.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.PAYMENT_AUTHORIZED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}


	@Test
	public void shouldCompleteOrderWhenStatusIsConfirmed()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(PAYMENT_GATEWAY_STATUS);

		testObj.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.COMPLETED);
		verify(order).setPaymentStatus(PaymentStatus.PAID);
		verify(modelService).save(order);
	}


	@Test
	public void shouldCancelOrderWhenStatusIsFailure()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(FAILURE_STATUS);

		testObj.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.CANCELLED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}


	@Test
	public void shouldCancelOrderWhenStatusIsDeactivated()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(DEACTIVATED_STATUS);

		testObj.updateOrderStatus(ORDER_CODE, paymentInfo, order);

		verify(order).setStatus(OrderStatus.CANCELLED);
		verify(order).setPaymentStatus(PaymentStatus.NOTPAID);
		verify(modelService).save(order);
	}


	@Test
	public void shouldThrowExceptionWhenOrderCodeIsBlank()
	{
		assertThatThrownBy(() -> testObj.updateOrderStatus(EMPTY_VALUE, paymentInfo, order))
				.isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void shouldThrowExceptionWhenPaymentInfoIsNull()
	{
		assertThatThrownBy(() -> testObj.updateOrderStatus(ORDER_CODE, null, order)).isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void shouldThrowExceptionWhenPaymentGatewayStatusIsInvalid()
	{
		when(paymentInfo.getPaymentGatewayStatus()).thenReturn(INVALID_STATUS);

		assertThatThrownBy(() -> testObj.updateOrderStatus(ORDER_CODE, paymentInfo, order))
				.isInstanceOf(RequestParameterException.class);
	}


	@Test
	public void shouldCancelOrderWhenCancelStatusIsUpdated()
	{
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		testObj.updateCancelStatus(ORDER_CODE);

		verify(order).setStatus(OrderStatus.CANCELLED);
		verify(modelService).save(order);
	}


	@Test
	public void shouldThrowExceptionWhenCancelOrderCodeIsBlank()
	{
		assertThatThrownBy(() -> testObj.updateCancelStatus(BLANK_VALUE)).isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldSetPartiallyPaidStatusWhenPartPaidUpdateIsRequested()
	{
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		testObj.updatePartPaidStatus(ORDER_CODE);

		verify(order).setPaymentStatus(PaymentStatus.PARTPAID);
		verify(modelService).save(order);
	}


	@Test
	public void shouldThrowExceptionWhenPartPaidOrderCodeIsBlank()
	{
		assertThatThrownBy(() -> testObj.updatePartPaidStatus(EMPTY_VALUE)).isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldUpdateLatestPaymentInfoStatus()
	{
		when(novalnetCallbackDao.getLatestNovalnetPaymentInfo(ORDER_CODE)).thenReturn(paymentInfo);

		testObj.updatePaymentInfo(ORDER_CODE, PAYMENT_GATEWAY_STATUS);

		verify(paymentInfo).setPaymentGatewayStatus(PAYMENT_GATEWAY_STATUS);
		verify(modelService).save(paymentInfo);
	}


	@Test
	public void shouldThrowExceptionWhenPaymentInfoOrderCodeIsBlank()
	{
		assertThatThrownBy(() -> testObj.updatePaymentInfo(EMPTY_VALUE, PAYMENT_GATEWAY_STATUS))
				.isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldThrowExceptionWhenPaymentGatewayStatusIsBlank()
	{
		assertThatThrownBy(() -> testObj.updatePaymentInfo(ORDER_CODE, BLANK_VALUE)).isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldUpdateCallbackInfo()
	{
		when(novalnetCallbackDao.findCallbackInfoByOriginalTid(ORIGINAL_TID)).thenReturn(callbackInfo);

		testObj.updateCallbackInfo(CALLBACK_TID, ORIGINAL_TID, PAID_AMOUNT);

		verify(callbackInfo).setCallbackTid(CALLBACK_TID);
		verify(callbackInfo).setPaidAmount(PAID_AMOUNT);
		verify(modelService).save(callbackInfo);
	}


	@Test
	public void shouldThrowExceptionWhenOriginalTidIsBlank()
	{
		assertThatThrownBy(() -> testObj.updateCallbackInfo(CALLBACK_TID, EMPTY_VALUE, PAID_AMOUNT))
				.isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldAppendCallbackCommentsToExistingHistoryNotes()
	{
		when(novalnetCallbackDao.getLatestNovalnetPaymentInfo(ORDER_CODE)).thenReturn(paymentInfo);
		when(paymentInfo.getOrderHistoryNotes()).thenReturn(EXISTING_NOTES);
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		OrderHistoryEntryModel entry = mock(OrderHistoryEntryModel.class);

		when(modelService.create(OrderHistoryEntryModel.class)).thenReturn(entry);

		testObj.updateCallbackComments(NEW_CALLBACK, ORDER_CODE, PAYMENT_GATEWAY_STATUS, CALLBACK_ENTRY);

		verify(paymentInfo).setOrderHistoryNotes(EXPECTED_EXISTING_NOTES);
		verify(paymentInfo).setPaymentGatewayStatus(PAYMENT_GATEWAY_STATUS);
		verify(entry).setOrder(order);
		verify(entry).setDescription(CALLBACK_ENTRY);
		verify(modelService).saveAll(paymentInfo, entry);
	}


	@Test
	public void shouldCreateCallbackHistoryNotesWhenExistingNotesAreNull()
	{
		when(novalnetCallbackDao.getLatestNovalnetPaymentInfo(ORDER_CODE)).thenReturn(paymentInfo);
		when(paymentInfo.getOrderHistoryNotes()).thenReturn(null);
		when(novalnetCallbackDao.findOrderByCode(ORDER_CODE)).thenReturn(order);

		OrderHistoryEntryModel entry = mock(OrderHistoryEntryModel.class);

		when(modelService.create(OrderHistoryEntryModel.class)).thenReturn(entry);

		testObj.updateCallbackComments(NEW_CALLBACK, ORDER_CODE, PAYMENT_GATEWAY_STATUS, CALLBACK_ENTRY);

		verify(paymentInfo).setOrderHistoryNotes(EXPECTED_EMPTY_NOTES);
		verify(paymentInfo).setPaymentGatewayStatus(PAYMENT_GATEWAY_STATUS);
		verify(entry).setOrder(order);
		verify(entry).setDescription(CALLBACK_ENTRY);
		verify(modelService).saveAll(paymentInfo, entry);
	}


	@Test
	public void shouldThrowExceptionWhenCallbackCommentOrderCodeIsBlank()
	{
		assertThatThrownBy(() -> testObj.updateCallbackComments("Comment", EMPTY_VALUE, PAYMENT_GATEWAY_STATUS, "Entry"))
				.isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldThrowExceptionWhenCallbackCommentStatusIsBlank()
	{
		assertThatThrownBy(() -> testObj.updateCallbackComments("Comment", ORDER_CODE, EMPTY_VALUE, "Entry"))
				.isInstanceOf(RequestParameterException.class);

		verifyNoInteractions(novalnetCallbackDao, modelService);
	}


	@Test
	public void shouldSetSessionLanguageFromOrder()
	{
		when(order.getLanguage()).thenReturn(language);

		testObj.setSessionLanguage(order);

		verify(commonI18NService).setCurrentLanguage(language);
	}


	@Test
	public void shouldReturnLocaleForOrderLanguage()
	{
		when(order.getLanguage()).thenReturn(language);
		when(commonI18NService.getLocaleForLanguage(language)).thenReturn(Locale.GERMANY);

		Locale result = testObj.getLocale(order);

		assertThat(result).isEqualTo(Locale.GERMANY);
	}


	@Test
	public void shouldReturnCurrencyForCurrencyCode()
	{
		when(commonI18NService.getCurrency(CURRENCY_CODE)).thenReturn(currency);

		CurrencyModel result = testObj.getCurrency(CURRENCY_CODE);

		assertThat(result).isEqualTo(currency);
	}


	@Test
	public void shouldBuildOrderHistoryNotesForTransaction()
	{
		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, TRANSACTION_AMOUNT);
		transaction.put(TEST_MODE_KEY, TEST_MODE_DISABLED);
		transaction.put(STATUS_CODE_KEY, SUCCESS_STATUS_CODE);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.paymentname")).thenReturn(PAYMENT_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.transactionID")).thenReturn(TRANSACTION_ID_LABEL);

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).contains("Payment : Invoice").contains("Transaction ID 12345678");
		}
	}

	@Test
	public void shouldIncludeZeroAmountMessageWhenTransactionAmountIsZero()
	{
		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, ZERO_TRANSACTION_AMOUNT);
		transaction.put(STATUS_CODE_KEY, SUCCESS_STATUS_CODE);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.paymentname")).thenReturn(PAYMENT_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.transactionID")).thenReturn(TRANSACTION_ID_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.zeroAmountTransactionText"))
					.thenReturn(ZERO_AMOUNT_MESSAGE);

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).contains(ZERO_AMOUNT_MESSAGE);
		}
	}

	@Test
	public void shouldIncludeTestModeMessageWhenTransactionIsInTestMode()
	{
		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, TRANSACTION_AMOUNT);
		transaction.put(TEST_MODE_KEY, TEST_MODE_ENABLED);
		transaction.put(STATUS_CODE_KEY, SUCCESS_STATUS_CODE);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.paymentname")).thenReturn(PAYMENT_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.transactionID")).thenReturn(TRANSACTION_ID_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.testOrderText"))
					.thenReturn(TEST_TRANSACTION_MESSAGE);

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).contains(TEST_TRANSACTION_MESSAGE);
		}
	}


	@Test
	public void shouldIncludeBankDetailsWhenBankInformationIsAvailable()
	{
		JSONObject bankDetails = new JSONObject();
		bankDetails.put(ACCOUNT_HOLDER, ACCOUNT_HOLDER_NAME);
		bankDetails.put(IBAN, IBAN_VALUE);
		bankDetails.put(BIC, BIC_VALUE);
		bankDetails.put(BANK_NAME, BANK_NAME_VALUE);
		bankDetails.put(BANK_PLACE, BANK_PLACE_VALUE);

		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, TRANSACTION_AMOUNT);
		transaction.put(STATUS_CODE_KEY, SUCCESS_STATUS_CODE);
		transaction.put(STATUS_KEY, PAYMENT_GATEWAY_STATUS);
		transaction.put(DUE_DATE_KEY, DUE_DATE_VALUE);
		transaction.put(BANK_DETAILS_KEY, bankDetails);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString(any())).thenAnswer(invocation -> invocation.getArgument(0));

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).contains(ACCOUNT_HOLDER_NAME).contains(IBAN_VALUE).contains(BIC_VALUE).contains(BANK_NAME_VALUE)
					.contains(BANK_PLACE_VALUE).contains(ORIGINAL_TID);
		}
	}

	@Test
	public void shouldIncludeGuaranteePendingMessageWhenStatusCodeIs75()
	{
		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, TRANSACTION_AMOUNT);
		transaction.put(STATUS_CODE_KEY, GUARANTEE_PENDING_STATUS_CODE);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.paymentname")).thenReturn(PAYMENT_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.transactionID")).thenReturn(TRANSACTION_ID_LABEL);
			localizationMock.when(() -> Localization.getLocalizedString("novalnet.guaranteePendingNote"))
					.thenReturn(GUARANTEE_PENDING_MESSAGE);

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).contains(GUARANTEE_PENDING_MESSAGE);
		}
	}

	@Test
	public void shouldIncludePartnerPaymentReferenceWhenAvailable()
	{
		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, TRANSACTION_AMOUNT);
		transaction.put(STATUS_CODE_KEY, SUCCESS_STATUS_CODE);
		transaction.put(PARTNER_PAYMENT_REFERENCE_KEY, PARTNER_PAYMENT_REFERENCE_VALUE);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString(any())).thenAnswer(invocation -> invocation.getArgument(0));

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).contains(PARTNER_PAYMENT_REFERENCE_VALUE);
		}
	}

	@Test
	public void shouldOmitPartnerPaymentReferenceWhenUnavailable()
	{
		JSONObject transaction = new JSONObject();
		transaction.put(TID_KEY, ORIGINAL_TID);
		transaction.put(AMOUNT_KEY, TRANSACTION_AMOUNT);
		transaction.put(STATUS_CODE_KEY, SUCCESS_STATUS_CODE);

		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString(any())).thenAnswer(invocation -> invocation.getArgument(0));

			String result = testObj.buildOrderHistoryNotes(transaction, AMOUNT, PAYMENT_NAME);

			assertThat(result).doesNotContain(PARTNER_PAYMENT_REFERENCE_KEY);
		}
	}


	@Test
	public void shouldReturnLocalizedLabel()
	{
		try (MockedStatic<Localization> localizationMock = mockStatic(Localization.class))
		{
			localizationMock.when(() -> Localization.getLocalizedString(TEST_LOCALIZATION_KEY)).thenReturn(TEST_LOCALIZATION_VALUE);

			String result = testObj.getLabel(TEST_LOCALIZATION_KEY);

			assertThat(result).isEqualTo(TEST_LOCALIZATION_VALUE);
		}
	}
}
