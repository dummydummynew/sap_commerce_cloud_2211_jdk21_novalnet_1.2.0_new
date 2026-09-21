/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.service.checkout.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.enums.CustomerType;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novalnet.model.NovalnetCreditCardPaymentModeModel;
import com.novalnet.model.NovalnetInvoicePaymentModeModel;
import com.novalnet.model.NovalnetPayPalPaymentModeModel;
import com.novalnet.model.NovalnetPaymentInfoModel;
import com.novalnet.service.order.NovalnetOrderService;
import com.novalnet.service.payment.NovalnetPaymentService;


@UnitTest
@ExtendWith(MockitoExtension.class)
class DefaultNovalnetCheckoutServiceTest
{
	private static final String ORDER_COMMENTS = "Novalnet transaction comment<br>with a line break";

	private static final String CURRENT_PAYMENT_CREDIT_CARD = "novalnetCreditCard";

	private static final String CURRENT_PAYMENT_INVOICE = "novalnetInvoice";

	private static final String TRANSACTION_STATUS_CONFIRMED = "CONFIRMED";

	private static final String TRANSACTION_STATUS_PENDING = "PENDING";

	private static final int ORDER_AMOUNT_CENT = 1999;

	private static final int ZERO_AMOUNT_CENT = 0;

	private static final String CURRENCY = "EUR";

	private static final String TRANSACTION_ID = "123456789";

	private static final String EMAIL = "customer@example.com";

	private static final String BANK_DETAILS = "IBAN: DE00000000000000000000";

	private static final String ORDER_CODE = "ORDER123";

	private static final String GUEST_UID_WITH_SEPARATOR = "anonymous|guest@example.com";

	private static final String GUEST_EMAIL = "guest@example.com";

	@Spy
	@InjectMocks
	private DefaultNovalnetCheckoutService service;

	@Mock
	private Converter<AddressData, AddressModel> addressReverseConverterMock;

	@Mock
	private NovalnetPaymentService novalnetPaymentServiceMock;

	@Mock
	private PaymentModeService paymentModeServiceMock;

	@Mock
	private NovalnetOrderService novalnetOrderServiceMock;

	@Mock
	private SessionService sessionServiceMock;

	@Mock
	private CartService cartServiceMock;

	@Mock
	private ModelService modelServiceMock;

	@Mock
	private Converter<OrderModel, OrderData> orderConverterMock;

	@Mock
	private CartModel cartModelMock;

	@Mock
	private UserModel userModelMock;

	@Mock
	private CustomerModel currentUserMock;

	@Mock
	private AddressData addressDataMock;

	@Mock
	private AddressModel billingAddressModelMock;

	@Mock
	private OrderModel orderModelMock;

	@Mock
	private OrderData orderDataMock;

	@Mock
	private OrderHistoryEntryModel orderHistoryEntryModelMock;

	@Mock
	private PaymentTransactionEntryModel paymentTransactionEntryModelMock;

	@BeforeEach
	void setUp()
	{
		service.setCartService(cartServiceMock);
	}

	@SuppressWarnings("unchecked")
	private static <T> T invokeProtected(final Object target, final String methodName, final Class<?>[] paramTypes,
			final Object... args)
	{
		Class<?> type = target.getClass();
		NoSuchMethodException lastFailure = null;

		while (type != null)
		{
			try
			{
				final Method method = type.getDeclaredMethod(methodName, paramTypes);

				method.setAccessible(true);

				return (T) method.invoke(target, args);
			}
			catch (final NoSuchMethodException e)
			{
				lastFailure = e;
				type = type.getSuperclass();
			}
			catch (final ReflectiveOperationException e)
			{
				throw new IllegalStateException("Failed to invoke " + methodName + " reflectively", e);
			}
		}

		throw new IllegalStateException("Method " + methodName + " not found on " + target.getClass(), lastFailure);
	}

	private void stubSaveOrderFlow()
	{
		when(cartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		lenient().when(modelServiceMock.create(AddressModel.class)).thenReturn(billingAddressModelMock);
		lenient().when(modelServiceMock.create(OrderHistoryEntryModel.class)).thenReturn(orderHistoryEntryModelMock);
		lenient().when(addressReverseConverterMock.convert(eq(addressDataMock), eq(billingAddressModelMock)))
				.thenReturn(billingAddressModelMock);
		lenient().when(novalnetPaymentServiceMock.createTransactionEntry(anyString(), any(CartModel.class), any(Integer.class),
				anyString(), anyString())).thenReturn(paymentTransactionEntryModelMock);
		lenient().when(orderModelMock.getCode()).thenReturn(ORDER_CODE);
		lenient().when(orderConverterMock.convert(orderModelMock)).thenReturn(orderDataMock);

		invokeProtected(lenient().doReturn(currentUserMock).when(service), "getCurrentUserForCheckout", new Class<?>[0]);
		invokeProtected(lenient().doNothing().when(service), "beforePlaceOrder", new Class<?>[]
		{ CartModel.class }, cartModelMock);
		invokeProtected(lenient().doReturn(orderModelMock).when(service), "placeOrder", new Class<?>[]
		{ CartModel.class }, cartModelMock);
		invokeProtected(lenient().doNothing().when(service), "afterPlaceOrder", new Class<?>[]
		{ CartModel.class, OrderModel.class }, cartModelMock, orderModelMock);
	}


	@Test
	void shouldSaveOrderDataAndSetFullPaidAmountForConfirmedCreditCardPayment() throws InvalidCartException
	{
		stubSaveOrderFlow();

		NovalnetCreditCardPaymentModeModel paymentModeMock = Mockito.mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeServiceMock.getPaymentModeForCode(CURRENT_PAYMENT_CREDIT_CARD)).thenReturn(paymentModeMock);

		OrderData result = service.saveOrderData(ORDER_COMMENTS, CURRENT_PAYMENT_CREDIT_CARD, TRANSACTION_STATUS_CONFIRMED,
				ORDER_AMOUNT_CENT, CURRENCY, TRANSACTION_ID, EMAIL, addressDataMock, BANK_DETAILS);

		assertEquals(orderDataMock, result);

		verify(orderModelMock).setPaymentMode(paymentModeMock);
		verify(orderModelMock).setPaidAmount(ORDER_AMOUNT_CENT);

		verify(novalnetOrderServiceMock).updateOrderStatus(eq(ORDER_CODE), any(NovalnetPaymentInfoModel.class));

		verify(modelServiceMock).saveAll(any(NovalnetPaymentInfoModel.class), eq(cartModelMock), eq(billingAddressModelMock));

		verify(modelServiceMock).saveAll(eq(orderModelMock), eq(orderHistoryEntryModelMock));

		verify(modelServiceMock).save(any());
	}


	@Test
	void shouldSetZeroPaidAmountForInvoicePaymentEvenWhenConfirmed() throws InvalidCartException
	{
		stubSaveOrderFlow();

		NovalnetInvoicePaymentModeModel paymentModeMock = Mockito.mock(NovalnetInvoicePaymentModeModel.class);

		when(paymentModeServiceMock.getPaymentModeForCode(CURRENT_PAYMENT_INVOICE)).thenReturn(paymentModeMock);

		service.saveOrderData(ORDER_COMMENTS, CURRENT_PAYMENT_INVOICE, TRANSACTION_STATUS_CONFIRMED, ORDER_AMOUNT_CENT, CURRENCY,
				TRANSACTION_ID, EMAIL, addressDataMock, BANK_DETAILS);

		verify(orderModelMock).setPaidAmount(ZERO_AMOUNT_CENT);
	}


	@Test
	void shouldSetZeroPaidAmountWhenTransactionStatusIsPending() throws InvalidCartException
	{
		stubSaveOrderFlow();

		NovalnetCreditCardPaymentModeModel paymentModeMock = Mockito.mock(NovalnetCreditCardPaymentModeModel.class);

		when(paymentModeServiceMock.getPaymentModeForCode(CURRENT_PAYMENT_CREDIT_CARD)).thenReturn(paymentModeMock);

		service.saveOrderData(ORDER_COMMENTS, CURRENT_PAYMENT_CREDIT_CARD, TRANSACTION_STATUS_PENDING, ORDER_AMOUNT_CENT, CURRENCY,
				TRANSACTION_ID, EMAIL, addressDataMock, BANK_DETAILS);

		verify(orderModelMock).setPaidAmount(ZERO_AMOUNT_CENT);
	}


	@Test
	void shouldNotAssignPaymentModeWhenPaymentModeServiceReturnsNull() throws InvalidCartException
	{
		stubSaveOrderFlow();

		when(paymentModeServiceMock.getPaymentModeForCode(CURRENT_PAYMENT_CREDIT_CARD)).thenReturn(null);

		service.saveOrderData(ORDER_COMMENTS, CURRENT_PAYMENT_CREDIT_CARD, TRANSACTION_STATUS_CONFIRMED, ORDER_AMOUNT_CENT,
				CURRENCY, TRANSACTION_ID, EMAIL, addressDataMock, BANK_DETAILS);

		verify(orderModelMock, never()).setPaymentMode(any());
	}


	@Test
	void shouldThrowIllegalStateExceptionWhenPaymentModeTypeDoesNotMatchExpectedCode()
	{
		stubSaveOrderFlow();

		NovalnetPayPalPaymentModeModel mismatchedPaymentModeMock = Mockito.mock(NovalnetPayPalPaymentModeModel.class);

		when(paymentModeServiceMock.getPaymentModeForCode(CURRENT_PAYMENT_CREDIT_CARD)).thenReturn(mismatchedPaymentModeMock);

		assertThrows(IllegalStateException.class, () -> service.saveOrderData(ORDER_COMMENTS, CURRENT_PAYMENT_CREDIT_CARD,
				TRANSACTION_STATUS_CONFIRMED, ORDER_AMOUNT_CENT, CURRENCY, TRANSACTION_ID, EMAIL, addressDataMock, BANK_DETAILS));
	}


	@Test
	void shouldSaveBillingAddressAndCartViaModelService()
	{
		service.saveData(billingAddressModelMock, cartModelMock);

		verify(modelServiceMock).saveAll(billingAddressModelMock, cartModelMock);
	}


	@Test
	void shouldCreateNewBillingAddressViaModelService()
	{
		when(modelServiceMock.create(AddressModel.class)).thenReturn(billingAddressModelMock);

		AddressModel result = service.getBillingAddress();

		assertEquals(billingAddressModelMock, result);

		verify(modelServiceMock).create(AddressModel.class);
	}


	@Test
	void shouldReturnTrueWhenCurrentCartUserIsAGuestCustomer()
	{
		CustomerModel guestCustomerMock = Mockito.mock(CustomerModel.class);

		when(guestCustomerMock.getType()).thenReturn(CustomerType.GUEST);

		when(cartModelMock.getUser()).thenReturn(guestCustomerMock);

		when(cartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		final boolean result = service.isGuestUser();

		assertEquals(true, result);
	}


	@Test
	void shouldReturnFalseWhenCurrentCartUserIsARegisteredCustomer()
	{
		CustomerModel registeredCustomerMock = Mockito.mock(CustomerModel.class);

		when(registeredCustomerMock.getType()).thenReturn(CustomerType.REGISTERED);

		when(cartModelMock.getUser()).thenReturn(registeredCustomerMock);

		when(cartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		boolean result = service.isGuestUser();

		assertEquals(false, result);
	}


	@Test
	void shouldReturnFalseWhenCartUserIsNotACustomerModel()
	{
		when(cartModelMock.getUser()).thenReturn(userModelMock);

		when(cartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		boolean result = service.isGuestUser();

		assertEquals(false, result);
	}


	@Test
	void shouldReturnEmailPortionOfGuestUidWhenCurrentCartUserIsAGuest()
	{
		CustomerModel guestCustomerMock = Mockito.mock(CustomerModel.class);

		when(guestCustomerMock.getType()).thenReturn(CustomerType.GUEST);

		when(guestCustomerMock.getUid()).thenReturn(GUEST_UID_WITH_SEPARATOR);

		when(cartModelMock.getUser()).thenReturn(guestCustomerMock);

		when(cartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		String result = service.getGuestEmail();

		assertEquals(GUEST_EMAIL, result);
	}


	@Test
	void shouldReturnNullGuestEmailWhenCurrentCartUserIsNotAGuest()
	{
		CustomerModel registeredCustomerMock = Mockito.mock(CustomerModel.class);

		when(registeredCustomerMock.getType()).thenReturn(CustomerType.REGISTERED);

		when(cartModelMock.getUser()).thenReturn(registeredCustomerMock);

		when(cartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		String result = service.getGuestEmail();

		assertNull(result);
	}


	@Test
	void shouldReturnCartFromInheritedGetCartHook()
	{
		invokeProtected(Mockito.doReturn(cartModelMock).when(service), "getCart", new Class<?>[0]);

		CartModel result = service.getNovalnetCheckoutCart();

		assertEquals(cartModelMock, result);
	}


	@Test
	void shouldReturnCurrentUserFromInheritedGetCurrentUserForCheckoutHook()
	{
		invokeProtected(Mockito.doReturn(currentUserMock).when(service), "getCurrentUserForCheckout", new Class<?>[0]);

		UserModel result = service.getCurrentUser();

		assertEquals(currentUserMock, result);

		invokeProtected(verify(service), "getCurrentUserForCheckout", new Class<?>[0]);
	}


	@Test
	void shouldWireSuppliedCartServiceSoSubsequentCallsUseIt()
	{
		CartService anotherCartServiceMock = Mockito.mock(CartService.class);

		when(anotherCartServiceMock.getSessionCart()).thenReturn(cartModelMock);

		CustomerModel registeredCustomerMock = Mockito.mock(CustomerModel.class);

		when(registeredCustomerMock.getType()).thenReturn(CustomerType.REGISTERED);

		when(cartModelMock.getUser()).thenReturn(registeredCustomerMock);

		service.setCartService(anotherCartServiceMock);

		boolean result = service.isGuestUser();

		assertEquals(false, result);

		verify(anotherCartServiceMock).getSessionCart();
	}
}