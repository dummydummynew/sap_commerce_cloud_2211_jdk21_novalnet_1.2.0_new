/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package novalnet.novalnetcheckoutaddon.controllers.pages.checkout.steps;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorfacades.flow.CheckoutFlowFacade;
import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.address.AddressVerificationFacade;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commerceservices.enums.CountryType;
import de.hybris.platform.servicelayer.session.SessionService;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.novalnet.dto.NovalnetPaymentDetailsForm;
import com.novalnet.facades.NovalnetPaymentFacade;
import com.novalnet.service.checkout.NovalnetCheckoutService;

import novalnet.novalnetcheckoutaddon.controllers.NovalnetcheckoutaddonControllerConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit tests for {@link NovalnetPaymentMethodCheckoutStepController}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NovalnetPaymentMethodCheckoutStepControllerTest
{
	private static final String PARAM_BILLING_COUNTRIES = "billingCountries";
	private static final String SESSION_ATTR_CHECKOUT_ERROR = "novalnetCheckoutError";
	private static final String SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID = "selectedPaymentMethodId";
	private static final String CHECKOUT_ERROR_MESSAGE = "Some checkout error occurred";
	private static final String NO_BILLING_ADDRESS_ERROR_KEY = "checkout.multi.paymentMethod.createSubscription.billingAddress.noneSelectedMsg";

	private static final String VIEW_CURRENT_STEP = "currentStep";
	private static final String VIEW_NEXT_STEP = "nextStep";
	private static final String VIEW_PREVIOUS_STEP = "previousStep";

	private static final String GUEST_EMAIL = "guest@example.com";
	private static final String PAYMENT_METHOD_ID = "CREDITCARD";
	private static final String EMPTY_PAYMENT_METHOD_ID = "";
	private static final String PAYMENT_INFO_ID = "paymentInfo_0001";

	@Spy
	@InjectMocks
	private NovalnetPaymentMethodCheckoutStepController testObj;

	@Mock
	private AcceleratorCheckoutFacade checkoutFacadeMock;

	@Mock
	private CheckoutFlowFacade checkoutFlowFacadeMock;

	@Mock
	private UserFacade userFacadeMock;

	@Mock
	private AddressVerificationFacade addressVerificationFacadeMock;

	@Mock
	private SessionService sessionServiceMock;

	@Mock
	private NovalnetCheckoutService novalnetCheckoutServiceMock;

	@Mock
	private NovalnetPaymentFacade novalnetPaymentFacadeMock;

	@Mock
	private Model modelMock;

	@Mock
	private RedirectAttributes redirectAttributesMock;

	@Mock
	private BindingResult bindingResultMock;

	@Mock
	private NovalnetPaymentDetailsForm paymentDetailsFormMock;

	@Mock
	private CartData cartDataMock;

	@Mock
	private AddressData deliveryAddressMock;

	@Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
	private CheckoutStep checkoutStepMock;

	private MockedStatic<GlobalMessages> globalMessagesMockedStatic;

	@BeforeEach
	void setUp()
	{
		globalMessagesMockedStatic = Mockito.mockStatic(GlobalMessages.class);
		when(novalnetCheckoutServiceMock.getGuestEmail()).thenReturn(GUEST_EMAIL);
	}

	@AfterEach
	void tearDown()
	{
		globalMessagesMockedStatic.close();
	}

	private void stubCheckoutStepNavigation(final String currentStep, final String nextStep, final String previousStep)
	{
		doReturn(checkoutStepMock).when(testObj).getCheckoutStep();
		when(checkoutStepMock.currentStep()).thenReturn(currentStep);
		when(checkoutStepMock.nextStep()).thenReturn(nextStep);
		when(checkoutStepMock.previousStep()).thenReturn(previousStep);
	}

	@Test
	void shouldReturnBillingCountriesFromCheckoutFacade()
	{
		List<CountryData> billingCountries = Collections.singletonList(new CountryData());
		when(checkoutFacadeMock.getCountries(CountryType.BILLING)).thenReturn(billingCountries);
		List<CountryData> result = (List<CountryData>) testObj.getBillingCountries();
		assertEquals(billingCountries, result);
	}

	@Test
	void shouldReturnAddPaymentMethodPageAndClearSessionErrorWhenNoPriorCheckoutError() throws CMSItemNotFoundException
	{
		when(sessionServiceMock.getAttribute(SESSION_ATTR_CHECKOUT_ERROR)).thenReturn(null);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(cartDataMock.getDeliveryAddress()).thenReturn(deliveryAddressMock);
		when(checkoutFlowFacadeMock.hasNoPaymentInfo()).thenReturn(true);

		doNothing().when(testObj).setupAddPaymentPage(modelMock);

		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);

		String result = testObj.enterStep(modelMock, redirectAttributesMock);

		assertEquals(NovalnetcheckoutaddonControllerConstants.AddPaymentMethodPage, result);

		verify(checkoutFacadeMock).setDeliveryModeIfAvailable();
		verify(modelMock).addAttribute(eq(PARAM_BILLING_COUNTRIES), any());
		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, null);
		verify(novalnetPaymentFacadeMock).addPaymentProcess(eq(modelMock), any(NovalnetPaymentDetailsForm.class), eq(cartDataMock));

		globalMessagesMockedStatic.verifyNoInteractions();
	}

	@Test
	void shouldAddErrorMessageWhenSessionHasPendingCheckoutError() throws CMSItemNotFoundException
	{
		when(sessionServiceMock.getAttribute(SESSION_ATTR_CHECKOUT_ERROR)).thenReturn(CHECKOUT_ERROR_MESSAGE);

		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(cartDataMock.getDeliveryAddress()).thenReturn(deliveryAddressMock);

		doNothing().when(testObj).setupAddPaymentPage(modelMock);

		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);

		String result = testObj.enterStep(modelMock, redirectAttributesMock);

		assertEquals(NovalnetcheckoutaddonControllerConstants.AddPaymentMethodPage, result);

		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, CHECKOUT_ERROR_MESSAGE));

		verify(sessionServiceMock).setAttribute(SESSION_ATTR_CHECKOUT_ERROR, null);
	}

	@Test
	void shouldReturnCurrentStepWhenUsingDeliveryAddressAndNoneIsAvailable() throws CMSItemNotFoundException
	{
		doNothing().when(testObj).setupAddPaymentPage(modelMock);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(paymentDetailsFormMock.isUseDeliveryAddress()).thenReturn(Boolean.TRUE);
		when(cartDataMock.getDeliveryAddress()).thenReturn(null);
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);

		String result = testObj.add(modelMock, paymentDetailsFormMock, bindingResultMock);

		assertEquals(VIEW_CURRENT_STEP, result);
		globalMessagesMockedStatic.verify(() -> GlobalMessages.addErrorMessage(modelMock, NO_BILLING_ADDRESS_ERROR_KEY));
		verify(novalnetPaymentFacadeMock, never()).processOneClickTokenData(any(), any(), any(), any(), any());
	}

	@Test
	void shouldReturnNextStepWhenOneClickTokenProcessingSucceeds() throws CMSItemNotFoundException
	{
		doNothing().when(testObj).setupAddPaymentPage(modelMock);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(paymentDetailsFormMock.getSelectedPaymentMethodId()).thenReturn(PAYMENT_METHOD_ID);
		when(paymentDetailsFormMock.isUseDeliveryAddress()).thenReturn(Boolean.FALSE);
		when(novalnetPaymentFacadeMock.processOneClickTokenData(any(), eq(paymentDetailsFormMock), eq(modelMock), eq(cartDataMock),
				any())).thenReturn(true);
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);

		String result = testObj.add(modelMock, paymentDetailsFormMock, bindingResultMock);

		assertEquals(VIEW_NEXT_STEP, result);
		verify(addressVerificationFacadeMock).verifyAddressData(any(AddressData.class));
		verify(sessionServiceMock).setAttribute(eq(SESSION_ATTR_SELECTED_PAYMENT_METHOD_ID), eq(PAYMENT_METHOD_ID));
		verify(novalnetPaymentFacadeMock).populateCustomerAddressDetails(eq(modelMock), eq(paymentDetailsFormMock),
				eq(cartDataMock), any(), any());
	}

	@Test
	void shouldReturnCurrentStepWhenOneClickTokenProcessingFails() throws CMSItemNotFoundException
	{
		doNothing().when(testObj).setupAddPaymentPage(modelMock);
		when(checkoutFacadeMock.getCheckoutCart()).thenReturn(cartDataMock);
		when(paymentDetailsFormMock.getSelectedPaymentMethodId()).thenReturn(PAYMENT_METHOD_ID);
		when(paymentDetailsFormMock.isUseDeliveryAddress()).thenReturn(Boolean.FALSE);
		when(novalnetPaymentFacadeMock.processOneClickTokenData(any(), eq(paymentDetailsFormMock), eq(modelMock), eq(cartDataMock),
				any())).thenReturn(false);
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);

		String result = testObj.add(modelMock, paymentDetailsFormMock, bindingResultMock);

		assertEquals(VIEW_CURRENT_STEP, result);
	}

	@Test
	void shouldUnlinkPaymentInfoAndReturnCurrentStep() throws CMSItemNotFoundException
	{
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);

		String result = testObj.remove(PAYMENT_INFO_ID, redirectAttributesMock);

		assertEquals(VIEW_CURRENT_STEP, result);
		verify(userFacadeMock).unlinkCCPaymentInfo(PAYMENT_INFO_ID);
		globalMessagesMockedStatic
				.verify(() -> GlobalMessages.addFlashMessage(eq(redirectAttributesMock), anyString(), anyString()));
	}

	@Test
	void shouldSetPaymentDetailsAndReturnNextStepWhenSelectedPaymentMethodIdIsProvided()
	{
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);
		String result = testObj.doSelectPaymentMethod(PAYMENT_METHOD_ID);
		assertEquals(VIEW_NEXT_STEP, result);
		verify(checkoutFacadeMock).setPaymentDetails(PAYMENT_METHOD_ID);
	}

	@Test
	void shouldNotSetPaymentDetailsButStillReturnNextStepWhenSelectedPaymentMethodIdIsBlank()
	{
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);
		String result = testObj.doSelectPaymentMethod(EMPTY_PAYMENT_METHOD_ID);
		assertEquals(VIEW_NEXT_STEP, result);
		verify(checkoutFacadeMock, never()).setPaymentDetails(anyString());
	}

	@Test
	void shouldReturnPreviousStepOnBack()
	{
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);
		String result = testObj.back(redirectAttributesMock);
		assertEquals(VIEW_PREVIOUS_STEP, result);
	}

	@Test
	void shouldReturnNextStepOnNext()
	{
		stubCheckoutStepNavigation(VIEW_CURRENT_STEP, VIEW_NEXT_STEP, VIEW_PREVIOUS_STEP);
		String result = testObj.next(redirectAttributesMock);
		assertEquals(VIEW_NEXT_STEP, result);
	}
}
