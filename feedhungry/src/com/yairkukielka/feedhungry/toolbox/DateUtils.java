package com.yairkukielka.feedhungry.toolbox;

import java.util.Date;

public class DateUtils {

	public static Date getDateFromJson(String sDate) {
	    long l = Long.valueOf(sDate);
	    return new Date(l);
	}

	public static Date getDateFromJson(Long lDate) {
	    return new Date(lDate);
	}
}
