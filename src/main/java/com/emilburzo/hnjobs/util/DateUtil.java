package com.emilburzo.hnjobs.util;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {

    /**
     * Parses dates specified as "x y ago"
     * <p/>
     * http://stackoverflow.com/questions/12049480/parsing-date-times-in-x-minutes-hours-days-weeks-months-years-ago-format
     *
     * @param relativeDate
     * @return
     */
    public static Date getAbsoluteDate(String relativeDate) {
        Pattern p = Pattern.compile("(\\d+)\\s+(.*?)s? ago");

        Map<String, Integer> fields = new HashMap<String, Integer>() {{
            put("second", Calendar.SECOND);
            put("minute", Calendar.MINUTE);
            put("hour", Calendar.HOUR);
            put("day", Calendar.DATE);
            put("week", Calendar.WEEK_OF_YEAR);
            put("month", Calendar.MONTH);
            put("year", Calendar.YEAR);
        }};

        Matcher m = p.matcher(relativeDate);

        if (m.matches()) {
            int amount = Integer.parseInt(m.group(1));
            String unit = m.group(2);

            Calendar cal = Calendar.getInstance();
            cal.add(fields.get(unit), -amount);
//            System.out.printf("%s: %tF, %<tT%n", relativeDate, cal);

            return cal.getTime();
        }

        return null;
    }
}
