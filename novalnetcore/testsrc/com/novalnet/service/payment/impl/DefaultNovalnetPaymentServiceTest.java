/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.jalo.user.User;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.util.localization.Localization;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.Model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.novalnet.dao.NovalnetDao;
import com.novalnet.dto.AddressForm;
import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetCallbackInfoModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitAchPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedInvoicePaymentModeModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.model.NovalnetPaymentRefInfoModel;
import com.novalnet.service.checkout.NovalnetCheckoutService;

/**
 * Unit tests for {@link DefaultNovalnetPaymentService}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultNovalnetPaymentServiceTest
{
	private static final String ORDER_CODE = "ORDER-1001";
	private static final String PAYMENT_CODE = "novalnetCreditCard";
	private static final String PAYMENT_NAME = "Credit Card";
	private static final String PAYMENT_STATUS = "SUCCESS";
	private static final long CALLBACK_TID = 123456789L;
	private static final int ORDER_PAID_AMOUNT = 5000;
	private static final String FIRST_NAME = "John";
	private static final String LAST_NAME = "Doe";
	private static final String FULL_NAME = "John Doe";
	private static final String CUSTOMER_NO = "8796093023001";
	private static final long REFERENCE_TRANSACTION_TID = 1122334455L;
	private static final int TRANSACTION_AMOUNT_IN_MINOR_UNITS = 10000;
	private static final int TRANSACTION_AMOUNT_IN_MAJOR_UNITS = 100;
	private static final int UNKNOWN_CURRENCY_TRANSACTION_AMOUNT = 500;
	private static final int ORDER_AMOUNT_IN_CENTS = 9999;
	private static final int DEFAULT_GUARANTEE_MINIMUM_AMOUNT = 999;
	private static final double TEST_CART_TOTAL_AMOUNT = 99.99;

	@InjectMocks
	private DefaultNovalnetPaymentService novalnetPaymentService;

	@Mock
	private ModelService modelService;

	@Mock
	private NovalnetDao novalnetDao;

	@Mock
	private PaymentModeService paymentModeService;

	@Mock
	private BaseStoreService baseStoreService;

	@Mock
	private NovalnetCheckoutService novalnetCheckoutService;

	@Mock
	private CustomerFacade customerFacade;

	@Mock
	private SessionService sessionService;

	@Mock
	private I18NFacade i18NFacade;

	@Mock
	private JaloSession jaloSession;

	@Mock
	private User jaloUser;

	@Mock
	private SessionContext sessionContext;

	@Mock
	private PK userPk;

	private MockedStatic<JaloSession> jaloSessionMock;
	private MockedStatic<Localization> localizationMock;

	@BeforeEach
	public void setUp()
	{
		jaloSessionMock = mockStatic(JaloSession.class);
		localizationMock = mockStatic(Localization.class);

		jaloSessionMock.when(JaloSession::getCurrentSession).thenReturn(jaloSession);
		when(jaloSession.getUser()).thenReturn(jaloUser);
		when(jaloSession.getSessionContext()).thenReturn(sessionContext);
		when(sessionContext.getLocale()).thenReturn(Locale.ENGLISH);
		when(jaloUser.getPK()).thenReturn(userPk);
		when(userPk.toString()).thenReturn(CUSTOMER_NO);

		localizationMock.when(() -> Localization.getLocalizedString(anyString())).thenReturn("localized-label");
	}

	@AfterEach
	public void tearDown()
	{
		jaloSessionMock.close();
		localizationMock.close();
	}

	@Test
	public void shouldReturnPaymentModelWhenPaymentInfoExists()
	{
		NovalnetPaymentInfoModel paymentInfoModel = mock(NovalnetPaymentInfoModel.class);
		NovalnetPaymentInfoModel expectedModel = mock(NovalnetPaymentInfoModel.class);
		PK paymentInfoPk = mock(PK.class);

		when(paymentInfoModel.getPk()).thenReturn(paymentInfoPk);
		when(modelService.get(paymentInfoPk)).thenReturn(expectedModel);

		List<NovalnetPaymentInfoModel> paymentInfo = List.of(paymentInfoModel);

		NovalnetPaymentInfoModel result = novalnetPaymentService.getPaymentModel(paymentInfo);

		assertThat(result).isSameAs(expectedModel);
		verify(paymentInfoModel).getPk();
		verify(modelService).get(paymentInfoPk);
	}

	@Test
	public void shouldThrowExceptionWhenPaymentInfoListIsEmpty()
	{
		List<NovalnetPaymentInfoModel> paymentInfo = List.of();

		assertThatThrownBy(() -> novalnetPaymentService.getPaymentModel(paymentInfo)).isInstanceOf(IndexOutOfBoundsException.class);

		verifyNoInteractions(modelService);
	}

	@Test
	public void shouldUpdateOrderStatusToCancelled()
	{
		OrderModel orderModel = mock(OrderModel.class);
		PK orderPk = mock(PK.class);

		when(novalnetDao.getOrderInfoModel(ORDER_CODE)).thenReturn(List.of(orderModel));
		when(orderModel.getPk()).thenReturn(orderPk);
		when(modelService.get(orderPk)).thenReturn(orderModel);

		novalnetPaymentService.updateCancelStatus(ORDER_CODE);

		verify(novalnetDao).getOrderInfoModel(ORDER_CODE);
		verify(orderModel).getPk();
		verify(modelService).get(orderPk);
		verify(orderModel).setStatus(OrderStatus.CANCELLED);
		verify(modelService).save(orderModel);
	}

	@Test
	public void shouldThrowExceptionWhenOrderInfoListIsNull()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_CODE)).thenReturn(null);

		assertThatThrownBy(() -> novalnetPaymentService.updateCancelStatus(ORDER_CODE)).isInstanceOf(NullPointerException.class);

		verify(novalnetDao).getOrderInfoModel(ORDER_CODE);
		verifyNoInteractions(modelService);
	}


	@Test
	public void shouldThrowExceptionWhenOrderInfoListIsEmpty()
	{
		when(novalnetDao.getOrderInfoModel(ORDER_CODE)).thenReturn(Collections.emptyList());

		assertThatThrownBy(() -> novalnetPaymentService.updateCancelStatus(ORDER_CODE))
				.isInstanceOf(IndexOutOfBoundsException.class);

		verifyNoInteractions(modelService);
	}

	@Test
	public void shouldReturnPaymentModeNameWhenPaymentModeExists()
	{
		PaymentModeModel paymentModeModel = mock(PaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode(PAYMENT_CODE)).thenReturn(paymentModeModel);
		when(paymentModeModel.getName()).thenReturn(PAYMENT_NAME);

		String result = novalnetPaymentService.getPaymentName(PAYMENT_CODE);

		assertThat(result).isEqualTo(PAYMENT_NAME);
		verify(paymentModeService).getPaymentModeForCode(PAYMENT_CODE);
		verify(paymentModeModel).getName();
	}

	@Test
	public void shouldReturnEmptyStringWhenPaymentModeDoesNotExist()
	{
		when(paymentModeService.getPaymentModeForCode(PAYMENT_CODE)).thenReturn(null);

		String result = novalnetPaymentService.getPaymentName(PAYMENT_CODE);

		assertThat(result).isEmpty();
		verify(paymentModeService).getPaymentModeForCode(PAYMENT_CODE);
	}

	@Test
	public void shouldReturnCurrentBaseStore()
	{
		BaseStoreModel expectedBaseStore = mock(BaseStoreModel.class);

		when(baseStoreService.getCurrentBaseStore()).thenReturn(expectedBaseStore);

		BaseStoreModel result = novalnetPaymentService.getBaseStoreModel();

		assertThat(result).isSameAs(expectedBaseStore);
		verify(baseStoreService).getCurrentBaseStore();
	}

	@Test
	public void shouldReturnNullWhenCurrentBaseStoreDoesNotExist()
	{
		when(baseStoreService.getCurrentBaseStore()).thenReturn(null);

		BaseStoreModel result = novalnetPaymentService.getBaseStoreModel();

		assertThat(result).isNull();
		verify(baseStoreService).getCurrentBaseStore();
	}

	@Test
	public void shouldUpdatePaymentGatewayStatus()
	{
		NovalnetPaymentInfoModel paymentInfoModel = mock(NovalnetPaymentInfoModel.class);
		PK paymentInfoPk = mock(PK.class);

		when(paymentInfoModel.getPk()).thenReturn(paymentInfoPk);
		when(modelService.get(paymentInfoPk)).thenReturn(paymentInfoModel);

		novalnetPaymentService.updatePaymentInfo(List.of(paymentInfoModel), PAYMENT_STATUS);

		verify(paymentInfoModel).getPk();
		verify(modelService).get(paymentInfoPk);
		verify(paymentInfoModel).setPaymentGatewayStatus(PAYMENT_STATUS);
		verify(modelService).save(paymentInfoModel);
	}

	@Test
	public void shouldThrowExceptionWhenPaymentInfoModelIsNotFound()
	{
		NovalnetPaymentInfoModel paymentInfoModel = mock(NovalnetPaymentInfoModel.class);
		PK paymentInfoPk = mock(PK.class);

		when(paymentInfoModel.getPk()).thenReturn(paymentInfoPk);
		when(modelService.get(paymentInfoPk)).thenReturn(null);

		assertThatThrownBy(() -> novalnetPaymentService.updatePaymentInfo(List.of(paymentInfoModel), PAYMENT_STATUS))
				.isInstanceOf(NullPointerException.class);

		verify(paymentInfoModel).getPk();
		verify(modelService).get(paymentInfoPk);
	}

	@Test
	public void shouldThrowExceptionWhenUpdatePaymentInfoReferenceListIsEmpty()
	{
		assertThatThrownBy(() -> novalnetPaymentService.updatePaymentInfo(Collections.emptyList(), PAYMENT_STATUS))
				.isInstanceOf(IndexOutOfBoundsException.class);

		verifyNoInteractions(modelService);
	}

	@Test
	public void shouldUpdateCallbackTidAndPaidAmount()
	{
		NovalnetCallbackInfoModel callbackInfoModel = mock(NovalnetCallbackInfoModel.class);
		PK callbackInfoPk = mock(PK.class);

		when(callbackInfoModel.getPk()).thenReturn(callbackInfoPk);
		when(modelService.get(callbackInfoPk)).thenReturn(callbackInfoModel);

		novalnetPaymentService.updateCallbackInfo(CALLBACK_TID, List.of(callbackInfoModel), ORDER_PAID_AMOUNT);

		verify(callbackInfoModel).getPk();
		verify(modelService).get(callbackInfoPk);
		verify(callbackInfoModel).setCallbackTid(CALLBACK_TID);
		verify(callbackInfoModel).setPaidAmount(ORDER_PAID_AMOUNT);
		verify(modelService).save(callbackInfoModel);
	}

	@Test
	public void shouldThrowExceptionWhenCallbackInfoListIsEmpty()
	{
		List<NovalnetCallbackInfoModel> orderReference = List.of();

		assertThatThrownBy(() -> novalnetPaymentService.updateCallbackInfo(CALLBACK_TID, orderReference, ORDER_PAID_AMOUNT))
				.isInstanceOf(IndexOutOfBoundsException.class);

		verifyNoInteractions(modelService);
	}

	@Test
	public void shouldPersistCreditCardReferenceInfoWhenPayloadIsComplete()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"1122334455\",\"payment_data\":{"
				+ "\"token\":\"tok-abc\",\"card_brand\":\"VISA\",\"card_holder\":\"John Doe\","
				+ "\"card_number\":\"1111\",\"card_expiry_month\":\"9\",\"card_expiry_year\":\"2028\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetCreditCard");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		NovalnetPaymentRefInfoModel saved = captor.getValue();

		assertThat(saved.getCardType()).isEqualTo("VISA");
		assertThat(saved.getExpiryDate()).isEqualTo("09 / 28");
		assertThat(saved.getOrginalTid()).isEqualTo(REFERENCE_TRANSACTION_TID);
		assertThat(saved.getPaymentType()).isEqualTo("novalnetCreditCard");
	}

	@Test
	public void shouldNormalizeGuaranteedSepaCodeToPlainSepaCode()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"999\",\"payment_data\":{\"token\":\"tok-sepa\","
				+ "\"iban\":\"DE00000000000000000000\",\"account_holder\":\"Jane Roe\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetGuaranteedDirectDebitSepa");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		assertThat(captor.getValue().getPaymentType()).isEqualTo("novalnetDirectDebitSepa");
		assertThat(captor.getValue().getAccountHolder()).isEqualTo("Jane Roe");
	}


	@Test
	public void shouldPersistAchReferenceInfoWhenPayloadIsComplete()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"77\",\"payment_data\":{\"token\":\"tok-ach\","
				+ "\"account_holder\":\"Alan Turing\",\"account_number\":\"6789\"," + "\"routing_number\":\"12345\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetDirectDebitAch");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		assertThat(captor.getValue().getAchAccountHolder()).isEqualTo("Alan Turing");
		assertThat(captor.getValue().getMaskedAchAccountNumber()).isEqualTo("6789");
		assertThat(captor.getValue().getMaskedAchRoutingNumber()).isEqualTo("12345");
	}


	@Test
	public void shouldPersistBothPaypalOptionalFieldsWhenPresent()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"56\",\"payment_data\":{\"token\":\"tok-pp2\","
				+ "\"paypal_transaction_id\":\"PPX-1\",\"paypal_account\":\"buyer@example.com\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetPayPal");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		assertThat(captor.getValue().getPaypalTransactionID()).isEqualTo("PPX-1");
		assertThat(captor.getValue().getPaypalEmailID()).isEqualTo("buyer@example.com");
	}

	@Test
	public void shouldLeavePaypalOptionalFieldsNullWhenAbsent()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"55\",\"payment_data\":{\"token\":\"tok-pp\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetPayPal");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		assertThat(captor.getValue().getPaypalTransactionID()).isNull();
		assertThat(captor.getValue().getPaypalEmailID()).isNull();
	}

	@Test
	public void shouldPersistGooglePayCardStyleFieldsWhenPayloadIsComplete()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"88\",\"payment_data\":{\"token\":\"tok-gp\","
				+ "\"card_brand\":\"MASTERCARD\",\"card_number\":\"4444\","
				+ "\"card_holder\":\"Grace Hopper\",\"card_expiry_month\":\"3\"," + "\"card_expiry_year\":\"2027\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetGooglePay");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		assertThat(captor.getValue().getCardType()).isEqualTo("MASTERCARD");
		assertThat(captor.getValue().getExpiryDate()).isEqualTo("03 / 27");
	}

	@Test
	public void shouldOnlySetApplePayFieldsThatArePresentInPayload()
	{
		StringBuilder response = new StringBuilder(
				"{\"transaction\":{\"tid\":\"89\",\"payment_data\":{\"token\":\"tok-ap\"," + "\"card_brand\":\"VISA\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetApplePay");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		assertThat(captor.getValue().getCardType()).isEqualTo("VISA");
		assertThat(captor.getValue().getCardHolder()).isNull();
		assertThat(captor.getValue().getExpiryDate()).isNull();
	}

	@Test
	public void shouldPersistOnlyBaseFieldsForUnrecognizedPaymentType()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"90\",\"payment_data\":{\"token\":\"tok-inv\"}}}");

		novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetInvoice");

		ArgumentCaptor<NovalnetPaymentRefInfoModel> captor = ArgumentCaptor.forClass(NovalnetPaymentRefInfoModel.class);

		verify(modelService).save(captor.capture());

		NovalnetPaymentRefInfoModel saved = captor.getValue();

		assertThat(saved.getPaymentType()).isEqualTo("novalnetInvoice");
		assertThat(saved.getToken()).isEqualTo("tok-inv");
		assertThat(saved.getCardType()).isNull();
		assertThat(saved.getMaskedAccountIban()).isNull();
	}

	@Test
	public void shouldThrowJsonExceptionWhenRequiredCreditCardFieldMissing()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"1\",\"payment_data\":{\"token\":\"tok\"}}}");

		assertThatThrownBy(() -> novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetCreditCard"))
				.isInstanceOf(JSONException.class);

		verify(modelService, never()).save(any());
	}


	@Test
	public void shouldThrowJsonExceptionWhenPayloadIsMalformed()
	{
		StringBuilder response = new StringBuilder("not-json-at-all");

		assertThatThrownBy(() -> novalnetPaymentService.handleReferenceTransactionInfo(response, CUSTOMER_NO, "novalnetCreditCard"))
				.isInstanceOf(JSONException.class);
	}


	@Test
	public void shouldThrowNumberFormatExceptionWhenCustomerNoIsNonNumeric()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"1\",\"payment_data\":{\"token\":\"tok\"}}}");

		assertThatThrownBy(() -> novalnetPaymentService.handleReferenceTransactionInfo(response, "not-a-number", "novalnetPayPal"))
				.isInstanceOf(NumberFormatException.class);

		verify(modelService, never()).save(any());
	}

	@Test
	public void shouldCreateTransactionEntryWithConvertedAmount()
	{
		CartModel cartModel = mock(CartModel.class);

		when(cartModel.getCode()).thenReturn("cart-001");

		CurrencyModel currency = mock(CurrencyModel.class);
		when(novalnetDao.getCurrencyForIsoCode("EUR")).thenReturn(currency);

		PaymentTransactionEntryModel entryModel = mock(PaymentTransactionEntryModel.class);

		when(modelService.create(PaymentTransactionEntryModel.class)).thenReturn(entryModel);

		PaymentTransactionEntryModel result = novalnetPaymentService.createTransactionEntry("req-1", cartModel,
				TRANSACTION_AMOUNT_IN_MINOR_UNITS, "captured", "EUR");

		assertThat(result).isSameAs(entryModel);

		verify(entryModel).setRequestId("req-1");
		verify(entryModel).setCode("cart-001");
		verify(entryModel).setCurrency(currency);
		verify(entryModel).setAmount(BigDecimal.valueOf(TRANSACTION_AMOUNT_IN_MAJOR_UNITS));
	}

	@Test
	public void shouldSetNullCurrencyWhenIsoCodeIsUnknown()
	{
		CartModel cartModel = mock(CartModel.class);

		when(cartModel.getCode()).thenReturn("cart-002");
		when(novalnetDao.getCurrencyForIsoCode("XXX")).thenReturn(null);

		PaymentTransactionEntryModel entryModel = mock(PaymentTransactionEntryModel.class);

		when(modelService.create(PaymentTransactionEntryModel.class)).thenReturn(entryModel);

		novalnetPaymentService.createTransactionEntry("req-2", cartModel, UNKNOWN_CURRENCY_TRANSACTION_AMOUNT, "note", "XXX");

		verify(entryModel).setCurrency(null);
	}

	@Test
	public void shouldCaptureReferenceTransactionWhenRegisteredCustomerOptsToStoreCreditCard()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"321\",\"payment_data\":{\"token\":\"tok\","
				+ "\"card_brand\":\"VISA\",\"card_holder\":\"John Doe\"," + "\"card_number\":\"1111\",\"card_expiry_month\":\"9\","
				+ "\"card_expiry_year\":\"2028\"}}}");

		JsonNode customerJson = mock(JsonNode.class);

		when(customerJson.path("customer_no")).thenReturn(TextNode.valueOf(CUSTOMER_NO));
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sessionService.getAttribute("novalnetCreditCardStorePaymentData")).thenReturn(true);

		novalnetPaymentService.handleStorePayment("novalnetCreditCard", response, customerJson);

		verify(modelService).save(any(NovalnetPaymentRefInfoModel.class));
	}


	@Test
	public void shouldNotCaptureReferenceTransactionForGuestUser()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"321\",\"payment_data\":{\"token\":\"tok\"}}}");

		JsonNode customerJson = mock(JsonNode.class);

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);

		novalnetPaymentService.handleStorePayment("novalnetCreditCard", response, customerJson);

		verify(modelService, never()).save(any());
		verify(sessionService, never()).getAttribute(anyString());
	}


	@Test
	public void shouldNotCaptureReferenceTransactionWhenStoreFlagIsFalse()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"321\",\"payment_data\":{\"token\":\"tok\"}}}");

		JsonNode customerJson = mock(JsonNode.class);

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);
		when(sessionService.getAttribute("novalnetCreditCardStorePaymentData")).thenReturn(false);

		novalnetPaymentService.handleStorePayment("novalnetCreditCard", response, customerJson);

		verify(modelService, never()).save(any());
	}

	@Test
	public void shouldNotCaptureReferenceTransactionForNonCreditCardPaymentType()
	{
		StringBuilder response = new StringBuilder("{\"transaction\":{\"tid\":\"321\",\"payment_data\":{\"token\":\"tok\"}}}");

		JsonNode customerJson = mock(JsonNode.class);

		novalnetPaymentService.handleStorePayment("novalnetPayPal", response, customerJson);

		verify(modelService, never()).save(any());
		verifyNoInteractions(sessionService);
	}


	@Test
	public void shouldPrefillAccountHolderFromDeliveryAddressForGuestCheckout() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(novalnetCheckoutService.getGuestEmail()).thenReturn("guest@example.com");
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(form).setGuaranteeAccountHolder(FULL_NAME);
		verify(form).setAccountHolder(FULL_NAME);
		verify(form).setAchAccountHolder(FULL_NAME);
		verify(model).addAttribute("customerFirstName", FIRST_NAME);
		verify(model).addAttribute("customerLastName", LAST_NAME);
		verify(model).addAttribute("email", "guest@example.com");
	}


	@Test
	public void shouldPrefillAccountHolderFromCustomerFacadeForRegisteredCustomer() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		CustomerData customerData = mock(CustomerData.class);

		when(customerData.getFirstName()).thenReturn("Ada");
		when(customerData.getLastName()).thenReturn("Lovelace");
		when(customerFacade.getCurrentCustomer()).thenReturn(customerData);
		when(novalnetCheckoutService.getGuestEmail()).thenReturn(null);
		when(jaloUser.getLogin()).thenReturn("ada@example.com");
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(form).setAccountHolder("Ada Lovelace");
		verify(model).addAttribute("email", "ada@example.com");
	}

	@Test
	public void shouldExposeOrderAmountInBothMajorUnitsAndCents() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(model).addAttribute("orderAmountCent", ORDER_AMOUNT_IN_CENTS);
		verify(model).addAttribute("currency", "EUR");
	}

	@Test
	public void shouldPreferBillingAddressOverDeliveryAddressWhenBothPresent() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		CCPaymentInfoData paymentInfo = mock(CCPaymentInfoData.class);

		AddressData billingAddress = buildAddress("addr-billing", "Billing", "Person", "DE");

		when(paymentInfo.getBillingAddress()).thenReturn(billingAddress);

		cartData.setPaymentInfo(paymentInfo);

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));
		when(i18NFacade.getRegionsForCountryIso("DE")).thenReturn(Collections.emptyList());

		stubAllPaymentModesInactive();

		ArgumentCaptor<AddressForm> addressFormCaptor = ArgumentCaptor.forClass(AddressForm.class);

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(form).setBillingAddress(addressFormCaptor.capture());

		assertThat(addressFormCaptor.getValue().getFirstName()).isEqualTo("Billing");
		assertThat(addressFormCaptor.getValue().getCountryIso()).isEqualTo("DE");

		verify(model).addAttribute("country", "DE");
	}

	@Test
	public void shouldDefaultLanguageToEnglishWhenSessionLocaleIsNull() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(sessionContext.getLocale()).thenReturn(null);
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(model).addAttribute("lang", "EN");
	}

	@Test
	public void shouldNotQueryPaymentRefInfoForGuestEvenWhenOneClickShoppingFlagIsTrue() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesActiveOneClickEligible();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(novalnetDao, never()).getPaymentRefInfo(anyString(), eq("novalnetCreditCard"));

		verify(model).addAttribute("novalnetCreditCardOneClickEnabled", false);
	}


	@Test
	public void shouldDefaultGuaranteedSepaMinimumAmountTo999WhenNotConfigured() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		NovalnetGuaranteedDirectDebitSepaPaymentModeModel guaranteedSepa = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeService
				.getPaymentModeForCode("novalnetGuaranteedDirectDebitSepa");

		when(guaranteedSepa.getActive()).thenReturn(true);
		when(guaranteedSepa.getNovalnetMinimumGuaranteeAmount()).thenReturn(null);

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(model).addAttribute("novalnetGuaranteedDirectDebitSepaMinAmount", DEFAULT_GUARANTEE_MINIMUM_AMOUNT);
	}

	@Test
	public void shouldNotAddGuaranteedInvoiceMinAmountAttributeWhenInactive() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(model, never()).addAttribute(eq("novalnetGuaranteedInvoiceMinAmount"), any());
	}

	@Test
	public void shouldSurfaceGooglePayAndApplePayZeroAmountBookingFlags() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(model).addAttribute(eq("novalnetGooglePayZeroAmountBooking"), any(Boolean.class));

		verify(model).addAttribute(eq("novalnetApplePayZeroAmountBooking"), any(Boolean.class));
	}

	@Test
	public void shouldPopulateModelAttributeForEveryConfiguredPaymentCode() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		verify(model).addAttribute(eq("novalnetCreditCard"), any());
		verify(model).addAttribute(eq("novalnetDirectDebitSepa"), any());
		verify(model).addAttribute(eq("novalnetPayPal"), any());
	}

	@Test
	public void shouldAddAllLocalizedLabelKeysToModel() throws CMSItemNotFoundException
	{
		Model model = mock(Model.class);
		NovalnetPaymentDetailsForm form = mock(NovalnetPaymentDetailsForm.class);
		CartData cartData = buildCartDataWithDeliveryAddressOnly();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);
		when(baseStoreService.getCurrentBaseStore()).thenReturn(mock(BaseStoreModel.class));

		stubAllPaymentModesInactive();

		novalnetPaymentService.addPaymentProcess(model, form, cartData);

		localizationMock.verify(() -> Localization.getLocalizedString("novalnet.endswith"), times(1));

		localizationMock.verify(() -> Localization.getLocalizedString("novalnet.expires"), times(1));

		localizationMock.verify(() -> Localization.getLocalizedString("novalnet.creditcardaddnew"), times(1));

		verify(model).addAttribute(eq("endswith"), anyString());
	}

	private CartData buildCartDataWithDeliveryAddressOnly()
	{
		CartData cartData = new CartData();

		PriceData total = new PriceData();
		total.setValue(BigDecimal.valueOf(TEST_CART_TOTAL_AMOUNT));
		total.setCurrencyIso("EUR");

		cartData.setTotalPriceWithTax(total);
		cartData.setDeliveryAddress(buildAddress("addr-1", FIRST_NAME, LAST_NAME, "US"));

		return cartData;
	}

	private AddressData buildAddress(String id, String firstName, String lastName, String countryIso)
	{
		AddressData address = new AddressData();

		address.setId(id);
		address.setFirstName(firstName);
		address.setLastName(lastName);
		address.setLine1("1 Test Street");
		address.setTown("Testville");
		address.setPostalCode("00000");

		CountryData country = new CountryData();
		country.setIsocode(countryIso);
		address.setCountry(country);

		return address;
	}

	private void stubAllPaymentModesInactive()
	{
		NovalnetCreditCardPaymentModeModel creditCard = mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode("novalnetCreditCard")).thenReturn(creditCard);

		NovalnetDirectDebitSepaPaymentModeModel sepa = mock(NovalnetDirectDebitSepaPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode("novalnetDirectDebitSepa")).thenReturn(sepa);

		NovalnetDirectDebitAchPaymentModeModel ach = mock(NovalnetDirectDebitAchPaymentModeModel.class);

		when(paymentModeService.getPaymentModeForCode("novalnetDirectDebitAch")).thenReturn(ach);

		NovalnetGuaranteedDirectDebitSepaPaymentModeModel guaranteedSepa = mock(
				NovalnetGuaranteedDirectDebitSepaPaymentModeModel.class);

		when(guaranteedSepa.getActive()).thenReturn(false);

		when(paymentModeService.getPaymentModeForCode("novalnetGuaranteedDirectDebitSepa")).thenReturn(guaranteedSepa);

		NovalnetGuaranteedInvoicePaymentModeModel guaranteedInvoice = mock(NovalnetGuaranteedInvoicePaymentModeModel.class);

		when(guaranteedInvoice.getActive()).thenReturn(false);

		when(paymentModeService.getPaymentModeForCode("novalnetGuaranteedInvoice")).thenReturn(guaranteedInvoice);

		when(paymentModeService.getPaymentModeForCode("novalnetGooglePay"))
				.thenReturn(mock(NovalnetGooglePayPaymentModeModel.class));

		when(paymentModeService.getPaymentModeForCode("novalnetApplePay")).thenReturn(mock(NovalnetApplePayPaymentModeModel.class));

		for (String code : List.of("novalnetPayPal", "novalnetInvoice", "novalnetPrepayment", "novalnetMultibanco",
				"novalnetOnlineBankTransfer", "novalnetBancontact", "novalnetIdeal", "novalnetTwint", "novalnetMbWay",
				"novalnetTrustly", "novalnetBlik", "novalnetWechatPay", "novalnetAlipay", "novalnetPrzelewy24", "novalnetEps",
				"novalnetPostFinance", "novalnetPostFinanceCard"))
		{
			when(paymentModeService.getPaymentModeForCode(code)).thenReturn(mock(PaymentModeModel.class));
		}
	}

	private void stubAllPaymentModesActiveOneClickEligible()
	{
		stubAllPaymentModesInactive();

		NovalnetCreditCardPaymentModeModel creditCard =
				(NovalnetCreditCardPaymentModeModel)
						paymentModeService.getPaymentModeForCode(
								"novalnetCreditCard");

		when(creditCard.getActive()).thenReturn(true);
		when(creditCard.getNovalnetOneClickShopping()).thenReturn(true);
	}
}

