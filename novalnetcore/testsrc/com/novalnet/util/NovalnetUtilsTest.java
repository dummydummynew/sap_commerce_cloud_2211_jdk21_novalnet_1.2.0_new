/*
 *
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.novalnet.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.store.BaseStoreModel;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import de.novalnet.beans.NnCallbackEventData;
import de.novalnet.beans.NnCallbackRequestData;
import de.novalnet.beans.NnCallbackResultData;
import de.novalnet.beans.NnCallbackTransactionData;

import jakarta.servlet.http.HttpServletRequest;


/**
 * Unit tests for {@link NovalnetUtils}.
 */
@UnitTest
@ExtendWith(MockitoExtension.class)
public class NovalnetUtilsTest
{
	private static final String VALID_ACCESS_KEY = "abc123XYZ";
	private static final String SAMPLE_TID = "176205220123456";
	private static final String STATUS_CONFIRMED = "CONFIRMED";
	private static final String DATE_PATTERN = "dd.MM.yyyy";
	private static final String SHA_256 = "SHA-256";

	private static final int PAST_DATE_OFFSET_DAYS = 10;
	private static final int SHA_256_HEX_LENGTH = 64;
	private static final int TEST_IP_FIRST_OCTET = 203;
	private static final int TEST_IP_SECOND_OCTET = 0;
	private static final int TEST_IP_THIRD_OCTET = 113;
	private static final int TEST_IP_LAST_OCTET = 10;
	private static final int HEX_STRING_MULTIPLIER = 2;

	private HttpServletRequest requestMock;
	private BaseStoreModel baseStoreMock;

	@BeforeEach
	public void setUp()
	{
		requestMock = mock(HttpServletRequest.class);
		baseStoreMock = mock(BaseStoreModel.class);
	}

	/**
	 * Verifies that a negative offset returns the expected past date.
	 */
	@Test
	public void shouldReturnPastDateWhenDateOffsetIsNegative()
	{
		final Calendar expected = Calendar.getInstance();
		expected.add(Calendar.DATE, -PAST_DATE_OFFSET_DAYS);

		final SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);

		assertThat(NovalnetUtils.formatDate(-PAST_DATE_OFFSET_DAYS))
				.isEqualTo(formatter.format(expected.getTime()));
	}

	/**
	 * Verifies that a generated checksum is a valid sixty-four-character hexadecimal digest.
	 */
	@Test
	public void shouldReturnSixtyFourCharacterHexDigestWhenGeneratingChecksum()
	{
		final String checksum = NovalnetUtils.generateChecksum("some-token-value");

		assertThat(checksum).isNotNull();
		assertThat(checksum).hasSize(SHA_256_HEX_LENGTH);
		assertThat(checksum).matches("[0-9a-f]{" + SHA_256_HEX_LENGTH + "}");
	}

	/**
	 * Verifies that callback IP validation does not throw an exception in test mode.
	 */
	@Test
	public void shouldNotThrowExceptionWhenCallbackIpValidationRunsInTestMode() throws Exception
	{
		when(baseStoreMock.getNovalnetVendorscriptTestMode()).thenReturn(Boolean.TRUE);
		when(requestMock.getHeader("X-Forwarded-For")).thenReturn(null);
		when(requestMock.getRemoteAddr()).thenReturn("203.0.113.5");

		final InetAddress novalnetAddress = InetAddress.getByAddress(new byte[]
		{
				(byte) TEST_IP_FIRST_OCTET,
				(byte) TEST_IP_SECOND_OCTET,
				(byte) TEST_IP_THIRD_OCTET,
				(byte) TEST_IP_LAST_OCTET
		});

		try (MockedStatic<InetAddress> inetAddressMock = mockStatic(InetAddress.class))
		{
			inetAddressMock.when(() -> InetAddress.getByName("pay-nn.de"))
					.thenReturn(novalnetAddress);

			NovalnetUtils.validateCallbackIp(requestMock, baseStoreMock);
		}
	}

	/**
	 * Verifies that a matching callback checksum is accepted.
	 */
	@Test
	public void shouldNotThrowExceptionWhenCallbackChecksumMatches()
	{
		when(baseStoreMock.getNovalnetPaymentAccessKey()).thenReturn(VALID_ACCESS_KEY);

		final NnCallbackEventData event = new NnCallbackEventData();
		event.setTid(SAMPLE_TID);
		event.setType("PAYMENT");

		final NnCallbackTransactionData txn = new NnCallbackTransactionData();
		txn.setAmount("1999");
		txn.setCurrency("EUR");

		final NnCallbackResultData result = new NnCallbackResultData();
		result.setStatus(STATUS_CONFIRMED);

		final String tokenSource = new StringBuilder()
				.append(event.getTid())
				.append(event.getType())
				.append(result.getStatus())
				.append(txn.getAmount())
				.append(txn.getCurrency())
				.append(new StringBuilder(VALID_ACCESS_KEY.trim()).reverse())
				.toString();

		event.setChecksum(sha256Hex(tokenSource));

		final NnCallbackRequestData request = new NnCallbackRequestData();
		request.setEvent(event);
		request.setTransaction(txn);
		request.setResult(result);

		try
		{
			NovalnetUtils.validateCallbackChecksum(request, baseStoreMock);
		}
		catch (IllegalStateException ex)
		{
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

	private String sha256Hex(String value)
	{
		try
		{
			final MessageDigest digest = MessageDigest.getInstance(SHA_256);
			final byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			final StringBuilder hex = new StringBuilder(hash.length * HEX_STRING_MULTIPLIER);

			for (final byte b : hash)
			{
				hex.append(String.format("%02x", b));
			}

			return hex.toString();
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new IllegalStateException(ex);
		}
	}
}


