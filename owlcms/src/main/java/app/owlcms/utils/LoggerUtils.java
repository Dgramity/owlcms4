/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.MDC;

import app.owlcms.init.OwlcmsSession;

/**
 * The Class LoggerUtils.
 */
public class LoggerUtils {

    public static void setWhere(String where) {
        MDC.put("page", where);
        OwlcmsSession.withFop(fop -> MDC.put("currentGroup", fop.getGroup() != null ? fop.getGroup() : "-"));
    }

    /**
     * Where from.
     *
     * @return the string
     */
    public static String stackTrace() {
        StringWriter sw = new StringWriter();
        new Exception("").printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * @param t
     * @return
     */
    public static String stackTrace(Throwable t) {
        // IDEA: skip from "at javax.servlet.http.HttpServlet.service" to line starting
        // with "Caused by"
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Where from.
     *
     * @return the string
     */
    public static String whereFrom() {
        return Thread.currentThread().getStackTrace()[3].toString();
    }
}
