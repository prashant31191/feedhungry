package com.yairkukielka.feedhungry.toolbox;

import java.text.DateFormat;
import java.text.Format;
import java.util.Date;

public class DateUtils {

//	private static final Format formatter = DateFormat.getDateTimeInstance();
	private static final Format formatter2 = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	
	public static Date getDateFromJson(String sDate) {
	    long l = Long.valueOf(sDate);
	    return new Date(l);
	}

	public static Date getDateFromJson(Long lDate) {
	    return new Date(lDate);
	}

	public static String dateToString(Date date) {
		return formatter2.format(date);
	}
}
