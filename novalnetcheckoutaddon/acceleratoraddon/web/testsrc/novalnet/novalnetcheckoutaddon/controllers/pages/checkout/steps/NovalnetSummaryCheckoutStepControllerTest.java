/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package novalnet.novalnetcheckoutaddon.controllers.pages.checkout.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorfacades.flow.CheckoutFlowFacade;
import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;
import de.hybris.platform.acceleratorservices.enums.CheckoutPciOptionEnum;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.PlaceOrderForm;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.novalnet.facades.NovalnetPaymentFacade;


/**
 * Unit tests for {@link NovalnetSummaryCheckoutStepController}.
 *
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NovalnetSummaryCheckoutStepControllerTest
{
	private static final String VIEW_PREVIOUS_STEP = "previousStep";
	private static final String VIEW_NEXT_STEP = "nextStep";
	private static final String VIEW_CURRENT_STEP = "currentStep";
	private static final String VIEW_ENTER_STEP = "enterStepView";

	private static final String CONFIRMATION_URL_PREFIX = "redirect:/checkout/novalnet/orderConfirmation/";

	private static final String ORDER_GUID = "order_guid_001";
	private static final String ORDER_CODE = "order_code_001";

	private static final String SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID = "selectedPaymentMethodId";

	private static final String SESSION_ATTR_CHECKOUT_ERROR = "novalnetCheckoutError";

	private static final String SESSION_ATTR_ORDER_AMOUNT = "novalnetOrderAmount";

	private static final String SESSION_ATTR_TXN_SECRET = "txn_secret";

	private static final String SESSION_ATTR_TXN_CHECK = "txn_check";

	private static final String NO_PAYMENT_METHOD_MESSAGE_KEY = "checkout.multi.paymentDetails.notprovided";

	private static final String NON_WALLET_PAYMENT_METHOD = "creditcard";

	private static final String DECLINED_STATUS_CODE = "300";
	private static final String SUCCESS_STATUS_CODE = "100";
	private static final String DECLINED_STATUS_TEXT = "Declined by acquirer";

	private static final String REDIRECT_URL = "https://pay.example.com/xyz";

	private static final String TXN_SECRET_VALUE = "secret123";

	private static final String PAYMENT_ACCESS_KEY = "accessKey123 ";

	private static final String CUSTOMER_NUMBER = "customer_0001";

	private static final Integer ORDER_AMOUNT_CENT = 1999;

	@Spy
	@InjectMocks
	private NovalnetSummaryCheckoutStepController testObj;

	@Mock
	private AcceleratorCheckoutFacade checkoutFacadeMock;

	@Mock
	private CheckoutFlowFacade checkoutFlowFacadeMock;

	@Mock
	private CheckoutCustomerStrategy checkoutCustomerStrategyMock;

	@Mock
	private NovalnetPaymentFacade novalnetPaymentFacadeMock;

	/**
	 * Injected into both the inherited base-class field and this controller's own field.
	 */
	@Mock
	private SessionService sessionServiceMock;

	@Mock
	private BaseStoreService baseStoreServiceMock;

	@Mock
	private BaseStoreModel baseStoreModelMock;

	@Mock
	private Model modelMock;

	@Mock
	private RedirectAttributes redirectAttributesMock;

	@Mock
	private HttpServletRequest httpServletRequestMock;

	@Mock
	private PlaceOrderForm placeOrderFormMock;

	@Mock
	private CartData cartDataMock;

	/**
	 * Deep stubs allow currentStep(), nextStep() and previousStep() to be safely stubbed.
	 */
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private CheckoutStep checkoutStepMock;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private JaloSession jaloSessionMock;

	private MockedStatic<GlobalMessages> globalMessagesMockedStatic;
	private MockedStatic<JaloSession> jaloSessionMockedStatic;

	/**
	 * Initializes common mocks and stubs checkout-step navigation for every test.
	 */
	@BeforeEach
	void setUp()
	{
		globalMessagesMockedStatic = Mockito.mockStatic(GlobalMessages.class);

		jaloSessionMockedStatic = Mockito.mockStatic(JaloSession.class);

		jaloSessionMockedStatic.when(JaloSession::getCurrentSession).thenReturn(jaloSessionMock);

		when(jaloSessionMock.getUser().getPK().toString()).thenReturn(CUSTOMER_NUMBER);

		testObj.setBaseStoreService(baseStoreServiceMock);

		when(baseStoreServiceMock.getCurrentBaseStore()).thenReturn(baseStoreModelMock);

		stubCheckoutStepNavigation();
	}

	/**
	 * Closes static mocks after every test.
	 */
	@AfterEach
	void tearDown()
	{
		globalMessagesMockedStatic.close();
		jaloSessionMockedStatic.close();
	}

	/**
	 * Stubs {@code getCheckoutStep()} and its navigation outcomes.
	 */
	private void stubCheckoutStepNavigation()
	{
		doReturn(checkoutStepMock).when(testObj).getCheckoutStep();

		when(checkoutStepMock.currentStep()).thenReturn(VIEW_CURRENT_STEP);

		when(checkoutStepMock.nextStep()).thenReturn(VIEW_NEXT_STEP);

		when(checkoutStepMock.previousStep()).thenReturn(VIEW_PREVIOUS_STEP);
	}


	private StringBuilder buildGatewayResponse(final String statusCode, final String statusText, final String redirectUrl)
	{
		final JSONObject result = new JSONObject();

		result.put("status_code", statusCode);

		if (statusText != null)
		{
			result.put("status_text", statusText);
		}

		if (redirectUrl != null)
		{
			result.put("redirect_url", redirectUrl);
		}

		final JSONObject transaction = new JSONObject();

		transaction.put("status", "CONFIRMED");

		if (redirectUrl != null)
		{
			transaction.put("txn_secret", TXN_SECRET_VALUE);
		}

		final JSONObject root = new JSONObject();

		root.put("result", result);
		root.put("transaction", transaction);
		root.put("customer", new JSONObject());

		return new StringBuilder(root.toString());
	}

	@Test
	void shouldReturnPreviousStepAndSetErrorWhenNoPaymentMethodIsSelected()
			throws CMSItemNotFoundException, CommerceCartModificationException
	{
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(cartDataMock.getEntries()).thenReturn(null);
		when(sessionServiceMock.getAttribute(SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID)).thenReturn(null);

		String result = testObj.enterStep(modelMock, redirectAttributesMock);
		assertEquals(VIEW_PREVIOUS_STEP, result);
		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, NO_PAYMENT_METHOD_MESSAGE_KEY);
	}

	@Test
	void shouldReturnPreviousStepWhenSelectedPaymentMethodIdIsBlank()
			throws CMSItemNotFoundException, CommerceCartModificationException
	{
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);

		when(cartDataMock.getEntries()).thenReturn(null);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID)).thenReturn("");

		String result = testObj.enterStep(modelMock, redirectAttributesMock);

		assertEquals(VIEW_PREVIOUS_STEP, result);

		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, NO_PAYMENT_METHOD_MESSAGE_KEY);
	}


	@Test
	void shouldDelegateWalletTransactionBookingToPaymentFacade() throws Exception
	{
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_ORDER_AMOUNT)).thenReturn(ORDER_AMOUNT_CENT);

		when(novalnetPaymentFacadeMock.bookWalletTransaction(httpServletRequestMock, baseStoreModelMock, CUSTOMER_NUMBER,
				ORDER_AMOUNT_CENT, cartDataMock)).thenReturn("{\"status\":\"ok\"}");

		String result = testObj.bookWalletTransaction(modelMock, httpServletRequestMock, redirectAttributesMock);

		assertEquals("{\"status\":\"ok\"}", result);

		verify(novalnetPaymentFacadeMock).bookWalletTransaction(httpServletRequestMock, baseStoreModelMock, CUSTOMER_NUMBER,
				ORDER_AMOUNT_CENT, cartDataMock);
	}

	@Test
	void shouldReturnPreviousStepAndSetErrorWhenGatewayDeclinesTransaction()
			throws CMSItemNotFoundException, InvalidCartException, CommerceCartModificationException
	{
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID)).thenReturn(NON_WALLET_PAYMENT_METHOD);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_ORDER_AMOUNT)).thenReturn(ORDER_AMOUNT_CENT);

		when(novalnetPaymentFacadeMock.createTransaction(eq(httpServletRequestMock), eq(baseStoreModelMock),
				eq(NON_WALLET_PAYMENT_METHOD), eq(CUSTOMER_NUMBER), any(), eq(cartDataMock)))
						.thenReturn(buildGatewayResponse(DECLINED_STATUS_CODE, DECLINED_STATUS_TEXT, null));

		String result = testObj.placeOrder(placeOrderFormMock, modelMock, httpServletRequestMock, redirectAttributesMock);

		assertEquals(VIEW_PREVIOUS_STEP, result);

		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, DECLINED_STATUS_TEXT);
	}

	@Test
	void shouldRedirectToPaygateUrlWhenGatewayReturnsARedirectUrl()
			throws CMSItemNotFoundException, InvalidCartException, CommerceCartModificationException
	{
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID)).thenReturn(NON_WALLET_PAYMENT_METHOD);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_ORDER_AMOUNT)).thenReturn(ORDER_AMOUNT_CENT);

		when(novalnetPaymentFacadeMock.createTransaction(eq(httpServletRequestMock), eq(baseStoreModelMock),
				eq(NON_WALLET_PAYMENT_METHOD), eq(CUSTOMER_NUMBER), any(), eq(cartDataMock)))
						.thenReturn(buildGatewayResponse(SUCCESS_STATUS_CODE, null, REDIRECT_URL));

		doNothing().when(testObj).setupPageModel(modelMock);

		when(baseStoreModelMock.getNovalnetPaymentAccessKey()).thenReturn(PAYMENT_ACCESS_KEY);

		String result = testObj.placeOrder(placeOrderFormMock, modelMock, httpServletRequestMock, redirectAttributesMock);

		assertEquals("redirect:" + REDIRECT_URL, result);

		verify(modelMock).addAttribute("paygateUrl", REDIRECT_URL);

		verify(sessionServiceMock).setAttribute(SESSION_ATTR_TXN_SECRET, TXN_SECRET_VALUE);

		verify(sessionServiceMock).setAttribute(SESSION_ATTR_TXN_CHECK, PAYMENT_ACCESS_KEY.trim());
	}

	@Test
	void shouldDelegateToEnterStepWhenPlaceOrderFormIsInvalid()
			throws CMSItemNotFoundException, InvalidCartException, CommerceCartModificationException
	{
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID)).thenReturn(NON_WALLET_PAYMENT_METHOD);

		when(sessionServiceMock.getAttribute(SESSION_ATTR_ORDER_AMOUNT)).thenReturn(ORDER_AMOUNT_CENT);

		when(novalnetPaymentFacadeMock.createTransaction(eq(httpServletRequestMock), eq(baseStoreModelMock),
				eq(NON_WALLET_PAYMENT_METHOD), eq(CUSTOMER_NUMBER), any(), eq(cartDataMock)))
						.thenReturn(buildGatewayResponse(SUCCESS_STATUS_CODE, null, null));

		doReturn(true).when(testObj).validateOrderForm(placeOrderFormMock, modelMock);

		doReturn(VIEW_ENTER_STEP).when(testObj).enterStep(modelMock, redirectAttributesMock);

		String result = testObj.placeOrder(placeOrderFormMock, modelMock, httpServletRequestMock, redirectAttributesMock);

		assertEquals(VIEW_ENTER_STEP, result);

		verify(testObj).enterStep(modelMock, redirectAttributesMock);
	}


	@Test
	void shouldFlagInvalidWhenDeliveryAddressIsMissing()
	{
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(true);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);
		when(placeOrderFormMock.isTermsCheck()).thenReturn(true);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(checkoutFacadeMock.containsTaxValues()).thenReturn(true);
		when(cartDataMock.isCalculated()).thenReturn(true);

		final boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);

		assertTrue(result);

		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, "checkout.deliveryAddress.notSelected"));
	}

	@Test
	void shouldFlagInvalidWhenDeliveryModeIsMissing()
	{
		// Given
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(true);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);
		when(placeOrderFormMock.isTermsCheck()).thenReturn(true);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(checkoutFacadeMock.containsTaxValues()).thenReturn(true);
		when(cartDataMock.isCalculated()).thenReturn(true);
		
		boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);
		
		assertTrue(result);

		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, "checkout.deliveryMethod.notSelected"));
	}

	@Test
	void shouldFlagInvalidWhenSecurityCodeMissingUnderDefaultPciOption()
	{
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(false);
		when(checkoutFlowFacadeMock.getSubscriptionPciOption()).thenReturn(CheckoutPciOptionEnum.DEFAULT);
		when(placeOrderFormMock.getSecurityCode()).thenReturn(" ");
		when(placeOrderFormMock.isTermsCheck()).thenReturn(true);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(checkoutFacadeMock.containsTaxValues()).thenReturn(true);
		when(cartDataMock.isCalculated()).thenReturn(true);

		boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);

		assertTrue(result);

		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, "checkout.paymentMethod.noSecurityCode"));
	}

	@Test
	void shouldFlagInvalidAndShortCircuitWhenTermsAreNotAccepted()
	{
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);
		when(placeOrderFormMock.isTermsCheck()).thenReturn(false);

		boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);

		assertTrue(result);

		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, "checkout.error.terms.not.accepted"));

		verify(checkoutFacadeMock, never()).containsTaxValues();
	}

	@Test
	void shouldFlagInvalidWhenCartHasNoTaxValues()
	{
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);
		when(placeOrderFormMock.isTermsCheck()).thenReturn(true);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(checkoutFacadeMock.containsTaxValues()).thenReturn(false);
		when(cartDataMock.getCode()).thenReturn(ORDER_CODE);
		when(cartDataMock.isCalculated()).thenReturn(true);

		boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);
		assertTrue(result);
		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, "checkout.error.tax.missing"));
	}

	@Test
	void shouldFlagInvalidWhenCartIsNotCalculated()
	{
		// Given
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);
		when(placeOrderFormMock.isTermsCheck()).thenReturn(true);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(checkoutFacadeMock.containsTaxValues()).thenReturn(true);
		when(cartDataMock.isCalculated()).thenReturn(false);
		when(cartDataMock.getCode()).thenReturn(ORDER_CODE);

		boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);

		assertTrue(result);

		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, "checkout.error.cart.notcalculated"));
	}

	@Test
	void shouldReturnValidWhenAllOrderFormConditionsAreSatisfied()
	{
		when(checkoutFlowFacadeMock.hasNoDeliveryAddress()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoDeliveryMode()).thenReturn(false);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);
		when(placeOrderFormMock.isTermsCheck()).thenReturn(true);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(checkoutFacadeMock.containsTaxValues()).thenReturn(true);
		when(cartDataMock.isCalculated()).thenReturn(true);

		boolean result = testObj.validateOrderForm(placeOrderFormMock, modelMock);
		assertFalse(result);

		globalMessagesMockedStatic.verifyNoInteractions();
	}

	@Test
	void shouldReturnGuidBasedConfirmationUrlForAnonymousCheckout()
	{
		
		de.hybris.platform.commercefacades.order.data.OrderData orderDataMock = Mockito
				.mock(de.hybris.platform.commercefacades.order.data.OrderData.class);

		when(checkoutCustomerStrategyMock.isAnonymousCheckout()).thenReturn(true);
		when(orderDataMock.getGuid()).thenReturn(ORDER_GUID);

		String result = testObj.confirmationPageURL(orderDataMock);
		assertEquals(CONFIRMATION_URL_PREFIX + ORDER_GUID, result);
	}

	@Test
	void shouldReturnCodeBasedConfirmationUrlForRegisteredCheckout()
	{
		de.hybris.platform.commercefacades.order.data.OrderData orderDataMock = Mockito
				.mock(de.hybris.platform.commercefacades.order.data.OrderData.class);
		when(checkoutCustomerStrategyMock.isAnonymousCheckout()).thenReturn(false);
		when(orderDataMock.getCode()).thenReturn(ORDER_CODE);
		String result = testObj.confirmationPageURL(orderDataMock);
		assertEquals(CONFIRMATION_URL_PREFIX + ORDER_CODE, result);
	}

	@Test
	void shouldReturnPreviousStepOnBack()
	{
		String result = testObj.back(redirectAttributesMock);
		assertEquals(VIEW_PREVIOUS_STEP, result);
	}

	@Test
	void shouldReturnNextStepOnNext()
	{
		String result = testObj.next(redirectAttributesMock);
		assertEquals(VIEW_NEXT_STEP, result);
	}


	@Test
	void shouldReturnCurrentBaseStoreFromBaseStoreService()
	{
		BaseStoreModel result = testObj.getBaseStoreModel();
		assertEquals(baseStoreModelMock, result);
	}
}
