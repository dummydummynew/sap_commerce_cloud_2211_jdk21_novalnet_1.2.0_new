/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package novalnet.novalnetcheckoutaddon.controllers.pages.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorservices.storefront.util.PageTitleResolver;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.Breadcrumb;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.ResourceBreadcrumbBuilder;
import de.hybris.platform.acceleratorstorefrontcommons.util.AddressDataUtil;
import de.hybris.platform.cms2.data.PagePreviewCriteriaData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.cms2.servicelayer.services.CMSPageService;
import de.hybris.platform.cms2.servicelayer.services.CMSPreviewService;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commercefacades.user.data.TitleData;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.I18NService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.novalnet.dao.NovalnetDao;
import com.novalnet.dto.NovalnetPaymentInfoData;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.util.NovalnetUtils;

import novalnet.novalnetcheckoutaddon.controllers.NovalnetcheckoutaddonControllerConstants;


@UnitTest
@ExtendWith(MockitoExtension.class)
public class NovalnetAccountPageControllerTest
{
	private static final String ORDER_CODE = "00012345";
	private static final String REDIRECT_MY_ACCOUNT = "redirect:/my-account";
	private static final String ISO_CODE = "DE";
	private static final String TITLE_CODE = "mr";
	private static final String PAGE_TITLE = "pageTitle";
	private static final String MESSAGE = "message";
	private static final String TRANSACTION_DETAILS = "{\"tid\":\"14000000000001\"}";
	private static final String HISTORY_NOTES = "Novalnet Transaction ID: 14000000000001";
	private static final String ORDERS_URL = "/my-account/orders";
	private static final String NOVALNET_PAYMENT_INFO = "novalnetPaymentInfo";
	private static final String ORDER_DATA = "orderData";
	private static final String CUSTOMER_DATA = "customerData";
	private static final String ORDER_INFO_MODEL_ADD = "orderInfoModeladd";
	private static final String BILLING_ADDRESS = "billingAddress";
	private static final String BREADCRUMBS = "breadcrumbs";
	private static final String META_ROBOTS = "metaRobots";
	private static final String NO_INDEX_NO_FOLLOW = "noindex,nofollow";
	private static final String TITLE = "title";
	private static final int EXPECTED_BREADCRUMB_COUNT = 2;

	@Spy
	@InjectMocks
	private NovalnetAccountPageController controller;

	@Mock
	private NovalnetOrderService novalnetOrderService;

	@Mock
	private NovalnetDao novalnetDao;

	@Mock
	private OrderFacade orderFacade;

	@Mock
	private CustomerFacade customerFacade;

	@Mock
	private UserFacade userFacade;

	@Mock
	private ResourceBreadcrumbBuilder accountBreadcrumbBuilder;

	@Mock
	private AddressDataUtil addressDataUtil;

	@Mock
	private I18NFacade i18NFacade;

	@Mock
	private CMSPageService cmsPageService;

	@Mock
	private CMSPreviewService cmsPreviewService;

	@Mock
	private PageTitleResolver pageTitleResolver;

	@Mock
	private MessageSource messageSource;

	@Mock
	private I18NService i18nService;

	@Mock
	private PagePreviewCriteriaData pagePreviewCriteria;

	@Mock
	private ContentPageModel contentPageModel;

	@Mock
	private Model model;

	@Mock
	private RedirectAttributes redirectAttributes;

	@Mock
	private OrderModel orderModel;

	@Mock
	private OrderData orderData;

	@Mock
	private PaymentInfoModel paymentInfoModel;

	@Mock
	private AddressModel billingAddress;

	@Mock
	private CountryModel countryModel;

	@Mock
	private CountryData countryData;

	@Mock
	private CustomerData customerData;

	@Mock
	private NovalnetPaymentInfoModel novalnetPaymentInfoModel;

	@Mock
	private TitleData titleData;

	@Captor
	private ArgumentCaptor<NovalnetPaymentInfoData> paymentInfoDataCaptor;

	@Captor
	private ArgumentCaptor<List<Breadcrumb>> breadcrumbsCaptor;


	@BeforeEach
	public void setUp() throws CMSItemNotFoundException
	{
		lenient().when(cmsPreviewService.getPagePreviewCriteria()).thenReturn(pagePreviewCriteria);

		lenient().when(cmsPageService.getPageForLabelOrId(anyString(), eq(pagePreviewCriteria))).thenReturn(contentPageModel);

		lenient().when(pageTitleResolver.resolveContentPageTitle(any())).thenReturn(PAGE_TITLE);

		lenient().when(i18nService.getCurrentLocale()).thenReturn(Locale.ENGLISH);

		lenient().when(messageSource.getMessage(anyString(), any(), eq(Locale.ENGLISH))).thenReturn(MESSAGE);

		lenient().when(messageSource.getMessage(anyString(), any(), anyString(), eq(Locale.ENGLISH))).thenReturn(MESSAGE);

		lenient().when(accountBreadcrumbBuilder.getBreadcrumbs(null)).thenReturn(new ArrayList<>());

		lenient().when(novalnetOrderService.getOrder(ORDER_CODE)).thenReturn(orderModel);

		lenient().when(orderFacade.getOrderDetailsForCode(ORDER_CODE)).thenReturn(orderData);

		lenient().when(orderData.getCode()).thenReturn(ORDER_CODE);

		lenient().when(novalnetDao.getNovalnetPaymentInfo(ORDER_CODE)).thenReturn(Collections.emptyList());

		lenient().when(orderModel.getPaymentInfo()).thenReturn(paymentInfoModel);

		lenient().when(paymentInfoModel.getBillingAddress()).thenReturn(billingAddress);

		lenient().when(billingAddress.getCountry()).thenReturn(countryModel);

		lenient().when(countryModel.getIsocode()).thenReturn(ISO_CODE);

		lenient().when(addressDataUtil.getI18NFacade()).thenReturn(i18NFacade);

		lenient().when(i18NFacade.getCountryForIsocode(ISO_CODE)).thenReturn(countryData);

		lenient().when(customerFacade.getCurrentCustomer()).thenReturn(customerData);
	}


	@Test
	public void shouldReturnNovalnetAccountPageViewWhenOrderExists() throws CMSItemNotFoundException
	{
		String result = controller.order(ORDER_CODE, model, redirectAttributes);

		assertEquals(NovalnetcheckoutaddonControllerConstants.Views.Pages.Account.AccountPage, result);

		verify(model).addAttribute(META_ROBOTS, NO_INDEX_NO_FOLLOW);
	}

	@Test
	public void shouldAddNovalnetPaymentInfoToModelWhenPaymentInfoExists() throws CMSItemNotFoundException
	{
		when(novalnetPaymentInfoModel.getPaymentInfo()).thenReturn(TRANSACTION_DETAILS);

		when(novalnetPaymentInfoModel.getOrderHistoryNotes()).thenReturn(HISTORY_NOTES);

		when(novalnetDao.getNovalnetPaymentInfo(ORDER_CODE)).thenReturn(Collections.singletonList(novalnetPaymentInfoModel));

		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(model).addAttribute(eq(NOVALNET_PAYMENT_INFO), paymentInfoDataCaptor.capture());

		assertThat(paymentInfoDataCaptor.getValue().getTransactionDetails()).isEqualTo(TRANSACTION_DETAILS);

		assertThat(paymentInfoDataCaptor.getValue().getOrderHistoryNotes()).isEqualTo(HISTORY_NOTES);
	}

	@Test
	public void shouldAddNullPaymentInfoToModelWhenPaymentInfoListIsEmpty() throws CMSItemNotFoundException
	{
		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(model).addAttribute(NOVALNET_PAYMENT_INFO, null);
	}

	@Test
	public void shouldAddNullPaymentInfoToModelWhenPaymentInfoListIsNull() throws CMSItemNotFoundException
	{
		when(novalnetDao.getNovalnetPaymentInfo(ORDER_CODE)).thenReturn(null);

		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(model).addAttribute(NOVALNET_PAYMENT_INFO, null);
	}


	@Test
	public void shouldAddOrderCustomerAndBillingDetailsToModel() throws CMSItemNotFoundException
	{
		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(model).addAttribute(ORDER_DATA, orderData);
		verify(model).addAttribute(CUSTOMER_DATA, customerData);
		verify(model).addAttribute(ORDER_INFO_MODEL_ADD, billingAddress);
		verify(model).addAttribute(BILLING_ADDRESS, countryData);
	}


	@Test
	public void shouldNotResolveCountryWhenBillingAddressHasNoCountry() throws CMSItemNotFoundException
	{
		when(billingAddress.getCountry()).thenReturn(null);

		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(i18NFacade, never()).getCountryForIsocode(anyString());
		verify(model).addAttribute(BILLING_ADDRESS, null);
	}


	@Test
	public void shouldNotResolveCountryWhenBillingAddressIsNull() throws CMSItemNotFoundException
	{
		when(paymentInfoModel.getBillingAddress()).thenReturn(null);

		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(i18NFacade, never()).getCountryForIsocode(anyString());
		verify(model).addAttribute(ORDER_INFO_MODEL_ADD, null);
		verify(model).addAttribute(BILLING_ADDRESS, null);
	}


	@Test
	public void shouldAddOrderHistoryAndOrderBreadcrumbs() throws CMSItemNotFoundException
	{
		controller.order(ORDER_CODE, model, redirectAttributes);

		verify(model).addAttribute(eq(BREADCRUMBS), breadcrumbsCaptor.capture());

		List<Breadcrumb> breadcrumbs = breadcrumbsCaptor.getValue();

		assertThat(breadcrumbs).hasSize(EXPECTED_BREADCRUMB_COUNT);
		assertThat(breadcrumbs.get(0).getUrl()).isEqualTo(ORDERS_URL);
		assertThat(breadcrumbs.get(1).getUrl()).isEqualTo("#");
	}


	@Test
	public void shouldRedirectToMyAccountWhenNovalnetOrderNotFound() throws CMSItemNotFoundException
	{
		when(novalnetOrderService.getOrder(ORDER_CODE)).thenThrow(new UnknownIdentifierException("not found"));

		String result = controller.order(ORDER_CODE, model, redirectAttributes);

		assertEquals(REDIRECT_MY_ACCOUNT, result);

		verify(novalnetDao, never()).getNovalnetPaymentInfo(anyString());
	}


	@Test
	public void shouldRedirectToMyAccountWhenOrderNotVisibleForCustomer() throws CMSItemNotFoundException
	{
		when(orderFacade.getOrderDetailsForCode(ORDER_CODE)).thenThrow(new UnknownIdentifierException("not visible"));

		String result = controller.order(ORDER_CODE, model, redirectAttributes);

		assertEquals(REDIRECT_MY_ACCOUNT, result);

		verify(model, never()).addAttribute(eq(ORDER_DATA), any());
	}


	@Test
	public void shouldAddTitleToModelWhenCustomerHasTitleCode() throws CMSItemNotFoundException
	{
		List<TitleData> titles = Collections.singletonList(titleData);

		when(userFacade.getTitles()).thenReturn(titles);

		when(customerData.getTitleCode()).thenReturn(TITLE_CODE);

		try (MockedStatic<NovalnetUtils> novalnetUtils = mockStatic(NovalnetUtils.class))
		{
			novalnetUtils.when(() -> NovalnetUtils.findTitleForCode(titles, TITLE_CODE)).thenReturn(titleData);

			controller.profile(model);

			verify(model).addAttribute(TITLE, titleData);
			verify(model).addAttribute(CUSTOMER_DATA, customerData);
		}
	}


	@Test
	public void shouldNotAddTitleToModelWhenCustomerHasNoTitleCode() throws CMSItemNotFoundException
	{
		when(customerData.getTitleCode()).thenReturn(null);

		try (MockedStatic<NovalnetUtils> novalnetUtils = mockStatic(NovalnetUtils.class))
		{
			controller.profile(model);

			verify(model, never()).addAttribute(eq(TITLE), any());

			verify(model).addAttribute(CUSTOMER_DATA, customerData);

			novalnetUtils.verifyNoInteractions();
		}
	}
}
