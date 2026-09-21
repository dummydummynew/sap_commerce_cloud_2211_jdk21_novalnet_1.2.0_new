/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.session.SessionService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.dto.PaymentConfigResult;
import com.novalnet.dto.payment.request.Customer;
import com.novalnet.dto.payment.request.HostedPage;
import com.novalnet.dto.payment.request.PaymentData;
import com.novalnet.dto.payment.request.Transaction;
import com.novalnet.model.NovalnetAliPayPaymentModeModel;
import com.novalnet.model.NovalnetApplePayPaymentModeModel;
import com.novalnet.model.NovalnetBancontactPaymentModeModel;
import com.novalnet.model.NovalnetBlikPaymentModeModel;
import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitAchPaymentModeModel;
import com.novalnet.model.NovalnetDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetEpsPaymentModeModel;
import com.novalnet.model.NovalnetGooglePayPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import com.novalnet.model.NovalnetGuaranteedInvoicePaymentModeModel;
import com.novalnet.model.NovalnetIdealPaymentModeModel;
import com.novalnet.model.NovalnetInvoicePaymentModeModel;
import com.novalnet.model.NovalnetMbWayPaymentModeModel;
import com.novalnet.model.NovalnetMultibancoPaymentModeModel;
import com.novalnet.model.NovalnetOnlineBankTransferPaymentModeModel;
import com.novalnet.model.NovalnetPayPalPaymentModeModel;
import com.novalnet.model.NovalnetPostFinanceCardPaymentModeModel;
import com.novalnet.model.NovalnetPostFinancePaymentModeModel;
import com.novalnet.model.NovalnetPrepaymentPaymentModeModel;
import com.novalnet.model.NovalnetPrzelewy24PaymentModeModel;
import com.novalnet.model.NovalnetTrustlyPaymentModeModel;
import com.novalnet.model.NovalnetTwintPaymentModeModel;
import com.novalnet.model.NovalnetWechatPayPaymentModeModel;
import com.novalnet.service.checkout.NovalnetCheckoutService;

import jakarta.servlet.http.HttpServletRequest;



@UnitTest
@ExtendWith(MockitoExtension.class)
public class DefaultNovalnetPaymentHandlerServiceTest
{

	private static final Integer ORDER_AMOUNT_CENT = 10000;
	private static final Integer TEST_MODE_ENABLED = 1;
	private static final Integer TEST_MODE_DISABLED = 0;
	private static final Integer SEPA_DUE_DATE_DAYS = 7;
	private static final Integer SEPA_ONHOLD_AMOUNT_CENT = 5000;
	private static final Integer GUARANTEED_SEPA_ONHOLD_AMOUNT_CENT = 2000;
	private static final Integer CREDIT_CARD_ONHOLD_AMOUNT_CENT = 5000;
	private static final Integer INVOICE_DUE_DATE_DAYS = 14;
	private static final Integer INVOICE_ONHOLD_AMOUNT_CENT = 3000;
	private static final Integer PREPAYMENT_DUE_DATE_DAYS = 21;
	private static final Integer GUARANTEED_INVOICE_ONHOLD_AMOUNT_CENT = 2500;
	private static final Integer GOOGLE_PAY_ONHOLD_AMOUNT_CENT = 1000;
	private static final Integer APPLE_PAY_ONHOLD_AMOUNT_CENT = 1000;

	@InjectMocks
	private DefaultNovalnetPaymentHandlerService testObj;

	@Mock
	private SessionService sessionService;

	@Mock
	private NovalnetCheckoutService novalnetCheckoutService;

	private Object onholdActionConstant(Class<?> modelClass, String getterName, String constantName)
	{
		Class<?> enumType;

		try
		{
			Method getter = modelClass.getMethod(getterName);
			enumType = getter.getReturnType();
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException("Getter '" + getterName + "' not found on " + modelClass.getName(), e);
		}

		try
		{
			Field field = enumType.getField(constantName);
			return field.get(null);
		}
		catch (NoSuchFieldException e)
		{
			throw new IllegalStateException("No constant '" + constantName + "' found on " + enumType.getName(), e);
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException("Unable to access constant '" + constantName + "' on " + enumType.getName(), e);
		}
	}

	@Test
	public void shouldConfigureSepaDueDateVerificationAndReuseToken()
	{
		NovalnetDirectDebitSepaPaymentModeModel model = mock(NovalnetDirectDebitSepaPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetDueDate()).thenReturn(SEPA_DUE_DATE_DAYS);
		when(model.getNovalnetOnholdAmount()).thenReturn(SEPA_ONHOLD_AMOUNT_CENT);
		when(model.getNovalnetOneClickShopping()).thenReturn(true);

		doReturn(
				onholdActionConstant(NovalnetDirectDebitSepaPaymentModeModel.class, "getOnholdActionTypeWithZeroAmount", "AUTHORIZE"))
						.when(model).getOnholdActionTypeWithZeroAmount();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute("novalnetDirectDebitSepaStorePaymentData")).thenReturn(false);

		when(sessionService.<String> getAttribute("novalnetDirectDebitSepatoken")).thenReturn("saved-sepa-token");

		Transaction transaction = new Transaction();
		PaymentData paymentData = new PaymentData();

		PaymentConfigResult result = testObj.handlePayment("novalnetDirectDebitSepa", model, transaction, paymentData,
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(transaction.getDue_date()).isNotNull();
		assertThat(result.isVerifyPaymentData()).isTrue();
		assertThat(paymentData.getToken()).isEqualTo("saved-sepa-token");
	}


	@Test
	public void shouldConfigureAchTestModeAndReuseToken()
	{
		NovalnetDirectDebitAchPaymentModeModel model = mock(NovalnetDirectDebitAchPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);
		when(model.getNovalnetOneClickShopping()).thenReturn(true);

		doReturn(onholdActionConstant(NovalnetDirectDebitAchPaymentModeModel.class, "getOnholdActionTypeWithoutAuthorize",
				"AUTHORIZE_WITH_ZERO_AMOUNT")).when(model).getOnholdActionTypeWithoutAuthorize();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute("novalnetDirectDebitAchStorePaymentData")).thenReturn(false);

		when(sessionService.<String> getAttribute("novalnetDirectDebitAchtoken")).thenReturn("saved-ach-token");

		PaymentData paymentData = new PaymentData();

		PaymentConfigResult result = testObj.handlePayment("novalnetDirectDebitAch", model, new Transaction(), paymentData,
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
		assertThat(result.isZeroAmountBooking()).isTrue();
		assertThat(paymentData.getToken()).isEqualTo("saved-ach-token");
	}


	@Test
	public void shouldConfigureGuaranteedSepaDobVerificationAndReuseToken()
	{
		NovalnetGuaranteedDirectDebitSepaPaymentModeModel model = mock(NovalnetGuaranteedDirectDebitSepaPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetOnholdAmount()).thenReturn(GUARANTEED_SEPA_ONHOLD_AMOUNT_CENT);
		when(model.getNovalnetOneClickShopping()).thenReturn(true);

		doReturn(
				onholdActionConstant(NovalnetGuaranteedDirectDebitSepaPaymentModeModel.class, "getNovalnetOnholdAction", "AUTHORIZE"))
						.when(model).getNovalnetOnholdAction();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute("novalnetGuaranteedDirectDebitSepaStorePaymentData")).thenReturn(false);

		when(sessionService.<String> getAttribute("novalnetDirectDebitSepatoken")).thenReturn("saved-guaranteed-sepa-token");

		when(sessionService.<String> getAttribute("novalnetGuaranteedDirectDebitSepaDateOfBirth")).thenReturn("1990-01-01");

		Customer customer = new Customer();
		PaymentData paymentData = new PaymentData();

		PaymentConfigResult result = testObj.handlePayment("novalnetGuaranteedDirectDebitSepa", model, new Transaction(),
				paymentData, customer, ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(customer.getBirth_date()).isEqualTo("1990-01-01");
		assertThat(result.isVerifyPaymentData()).isTrue();
		assertThat(paymentData.getToken()).isEqualTo("saved-guaranteed-sepa-token");
	}

	@Test
	public void shouldConfigurePayPalRedirectTestModeAndVerification()
	{
		NovalnetPayPalPaymentModeModel model = mock(NovalnetPayPalPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetOnholdAmount()).thenReturn(SEPA_ONHOLD_AMOUNT_CENT);

		doReturn(onholdActionConstant(NovalnetPayPalPaymentModeModel.class, "getNovalnetOnholdAction", "AUTHORIZE")).when(model)
				.getNovalnetOnholdAction();

		PaymentConfigResult result = testObj.handlePayment("novalnetPayPal", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(result.isVerifyPaymentData()).isTrue();
	}


	@Test
	public void shouldConfigureCreditCardEnforce3dAndReuseToken()
	{
		NovalnetCreditCardPaymentModeModel model = mock(NovalnetCreditCardPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetOnholdAmount()).thenReturn(CREDIT_CARD_ONHOLD_AMOUNT_CENT);
		when(model.getNovalnetEnforce3D()).thenReturn(true);
		when(model.getNovalnetOneClickShopping()).thenReturn(true);

		doReturn(onholdActionConstant(NovalnetCreditCardPaymentModeModel.class, "getOnholdActionTypeWithZeroAmount", "AUTHORIZE"))
				.when(model).getOnholdActionTypeWithZeroAmount();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute("novalnetCreditCardStorePaymentData")).thenReturn(false);

		when(sessionService.<String> getAttribute("novalnetCreditCardtoken")).thenReturn("saved-cc-token");

		Transaction transaction = new Transaction();
		PaymentData paymentData = new PaymentData();

		PaymentConfigResult result = testObj.handlePayment("novalnetCreditCard", model, transaction, paymentData, new Customer(),
				ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(transaction.getEnforce_3d()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(result.isVerifyPaymentData()).isTrue();
		assertThat(paymentData.getToken()).isEqualTo("saved-cc-token");
	}

	@Test
	public void shouldConfigureInvoiceDueDateAndVerification()
	{
		NovalnetInvoicePaymentModeModel model = mock(NovalnetInvoicePaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);
		when(model.getNovalnetDueDate()).thenReturn(INVOICE_DUE_DATE_DAYS);
		when(model.getNovalnetOnholdAmount()).thenReturn(INVOICE_ONHOLD_AMOUNT_CENT);

		doReturn(onholdActionConstant(NovalnetInvoicePaymentModeModel.class, "getNovalnetOnholdAction", "AUTHORIZE")).when(model)
				.getNovalnetOnholdAction();

		Transaction transaction = new Transaction();

		PaymentConfigResult result = testObj.handlePayment("novalnetInvoice", model, transaction, new PaymentData(), new Customer(),
				ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(transaction.getDue_date()).isNotNull();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
		assertThat(result.isVerifyPaymentData()).isTrue();
	}


	@Test
	public void shouldConfigurePrepaymentDueDateAndTestMode()
	{
		NovalnetPrepaymentPaymentModeModel model = mock(NovalnetPrepaymentPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetDueDate()).thenReturn(PREPAYMENT_DUE_DATE_DAYS);

		Transaction transaction = new Transaction();

		PaymentConfigResult result = testObj.handlePayment("novalnetPrepayment", model, transaction, new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(transaction.getDue_date()).isNotNull();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureMultibancoTestModeWithoutRedirect()
	{
		NovalnetMultibancoPaymentModeModel model = mock(NovalnetMultibancoPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetMultibanco", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(result.isRedirect()).isFalse();
	}

	@Test
	public void shouldConfigureTwintRedirectAndTestMode()
	{
		NovalnetTwintPaymentModeModel model = mock(NovalnetTwintPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetTwint", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureMbWayRedirectAndTestMode()
	{
		NovalnetMbWayPaymentModeModel model = mock(NovalnetMbWayPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);

		PaymentConfigResult result = testObj.handlePayment("novalnetMbWay", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
	}

	@Test
	public void shouldConfigureTrustlyRedirectAndTestMode()
	{
		NovalnetTrustlyPaymentModeModel model = mock(NovalnetTrustlyPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetTrustly", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureBlikRedirectAndTestMode()
	{
		NovalnetBlikPaymentModeModel model = mock(NovalnetBlikPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);

		PaymentConfigResult result = testObj.handlePayment("novalnetBlik", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
	}


	@Test
	public void shouldConfigureWechatPayRedirectAndTestMode()
	{
		NovalnetWechatPayPaymentModeModel model = mock(NovalnetWechatPayPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetWechatPay", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureAlipayRedirectAndTestMode()
	{
		NovalnetAliPayPaymentModeModel model = mock(NovalnetAliPayPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);

		PaymentConfigResult result = testObj.handlePayment("novalnetAlipay", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
	}

	@Test
	public void shouldConfigureGuaranteedInvoiceDobAndVerification()
	{
		NovalnetGuaranteedInvoicePaymentModeModel model = mock(NovalnetGuaranteedInvoicePaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetOnholdAmount()).thenReturn(GUARANTEED_INVOICE_ONHOLD_AMOUNT_CENT);

		doReturn(onholdActionConstant(NovalnetGuaranteedInvoicePaymentModeModel.class, "getNovalnetOnholdAction", "AUTHORIZE"))
				.when(model).getNovalnetOnholdAction();

		when(sessionService.<String> getAttribute("novalnetGuaranteedInvoiceDateOfBirth")).thenReturn("1985-05-20");

		Customer customer = new Customer();

		PaymentConfigResult result = testObj.handlePayment("novalnetGuaranteedInvoice", model, new Transaction(), new PaymentData(),
				customer, ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(customer.getBirth_date()).isEqualTo("1985-05-20");
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(result.isVerifyPaymentData()).isTrue();
	}


	@Test
	public void shouldConfigureOnlineBankTransferRedirectAndTestMode()
	{
		NovalnetOnlineBankTransferPaymentModeModel model = mock(NovalnetOnlineBankTransferPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetOnlineBankTransfer", model, new Transaction(),
				new PaymentData(), new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureBancontactRedirectAndTestMode()
	{
		NovalnetBancontactPaymentModeModel model = mock(NovalnetBancontactPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);

		PaymentConfigResult result = testObj.handlePayment("novalnetBancontact", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
	}

	@Test
	public void shouldConfigureIdealRedirectAndTestMode()
	{
		NovalnetIdealPaymentModeModel model = mock(NovalnetIdealPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetIdeal", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureGooglePayWalletTokenRedirectAndEnforce3d()
	{
		NovalnetGooglePayPaymentModeModel model = mock(NovalnetGooglePayPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetOnholdAmount()).thenReturn(GOOGLE_PAY_ONHOLD_AMOUNT_CENT);
		when(model.getNovalnetEnforce3D()).thenReturn(true);

		doReturn(onholdActionConstant(NovalnetGooglePayPaymentModeModel.class, "getOnholdActionTypeWithZeroAmount", "AUTHORIZE"))
				.when(model).getOnholdActionTypeWithZeroAmount();

		HttpServletRequest request = mock(HttpServletRequest.class);

		when(request.getParameter("token")).thenReturn("google-wallet-token");

		when(request.getParameter("doRedirect")).thenReturn("true");

		Transaction transaction = new Transaction();
		PaymentData paymentData = new PaymentData();

		PaymentConfigResult result = testObj.handlePayment("novalnetGooglePay", model, transaction, paymentData, new Customer(),
				ORDER_AMOUNT_CENT, request, new HostedPage());

		assertThat(paymentData.getWallet_token()).isEqualTo("google-wallet-token");
		assertThat(result.isRedirect()).isTrue();
		assertThat(transaction.getEnforce_3d()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigureApplePayHostedPageRedirectAndReuseToken()
	{
		NovalnetApplePayPaymentModeModel model = mock(NovalnetApplePayPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);
		when(model.getNovalnetOnholdAmount()).thenReturn(APPLE_PAY_ONHOLD_AMOUNT_CENT);
		when(model.getNovalnetOneClickShopping()).thenReturn(true);

		doReturn(onholdActionConstant(NovalnetApplePayPaymentModeModel.class, "getOnholdActionTypeWithZeroAmount", "AUTHORIZE"))
				.when(model).getOnholdActionTypeWithZeroAmount();

		when(novalnetCheckoutService.isGuestUser()).thenReturn(false);

		when(sessionService.<Boolean> getAttribute("novalnetApplePayStorePaymentData")).thenReturn(false);

		when(sessionService.<String> getAttribute("novalnetApplePaytoken")).thenReturn("saved-apple-pay-token");

		PaymentData paymentData = new PaymentData();
		HostedPage hostedPage = new HostedPage();

		PaymentConfigResult result = testObj.handlePayment("novalnetApplePay", model, new Transaction(), paymentData,
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), hostedPage);

		assertThat(hostedPage.getDisplay_payments()).isEqualTo(List.of("APPLEPAY"));
		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
		assertThat(paymentData.getToken()).isEqualTo("saved-apple-pay-token");
	}

	@Test
	public void shouldConfigureEpsRedirectAndTestMode()
	{
		NovalnetEpsPaymentModeModel model = mock(NovalnetEpsPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetEps", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}

	@Test
	public void shouldConfigurePostFinanceRedirectAndTestMode()
	{
		NovalnetPostFinancePaymentModeModel model = mock(NovalnetPostFinancePaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);

		PaymentConfigResult result = testObj.handlePayment("novalnetPostFinance", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
	}

	@Test
	public void shouldConfigurePostFinanceCardRedirectAndTestMode()
	{
		NovalnetPostFinanceCardPaymentModeModel model = mock(NovalnetPostFinanceCardPaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(true);

		PaymentConfigResult result = testObj.handlePayment("novalnetPostFinanceCard", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_ENABLED);
	}


	@Test
	public void shouldConfigurePrzelewy24RedirectAndTestMode()
	{
		NovalnetPrzelewy24PaymentModeModel model = mock(NovalnetPrzelewy24PaymentModeModel.class);

		when(model.getNovalnetTestMode()).thenReturn(false);

		PaymentConfigResult result = testObj.handlePayment("novalnetPrzelewy24", model, new Transaction(), new PaymentData(),
				new Customer(), ORDER_AMOUNT_CENT, mock(HttpServletRequest.class), new HostedPage());

		assertThat(result.isRedirect()).isTrue();
		assertThat(result.getTestMode()).isEqualTo(TEST_MODE_DISABLED);
	}
}

