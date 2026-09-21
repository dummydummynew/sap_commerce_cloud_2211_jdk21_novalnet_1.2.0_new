/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package novalnet.novalnetcheckoutaddon.controllers.pages.checkout.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorfacades.flow.CheckoutFlowFacade;
import de.hybris.platform.acceleratorfacades.flow.impl.SessionOverrideCheckoutFlowFacade;
import de.hybris.platform.acceleratorservices.config.SiteConfigService;
import de.hybris.platform.acceleratorservices.storefront.util.PageTitleResolver;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.ThirdPartyConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.GuestRegisterForm;
import de.hybris.platform.acceleratorstorefrontcommons.forms.validation.GuestRegisterValidator;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.cms2.data.PagePreviewCriteriaData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.cms2.servicelayer.services.CMSPageService;
import de.hybris.platform.cms2.servicelayer.services.CMSPreviewService;
import de.hybris.platform.commercefacades.consent.ConsentFacade;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.site.BaseSiteService;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import novalnet.novalnetcheckoutaddon.controllers.NovalnetcheckoutaddonControllerConstants;
import novalnet.novalnetcheckoutaddon.services.NovalnetCheckoutAddonService;


@UnitTest
@ExtendWith(MockitoExtension.class)
class NovalnetCheckoutControllerTest
{
	private static final String ORDER_CODE = "00012345";
	private static final String ERROR_MESSAGE = "model not found";
	private static final String FORWARD_404 = "forward:/404";
	private static final String REDIRECT_CART = "redirect:/cart";
	private static final String REDIRECT_ROOT = "redirect:/";
	private static final String CONFIRMATION_CMS_PAGE = "orderConfirmation";
	private static final String FORM_GLOBAL_ERROR = "form.global.error";
	private static final String PROCESSED_VIEW = "processedView";
	private static final String GUEST_VIEW = "guestRegisteredView";
	private static final String MESSAGE_ATTRIBUTE = "message";
	private static final String META_ROBOTS = "metaRobots";

	@Spy
	@InjectMocks
	private NovalnetCheckoutController controller;

	@Mock
	private NovalnetCheckoutAddonService novalnetCheckoutAddonService;
	@Mock
	private CheckoutFacade checkoutFacade;
	@Mock
	private CheckoutFlowFacade checkoutFlowFacade;
	@Mock
	private CartFacade cartFacade;
	@Mock
	private GuestRegisterValidator guestRegisterValidator;
	@Mock
	private ConsentFacade consentFacade;
	@Mock
	private UserFacade userFacade;
	@Mock
	private BaseSiteService baseSiteService;
	@Mock
	private BaseSiteModel baseSite;
	@Mock
	private CMSPageService cmsPageService;
	@Mock
	private CMSPreviewService cmsPreviewService;
	@Mock
	private PageTitleResolver pageTitleResolver;
	@Mock
	private PagePreviewCriteriaData pagePreviewCriteria;
	@Mock
	private ContentPageModel contentPage;
	@Mock
	private SiteConfigService siteConfigService;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private Model model;
	@Mock
	private RedirectAttributes redirectAttributes;
	@Mock
	private BindingResult bindingResult;
	@Mock
	private OrderData orderData;
	@Mock
	private ModelNotFoundException modelNotFoundException;
	@Mock
	private CartModificationData cartModification;

	private GuestRegisterForm form;

	@BeforeEach
	void setUp() throws CMSItemNotFoundException
	{
		form = new GuestRegisterForm();
		form.setOrderCode(ORDER_CODE);

		ReflectionTestUtils.setField(controller, "siteConfigService", siteConfigService);

		lenient().when(cmsPreviewService.getPagePreviewCriteria()).thenReturn(pagePreviewCriteria);
		lenient().when(cmsPageService.getPageForLabelOrId(anyString(), eq(pagePreviewCriteria))).thenReturn(contentPage);
		lenient().when(pageTitleResolver.resolveContentPageTitle(any())).thenReturn("pageTitle");
		lenient().when(modelNotFoundException.getMessage()).thenReturn(ERROR_MESSAGE);
		lenient().when(baseSiteService.getCurrentBaseSite()).thenReturn(baseSite);
		lenient().when(userFacade.isAnonymousUser()).thenReturn(false);
		lenient().when(siteConfigService.getProperty(anyString())).thenReturn(null);
	}

	@Test
	void shouldForwardTo404AndSetMessageWhenModelNotFound()
	{
		final String result = controller.handleModelNotFoundException(modelNotFoundException, request);

		verify(request).setAttribute(MESSAGE_ATTRIBUTE, ERROR_MESSAGE);
		assertEquals(FORWARD_404, result);
	}

	@Test
	void shouldRedirectToCartWhenCartIsNotValid()
	{
		when(checkoutFlowFacade.hasValidCart()).thenReturn(false);

		final String result = controller.checkout(redirectAttributes);

		assertEquals(REDIRECT_CART, result);
		verify(checkoutFacade, never()).prepareCartForCheckout();
	}

	@Test
	void shouldRedirectToCartWhenCartHasModifications() throws CommerceCartModificationException
	{
		when(checkoutFlowFacade.hasValidCart()).thenReturn(true);
		when(cartFacade.validateCartData()).thenReturn(Collections.singletonList(cartModification));

		final String result = controller.checkout(redirectAttributes);

		assertEquals(REDIRECT_CART, result);
		verify(checkoutFacade, never()).prepareCartForCheckout();
	}

	@Test
	void shouldPrepareCartForCheckoutWhenCartIsValid() throws CommerceCartModificationException
	{
		when(checkoutFlowFacade.hasValidCart()).thenReturn(true);
		when(cartFacade.validateCartData()).thenReturn(Collections.emptyList());

		controller.checkout(redirectAttributes);

		verify(checkoutFacade).prepareCartForCheckout();
	}

	@Test
	void shouldResetSessionOverridesAndProcessOrderCodeOnOrderConfirmation() throws CMSItemNotFoundException
	{
		doReturn(PROCESSED_VIEW).when(controller).processOrderCode(ORDER_CODE, model, request, redirectAttributes);

		try (MockedStatic<SessionOverrideCheckoutFlowFacade> sessionOverride = mockStatic(SessionOverrideCheckoutFlowFacade.class))
		{
			final String result = controller.orderConfirmation(ORDER_CODE, request, model, redirectAttributes);

			sessionOverride.verify(SessionOverrideCheckoutFlowFacade::resetSessionOverrides);
			assertEquals(PROCESSED_VIEW, result);
		}
	}

	@Test
	void shouldValidateFormAndRegisterGuestOnOrderConfirmationPost() throws CMSItemNotFoundException
	{
		doReturn(GUEST_VIEW).when(controller).processRegisterGuestUserRequest(form, bindingResult, model, request, response,
				redirectAttributes);

		final String result = controller.orderConfirmation(form, bindingResult, model, request, response, redirectAttributes);

		verify(guestRegisterValidator).validate(form, bindingResult);
		assertEquals(GUEST_VIEW, result);
	}

	@Test
	void shouldRegisterGuestUserWhenFormHasNoErrors() throws CMSItemNotFoundException
	{
		when(bindingResult.hasErrors()).thenReturn(false);
		when(novalnetCheckoutAddonService.registerGuestUser(form, model, request, response, redirectAttributes))
				.thenReturn(GUEST_VIEW);

		final String result = controller.processRegisterGuestUserRequest(form, bindingResult, model, request, response,
				redirectAttributes);

		assertEquals(GUEST_VIEW, result);
	}

	@Test
	void shouldReloadOrderConfirmationWhenGuestFormHasErrors() throws CMSItemNotFoundException
	{
		form.setTermsCheck(true);
		when(bindingResult.hasErrors()).thenReturn(true);
		doReturn(PROCESSED_VIEW).when(controller).processOrderCode(ORDER_CODE, model, request, redirectAttributes);

		try (MockedStatic<GlobalMessages> globalMessages = mockStatic(GlobalMessages.class))
		{
			final String result = controller.processRegisterGuestUserRequest(form, bindingResult, model, request, response,
					redirectAttributes);

			globalMessages.verify(() -> GlobalMessages.addErrorMessage(model, FORM_GLOBAL_ERROR));
			assertEquals(PROCESSED_VIEW, result);
			assertFalse(form.isTermsCheck());
			verify(novalnetCheckoutAddonService, never()).registerGuestUser(any(), any(), any(), any(), any());
		}
	}

	@Test
	void shouldRedirectToRootWhenOrderIsNotFound() throws CMSItemNotFoundException
	{
		when(novalnetCheckoutAddonService.getOrderConfirmationDetails(ORDER_CODE, model, request))
				.thenThrow(new UnknownIdentifierException("not found"));

		final String result = controller.processOrderCode(ORDER_CODE, model, request, redirectAttributes);

		assertEquals(REDIRECT_ROOT, result);
		verify(cmsPageService, never()).getPageForLabelOrId(anyString(), any(PagePreviewCriteriaData.class));
	}

	@Test
	void shouldNotLoadConfirmationPageWhenOrderDetailsAreNull() throws CMSItemNotFoundException
	{
		when(novalnetCheckoutAddonService.getOrderConfirmationDetails(ORDER_CODE, model, request)).thenReturn(null);

		final String result = controller.processOrderCode(ORDER_CODE, model, request, redirectAttributes);

		assertNotEquals(NovalnetcheckoutaddonControllerConstants.Views.Pages.Checkout.CheckoutPage, result);
		verify(cmsPageService, never()).getPageForLabelOrId(anyString(), any(PagePreviewCriteriaData.class));
	}

	@Test
	void shouldReturnCheckoutPageWhenOrderDetailsExist() throws CMSItemNotFoundException
	{
		when(novalnetCheckoutAddonService.getOrderConfirmationDetails(ORDER_CODE, model, request)).thenReturn(orderData);

		final String result = controller.processOrderCode(ORDER_CODE, model, request, redirectAttributes);

		assertEquals(NovalnetcheckoutaddonControllerConstants.Views.Pages.Checkout.CheckoutPage, result);
		verify(cmsPageService).getPageForLabelOrId(CONFIRMATION_CMS_PAGE, pagePreviewCriteria);
		verify(model).addAttribute(META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
	}
}

