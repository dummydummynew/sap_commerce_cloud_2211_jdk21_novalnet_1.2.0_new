
package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.localization.Localization;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novalnet.service.checkout.NovalnetCheckoutService;
import com.novalnet.service.http.NovalnetApiService;
import com.novalnet.service.payment.NovalnetEndpointConfigService;
import com.novalnet.service.payment.NovalnetPaymentService;


/**
 * Unit tests for {@link NovalnetTransactionResultService}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
public class NovalnetTransactionResultServiceTest
{
	private static final String ATTR_SELECTED_PAYMENT = "selectedPaymentMethodId";
	private static final String ATTR_ZERO_AMOUNT_BOOKING = "novalnetZeroAmountBooking";
	private static final String ATTR_CHECKOUT_ERROR = "novalnetCheckoutError";
	private static final String ATTR_ORDER_DATA = "novalnetOrderData";
	private static final String ATTR_TID = "tid";
	private static final String ATTR_EMAIL = "email";
	private static final String ATTR_CC_STORE_PAYMENT_DATA = "novalnetCreditCardStorePaymentData";

	private static final String CREDIT_CARD = "novalnetCreditCard";
	private static final String GOOGLE_PAY = "novalnetGooglePay";
	private static final String APPLE_PAY = "novalnetApplePay";
	private static final String DIRECT_PAYMENT = "novalnetPrepayment";

	private static final String TID = "177100000001";
	private static final String CUSTOMER_NO = "CUST-001";
	private static final String EMAIL = "buyer@example.com";
	private static final String CURRENCY = "EUR";
	private static final int AMOUNT_CENTS = 1000;
	private static final String ORDER_CODE = "00001234";

	private static final String STATUS_CONFIRMED = "CONFIRMED";
	private static final String STATUS_FAILURE = "FAILURE";

	private static final String TRANSACTION_DETAILS_URL = "https://payport.novalnet.de/v2/transaction/details";

	private static final String TRANSACTION_UPDATE_URL = "https://payport.novalnet.de/v2/transaction/update";

	private static final String UNKNOWN_ERROR_KEY = "novalnet.unknown.error";

	private final ObjectMapper mapper = new ObjectMapper();

	private NovalnetTransactionResultService service;

	@Mock
	private NovalnetApiService novalnetApiService;

	@Mock
	private NovalnetEndpointConfigService novalnetEndpointConfigService;

	@Mock
	private SessionService sessionService;

	@Mock
	private NovalnetCheckoutService novalnetCheckoutService;

	@Mock
	private NovalnetPaymentService novalnetPaymentService;

	@Mock
	private JaloSession jaloSession;

	@Mock
	private SessionContext sessionContext;

	@Mock
	private AddressData addressData;

	private MockedStatic<JaloSession> jaloSessionMock;
	private MockedStatic<Localization> localizationMock;


	@BeforeEach
	void setUp()
	{
		service = new NovalnetTransactionResultService();

		ReflectionTestUtils.setField(service, "novalnetApiService", novalnetApiService);

		ReflectionTestUtils.setField(service, "novalnetEndpointConfigService",
				novalnetEndpointConfigService);

		ReflectionTestUtils.setField(service, "sessionService", sessionService);

		ReflectionTestUtils.setField(service, "novalnetCheckoutService", novalnetCheckoutService);

		ReflectionTestUtils.setField(service, "novalnetPaymentService", novalnetPaymentService);

		jaloSessionMock = mockStatic(JaloSession.class);

		jaloSessionMock.when(JaloSession::getCurrentSession).thenReturn(jaloSession);

		lenient().when(jaloSession.getSessionContext()).thenReturn(sessionContext);

		lenient().when(sessionContext.getLocale()).thenReturn(Locale.US);

		localizationMock = mockStatic(Localization.class);

		localizationMock.when(() -> Localization.getLocalizedString(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		lenient().when(novalnetEndpointConfigService.getTransactionDetailsUrl()).thenReturn(TRANSACTION_DETAILS_URL);

		lenient().when(novalnetEndpointConfigService.getTransactionUpdateUrl()).thenReturn(TRANSACTION_UPDATE_URL);
	}


	@AfterEach
	void tearDown()
	{
		jaloSessionMock.close();
		localizationMock.close();
	}


	private Map<String, String> buildResultMap(final String tid, final String statusDesc)
	{
		final Map<String, String> map = new HashMap<>();

		map.put("tid", tid);
		map.put("status_desc", statusDesc);

		return map;
	}


	private StringBuilder confirmedTransactionResponse(final String status, final String testMode, final boolean withStatusText)
	{
		final String resultBlock = withStatusText ? "{\"status_text\":\"SUCCESS\"}" : "{}";

		final String json = "{" + "\"transaction\":{" + "\"status\":\"" + status + "\"," + "\"tid\":\"" + TID + "\","
				+ "\"amount\":" + AMOUNT_CENTS + "," + "\"currency\":\"" + CURRENCY + "\"," + "\"test_mode\":\"" + testMode + "\""
				+ "}," + "\"customer\":{" + "\"email\":\"" + EMAIL + "\"," + "\"customer_no\":\"" + CUSTOMER_NO + "\"" + "},"
				+ "\"result\":" + resultBlock + "}";

		return new StringBuilder(json);
	}


	private StringBuilder failedTransactionResponse(final boolean withStatusText)
	{
		final String resultBlock = withStatusText ? "{\"status_text\":\"Card declined\"}" : "{}";

		final String json = "{" + "\"transaction\":{\"status\":\"" + STATUS_FAILURE + "\"}," + "\"customer\":{}," + "\"result\":"
				+ resultBlock + "}";

		return new StringBuilder(json);
	}


	private void stubOrderCreation() throws InvalidCartException
	{
		final OrderData orderData = new OrderData();
		orderData.setCode(ORDER_CODE);

		lenient().when(novalnetCheckoutService.saveOrderData(anyString(), anyString(), anyString(), anyInt(), anyString(),
				anyString(), anyString(), any(), anyString())).thenReturn(orderData);
	}


	@Test
	void shouldReturnTrueWhenTransactionConfirmed() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		final boolean result = service.processTransaction(buildResultMap(TID, null));

		assertThat(result).isTrue();
	}


	@Test
	void shouldStoreSessionAttributesWhenTransactionConfirmed() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		service.processTransaction(buildResultMap(TID, null));

		verify(sessionService).setAttribute(eq(ATTR_TID), anyString());

		verify(sessionService).setAttribute(ATTR_EMAIL, EMAIL);

		verify(sessionService).setAttribute(eq(ATTR_ORDER_DATA), any(OrderData.class));

		verify(sessionService, never()).setAttribute(eq(ATTR_CHECKOUT_ERROR), any());
	}


	@Test
	void shouldPassCorrectValuesToSaveOrderData() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		final ArgumentCaptor<String> commentsCaptor = ArgumentCaptor.forClass(String.class);

		service.processTransaction(buildResultMap(TID, null));

		verify(novalnetCheckoutService).saveOrderData(commentsCaptor.capture(), eq(DIRECT_PAYMENT), eq(STATUS_CONFIRMED),
				eq(AMOUNT_CENTS), eq(CURRENCY), eq(TID), eq(EMAIL), any(), eq(""));

		assertThat(commentsCaptor.getValue()).contains("Prepayment").contains(TID);
	}


	@Test
	void shouldAppendTestModeNoteWhenTestModeFlagSet() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "1", true));

		stubOrderCreation();

		final ArgumentCaptor<String> commentsCaptor = ArgumentCaptor.forClass(String.class);

		service.processTransaction(buildResultMap(TID, null));

		verify(novalnetCheckoutService).saveOrderData(commentsCaptor.capture(), anyString(), anyString(), anyInt(), anyString(),
				anyString(), anyString(), any(), anyString());

		assertThat(commentsCaptor.getValue()).contains("novalnet.testOrderText");
	}


	@Test
	void shouldAppendZeroAmountBookingNoteWhenFlagIsTrue() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(sessionService.<Boolean> getAttribute(ATTR_ZERO_AMOUNT_BOOKING)).thenReturn(Boolean.TRUE);

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		final ArgumentCaptor<String> commentsCaptor = ArgumentCaptor.forClass(String.class);

		service.processTransaction(buildResultMap(TID, null));

		verify(novalnetCheckoutService).saveOrderData(commentsCaptor.capture(), anyString(), anyString(), anyInt(), anyString(),
				anyString(), anyString(), any(), anyString());

		assertThat(commentsCaptor.getValue()).contains("novalnet.zeroAmountBooking");

		verify(sessionService).removeAttribute(ATTR_ZERO_AMOUNT_BOOKING);
	}


	@Test
	void shouldNotAppendZeroAmountBookingNoteWhenFlagIsFalse() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(sessionService.<Boolean> getAttribute(ATTR_ZERO_AMOUNT_BOOKING)).thenReturn(Boolean.FALSE);

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		final ArgumentCaptor<String> commentsCaptor = ArgumentCaptor.forClass(String.class);

		service.processTransaction(buildResultMap(TID, null));

		verify(novalnetCheckoutService).saveOrderData(commentsCaptor.capture(), anyString(), anyString(), anyInt(), anyString(),
				anyString(), anyString(), any(), anyString());

		assertThat(commentsCaptor.getValue()).doesNotContain("novalnet.zeroAmountBooking");
	}


	@Test
	void shouldSendUpdateRequestWithOrderCode() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		final ArgumentCaptor<String> updateJsonCaptor = ArgumentCaptor.forClass(String.class);

		service.processTransaction(buildResultMap(TID, null));

		verify(novalnetApiService).sendRequest(eq(TRANSACTION_UPDATE_URL), updateJsonCaptor.capture());

		assertThat(updateJsonCaptor.getValue()).contains(ORDER_CODE);
	}


	@Test
	void shouldReturnTrueWhenStatusIsOnHold() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse("ON_HOLD", "0", true));

		stubOrderCreation();

		assertThat(service.processTransaction(buildResultMap(TID, null))).isTrue();
	}


	@Test
	void shouldReturnTrueWhenStatusIsPending() throws InvalidCartException
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse("PENDING", "0", true));

		stubOrderCreation();

		assertThat(service.processTransaction(buildResultMap(TID, null))).isTrue();
	}


	@Test
	void shouldFallBackToDefaultLanguageWhenLocaleIsNull() throws InvalidCartException
	{
		when(sessionContext.getLocale()).thenReturn(null);

		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		stubOrderCreation();

		assertThat(service.processTransaction(buildResultMap(TID, null))).isTrue();
	}


	@Test
	void shouldReturnFalseWithStatusTextErrorWhenTransactionDeclined() throws InvalidCartException
	{
		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString())).thenReturn(failedTransactionResponse(true));

		final boolean result = service.processTransaction(buildResultMap(TID, "Insufficient funds"));

		assertThat(result).isFalse();

		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "Card declined");

		verify(novalnetCheckoutService, never()).saveOrderData(anyString(), anyString(), anyString(), anyInt(), anyString(),
				anyString(), anyString(), any(), anyString());
	}


	@Test
	void shouldFallBackToStatusDescWhenStatusTextMissing()
	{
		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString())).thenReturn(failedTransactionResponse(false));

		service.processTransaction(buildResultMap(TID, "Insufficient funds"));

		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, "Insufficient funds");
	}


	@Test
	void shouldClearSessionAttributesWhenTransactionFails()
	{
		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString())).thenReturn(failedTransactionResponse(true));

		service.processTransaction(buildResultMap(TID, "Insufficient funds"));

		verify(sessionService).setAttribute("novalnetOrderCurrency", null);

		verify(sessionService).setAttribute("novalnetOrderAmount", null);

		verify(sessionService).setAttribute("novalnetCustomerParams", null);

		verify(sessionService).setAttribute("novalnetRedirectPaymentTestModeValue", null);

		verify(sessionService).setAttribute("novalnetRedirectPaymentName", null);

		verify(sessionService).setAttribute("novalnetCreditCardPanHash", null);

		verify(sessionService).setAttribute("paymentAccessKey", null);
	}


	@Test
	void shouldReturnFalseWhenResponseIsMalformedJson()
	{
		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(new StringBuilder("{not-valid-json"));

		final boolean result = service.processTransaction(buildResultMap(TID, null));

		assertThat(result).isFalse();

		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, UNKNOWN_ERROR_KEY);

		verify(novalnetApiService, never()).sendRequest(eq(TRANSACTION_UPDATE_URL), anyString());
	}


	@Test
	void shouldReturnFalseWhenOrderCreationFails() throws Exception
	{
		when(sessionService.<String> getAttribute(ATTR_SELECTED_PAYMENT)).thenReturn(DIRECT_PAYMENT);

		when(novalnetPaymentService.getPaymentName(DIRECT_PAYMENT)).thenReturn("Prepayment");

		when(novalnetApiService.sendRequest(eq(TRANSACTION_DETAILS_URL), anyString()))
				.thenReturn(confirmedTransactionResponse(STATUS_CONFIRMED, "0", true));

		when(novalnetCheckoutService.saveOrderData(anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(),
				anyString(), any(), anyString())).thenThrow(new InvalidCartException("Cart is invalid"));

		final boolean result = service.processTransaction(buildResultMap(TID, null));

		assertThat(result).isFalse();

		verify(sessionService).setAttribute(ATTR_CHECKOUT_ERROR, UNKNOWN_ERROR_KEY);

		verify(novalnetApiService, never()).sendRequest(eq(TRANSACTION_UPDATE_URL), anyString());

		verify(sessionService, never()).setAttribute(eq(ATTR_ORDER_DATA), any());
	}


	private JsonNode customerJsonWithNumber() throws Exception
	{
		return mapper.readTree("{\"customer_no\":\"" + CUSTOMER_NO + "\"}");
	}


	private StringBuilder responseWithToken()
	{
		return new StringBuilder("{\"transaction\":{\"payment_data\":{\"token\":\"tok_123\"}}}");
	}


	private StringBuilder responseWithoutToken()
	{
		return new StringBuilder("{\"transaction\":{\"payment_data\":{}}}");
	}


	private StringBuilder responseWithoutPaymentData()
	{
		return new StringBuilder("{\"transaction\":{}}");
	}


	private StringBuilder responseWithoutTransaction()
	{
		return new StringBuilder("{}");
	}


	@Test
	void shouldStoreReferenceWhenCreditCardRegisteredCustomerOptedIn() throws Exception
	{
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute(ATTR_CC_STORE_PAYMENT_DATA)).thenReturn(Boolean.TRUE);

		final StringBuilder response = new StringBuilder("{}");
		final JsonNode customer = customerJsonWithNumber();

		service.handleStorePayment(CREDIT_CARD, response, customer);

		verify(novalnetPaymentService).handleReferenceTransactionInfo(response, CUSTOMER_NO, CREDIT_CARD);
	}


	@Test
	void shouldNotStoreReferenceWhenCreditCardGuestUser() throws Exception
	{
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);

		service.handleStorePayment(CREDIT_CARD, new StringBuilder("{}"), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());

		verify(sessionService, never()).getAttribute(ATTR_CC_STORE_PAYMENT_DATA);
	}


	@Test
	void shouldNotStoreReferenceWhenCreditCardOptOut() throws Exception
	{
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute(ATTR_CC_STORE_PAYMENT_DATA)).thenReturn(Boolean.FALSE);

		service.handleStorePayment(CREDIT_CARD, new StringBuilder("{}"), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());
	}


	@Test
	void shouldStoreReferenceWhenWalletPaymentHasTokenAndOptedIn() throws Exception
	{
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute(GOOGLE_PAY + "StorePaymentData")).thenReturn(Boolean.TRUE);

		final StringBuilder response = responseWithToken();
		final JsonNode customer = customerJsonWithNumber();

		service.handleStorePayment(GOOGLE_PAY, response, customer);

		verify(novalnetPaymentService).handleReferenceTransactionInfo(response, CUSTOMER_NO, GOOGLE_PAY);
	}


	@Test
	void shouldStoreReferenceWhenApplePayHasTokenAndOptedIn() throws Exception
	{
		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute(APPLE_PAY + "StorePaymentData")).thenReturn(Boolean.TRUE);

		service.handleStorePayment(APPLE_PAY, responseWithToken(), customerJsonWithNumber());

		verify(novalnetPaymentService).handleReferenceTransactionInfo(any(), eq(CUSTOMER_NO), eq(APPLE_PAY));
	}


	@Test
	void shouldNotStoreReferenceWhenWalletPaymentHasNoToken() throws Exception
	{
		service.handleStorePayment(GOOGLE_PAY, responseWithoutToken(), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());
	}


	@Test
	void shouldNotStoreReferenceWhenWalletPaymentDataMissing() throws Exception
	{
		service.handleStorePayment(GOOGLE_PAY, responseWithoutPaymentData(), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());
	}


	@Test
	void shouldNotStoreReferenceWhenWalletTransactionMissing() throws Exception
	{
		service.handleStorePayment(GOOGLE_PAY, responseWithoutTransaction(), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());
	}


	@Test
	void shouldNotStoreReferenceWhenWalletGuestUser() throws Exception
	{
		when(novalnetCheckoutService.isGuestUser()).thenReturn(true);

		service.handleStorePayment(GOOGLE_PAY, responseWithToken(), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());
	}


	@Test
	void shouldDoNothingForUnrelatedPaymentMethod() throws Exception
	{
		service.handleStorePayment(DIRECT_PAYMENT, new StringBuilder("{}"), customerJsonWithNumber());

		verify(novalnetPaymentService, never()).handleReferenceTransactionInfo(any(), anyString(), anyString());

		verify(novalnetCheckoutService, never()).isGuestUser();
	}
}


