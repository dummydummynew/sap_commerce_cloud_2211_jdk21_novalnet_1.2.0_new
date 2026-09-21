/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package novalnet.novalnetcheckoutaddon.controllers.pages.checkout.steps;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.session.SessionService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.facades.NovalnetPaymentFacade;
import com.novalnet.util.NovalnetUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@UnitTest
@ExtendWith(MockitoExtension.class)
class NovalnetHopPaymentResponseControllerTest
{
	private static final String VIEW_CURRENT_STEP = "currentStep";
	private static final String VIEW_CONFIRMATION_PAGE = "confirmationPage";
	private static final String CONFIRMATION_URL_PREFIX = "redirect:/checkout/novalnet/orderConfirmation/";

	private static final String PARAM_CHECKSUM = "checksum";
	private static final String PARAM_TID = "tid";
	private static final String PARAM_STATUS = "status";
	private static final String EMPTY_VALUE = "";

	private static final String SESSION_ATTR_TXN_SECRET = "txn_secret";
	private static final String SESSION_ATTR_TXN_CHECK = "txn_check";
	private static final String SESSION_ATTR_ORDER_DATA = "novalnetOrderData";
	private static final String SESSION_ATTR_CHECKOUT_ERROR = "novalnetCheckoutError";
	private static final String HASH_CHECK_FAILED_MESSAGE = "While redirecting some data has been changed. The hash check failed";

	private static final String TID_VALUE = "tid_123456789";
	private static final String CHECKSUM_VALUE = "valid_checksum_value";
	private static final String STATUS_VALUE = "SUCCESS";
	private static final String TXN_SECRET_VALUE = "txn_secret_value";
	private static final String TXN_CHECK_VALUE = "txn_check_value";
	private static final String ORDER_GUID = "order_guid_001";
	private static final String ORDER_CODE = "order_code_001";

	@Spy
	@InjectMocks
	private NovalnetHopPaymentResponseController testObj;

	@Mock
	private AcceleratorCheckoutFacade checkoutFacadeMock;

	@Mock
	private NovalnetPaymentFacade novalnetPaymentFacadeMock;

	@Mock
	private OrderData orderDataMock;

	@Mock
	private HttpServletRequest httpServletRequestMock;

	@Mock
	private SessionService sessionServiceMock;

	@Mock
	private CheckoutCustomerStrategy checkoutCustomerStrategyMock;

	@Mock
	private CheckoutStep checkoutStepMock;

	private MockedStatic<NovalnetUtils> novalnetUtilsMockedStatic;

	@BeforeEach
	void setUp()
	{
		novalnetUtilsMockedStatic = Mockito.mockStatic(NovalnetUtils.class);
	}

	@AfterEach
	void tearDown()
	{
		novalnetUtilsMockedStatic.close();
	}


	private Map<String, String> buildValidResultMap()
	{
		final Map<String, String> resultMap = new HashMap<>();
		resultMap.put(PARAM_CHECKSUM, CHECKSUM_VALUE);
		resultMap.put(PARAM_TID, TID_VALUE);
		resultMap.put(PARAM_STATUS, STATUS_VALUE);
		return resultMap;
	}


	private void stubRequestParameters(final Map<String, String> params)
	{
		when(httpServletRequestMock.getParameterNames()).thenReturn(Collections.enumeration(params.keySet()));
		for (final Map.Entry<String, String> entry : params.entrySet())
		{
			when(httpServletRequestMock.getParameter(entry.getKey())).thenReturn(entry.getValue());
		}
	}

	private void stubCurrentCheckoutStep()
	{
		doReturn(checkoutStepMock).when(testObj).getCheckoutStep();
		when(checkoutStepMock.currentStep()).thenReturn(VIEW_CURRENT_STEP);
	}

	@Test
	void shouldReturnConfirmationPageWhenPlaceOrderSucceeds() throws InvalidCartException
	{
		when(checkoutFacadeMock.placeOrder()).thenReturn(orderDataMock);
		doReturn(VIEW_CONFIRMATION_PAGE).when(testObj).confirmationPageURL(orderDataMock);

		String result = testObj.doHandleHopResponse(httpServletRequestMock);

		assertEquals(VIEW_CONFIRMATION_PAGE, result);
		verify(checkoutFacadeMock).placeOrder();
		verify(testObj).confirmationPageURL(orderDataMock);
	}

	@Test
	void shouldReturnCurrentStepWhenPlaceOrderThrowsInvalidCartException() throws InvalidCartException
	{
		when(checkoutFacadeMock.placeOrder()).thenThrow(new InvalidCartException("Invalid cart"));
		stubCurrentCheckoutStep();

		String result = testObj.doHandleHopResponse(httpServletRequestMock);

		assertEquals(VIEW_CURRENT_STEP, result);
		verify(checkoutFacadeMock).placeOrder();
	}

	@Test
	void shouldReturnConfirmationPageWhenChecksumValidAndProcessTransactionSucceeds()
	{
		stubRequestParameters(buildValidResultMap());
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_SECRET)).thenReturn(TXN_SECRET_VALUE);
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_CHECK)).thenReturn(TXN_CHECK_VALUE);

		String tokenString = TID_VALUE + TXN_SECRET_VALUE + STATUS_VALUE + new StringBuilder(TXN_CHECK_VALUE).reverse().toString();
		novalnetUtilsMockedStatic.when(() -> NovalnetUtils.generateChecksum(tokenString)).thenReturn(CHECKSUM_VALUE);

		when(novalnetPaymentFacadeMock.processTransaction(any())).thenReturn(true);
		when(sessionServiceMock.getAttribute(SESSION_ATTR_ORDER_DATA)).thenReturn(orderDataMock);
		doReturn(VIEW_CONFIRMATION_PAGE).when(testObj).confirmationPageURL(orderDataMock);

		String result = testObj.handleHopResponse(null, httpServletRequestMock);

		// Then
		assertEquals(VIEW_CONFIRMATION_PAGE, result);
		verify(novalnetPaymentFacadeMock).processTransaction(any());
		verify(testObj).confirmationPageURL(orderDataMock);
	}

	@Test
	void shouldReturnCurrentStepAndSetErrorAttributeWhenChecksumMismatch()
	{
		stubRequestParameters(buildValidResultMap());
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_SECRET)).thenReturn(TXN_SECRET_VALUE);
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_CHECK)).thenReturn(TXN_CHECK_VALUE);
		novalnetUtilsMockedStatic.when(() -> NovalnetUtils.generateChecksum(anyString())).thenReturn("different_checksum");
		stubCurrentCheckoutStep();

		String result = testObj.handleHopResponse(null, httpServletRequestMock);

		assertEquals(VIEW_CURRENT_STEP, result);
		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, HASH_CHECK_FAILED_MESSAGE);
		verify(novalnetPaymentFacadeMock, Mockito.never()).processTransaction(any());
	}

	@Test
	void shouldReturnCurrentStepWhenProcessTransactionFails()
	{
		stubRequestParameters(buildValidResultMap());
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_SECRET)).thenReturn(TXN_SECRET_VALUE);
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_CHECK)).thenReturn(TXN_CHECK_VALUE);

		String tokenString = TID_VALUE + TXN_SECRET_VALUE + STATUS_VALUE + new StringBuilder(TXN_CHECK_VALUE).reverse().toString();
		novalnetUtilsMockedStatic.when(() -> NovalnetUtils.generateChecksum(tokenString)).thenReturn(CHECKSUM_VALUE);
		when(novalnetPaymentFacadeMock.processTransaction(any())).thenReturn(false);
		stubCurrentCheckoutStep();

		String result = testObj.handleHopResponse(null, httpServletRequestMock);

		assertEquals(VIEW_CURRENT_STEP, result);
		verify(novalnetPaymentFacadeMock).processTransaction(any());
		verify(testObj, Mockito.never()).confirmationPageURL(any());
	}

	@Test
	void shouldReturnCurrentStepAndSetErrorAttributeWhenRequiredParamsMissing()
	{
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put(PARAM_CHECKSUM, EMPTY_VALUE);
		resultMap.put(PARAM_TID, TID_VALUE);
		resultMap.put(PARAM_STATUS, STATUS_VALUE);
		stubRequestParameters(resultMap);
		when(sessionServiceMock.getAttribute(SESSION_ATTR_TXN_SECRET)).thenReturn(TXN_SECRET_VALUE);
		stubCurrentCheckoutStep();

		String result = testObj.handleHopResponse(null, httpServletRequestMock);

		assertEquals(VIEW_CURRENT_STEP, result);
		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, HASH_CHECK_FAILED_MESSAGE);
		verify(novalnetPaymentFacadeMock, Mockito.never()).processTransaction(any());
	}

	@Test
	void shouldReturnGuidBasedUrlWhenAnonymousCheckout()
	{
		when(checkoutCustomerStrategyMock.isAnonymousCheckout()).thenReturn(true);
		when(orderDataMock.getGuid()).thenReturn(ORDER_GUID);

		String result = testObj.confirmationPageURL(orderDataMock);

		assertEquals(CONFIRMATION_URL_PREFIX + ORDER_GUID, result);
	}

	@Test
	void shouldReturnCodeBasedUrlWhenNotAnonymousCheckout()
	{
		when(checkoutCustomerStrategyMock.isAnonymousCheckout()).thenReturn(false);
		when(orderDataMock.getCode()).thenReturn(ORDER_CODE);

		String result = testObj.confirmationPageURL(orderDataMock);

		assertEquals(CONFIRMATION_URL_PREFIX + ORDER_CODE, result);
	}
}
