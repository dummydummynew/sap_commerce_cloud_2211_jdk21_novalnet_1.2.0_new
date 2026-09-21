/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.novalnet.service.payment;

public interface NovalnetcoreService
{
	public String getHybrisLogoUrl(String logoCode);

	public void createLogo(String logoCode);
}
