/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.utils;

/**
 * The Class LoggerUtils.
 */
public class LoggerUtils {
	
	/**
	 * Where from.
	 *
	 * @return the string
	 */
	public static String whereFrom() {
		return Thread.currentThread().getStackTrace()[3].toString();
	}
}
