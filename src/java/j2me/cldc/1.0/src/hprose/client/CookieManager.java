/**********************************************************\
|                                                          |
|                          hprose                          |
|                                                          |
| Official WebSite: http://www.hprose.com/                 |
|                   http://www.hprose.net/                 |
|                   http://www.hprose.org/                 |
|                                                          |
\**********************************************************/
/**********************************************************\
 *                                                        *
 * CookieManager.java                                     *
 *                                                        *
 * cookie manager class for Java.                         *
 *                                                        *
 * LastModified: May 6, 2011                              *
 * Author: Ma Bingyao <andot@hprose.com>                  *
 *                                                        *
\**********************************************************/
package hprose.client;

import hprose.io.HproseHelper;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

public class CookieManager {
    private static final TimeZone utc = TimeZone.getTimeZone("UTC");

    private static int parseMonth(String str) {
        str = str.toLowerCase();
        switch (str.charAt(0) + str.charAt(1) + str.charAt(2) - 300) {
            case 13: return 0;
            case 1: return 1;
            case 20: return 2;
            case 23: return 3;
            case 27: return 4;
            case 33: return 5;
            case 31: return 6;
            case 17: return 7;
            case 28: return 8;
            case 26: return 9;
            case 39: return 10;
            case 0: return 11;
            default: return 0;
        }
    }
    private static Calendar parseCalendar(String str) {
        Calendar calendar = Calendar.getInstance(utc);
        if (str == null || str.equals("")) {
            return calendar;
        }
        String[] datetime = HproseHelper.split(str, ' ', 0);
        int day, month, year, hour, minute, second;
        String[] time;
        if (datetime[1].indexOf('-') > 0) {
            String[] date = HproseHelper.split(datetime[1], '-', 0);
            day = Integer.parseInt(date[0]);
            month = parseMonth(date[1]);
            year = Integer.parseInt(date[2]);
            time = HproseHelper.split(datetime[2], ':', 0);
        }
        else {
            day = Integer.parseInt(datetime[1]);
            month = parseMonth(datetime[2]);
            year = Integer.parseInt(datetime[3]);
            time = HproseHelper.split(datetime[4], ':', 0);
        }
        hour = Integer.parseInt(time[0]);
        minute = Integer.parseInt(time[1]);
        second = Integer.parseInt(time[2]);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Hashtable container = new Hashtable();

    public CookieManager() {
    }

    public synchronized void setCookie(Vector cookieList, String host) {
        if (cookieList == null) {
            return;
        }
        for (int i = 0, n = cookieList.size(); i < n; i++) {
            String cookieString = (String)cookieList.elementAt(i);
            if (cookieString.equals("")) {
                continue;
            }
            String[] cookies = HproseHelper.split(cookieString.trim(), ';', 0);
            Hashtable cookie = new Hashtable();
            String[] value = HproseHelper.split(cookies[0].trim(), '=', 2);
            cookie.put("name", value[0]);
            if (value.length == 2) {
                cookie.put("value", value[1]);
            } else {
                cookie.put("value", "");
            }
            for (int j = 1, m = cookies.length; j < m; j++) {
                value = HproseHelper.split(cookies[j].trim(), '=', 2);
                if (value.length == 2) {
                    cookie.put(value[0].toUpperCase(), value[1]);
                } else {
                    cookie.put(value[0].toUpperCase(), "");
                }
            }
            // Tomcat can return SetCookie2 with path wrapped in "
            if (cookie.containsKey("PATH")) {
                String path = (String) cookie.get("PATH");
                if (path.charAt(0) == '"') {
                    path = path.substring(1);
                }
                if (path.charAt(path.length() - 1) == '"') {
                    path = path.substring(0, path.length() - 1);
                }
                cookie.put("PATH", path);
            } else {
                cookie.put("PATH", "/");
            }
            if (cookie.containsKey("EXPIRES")) {
                Calendar expires = parseCalendar((String) cookie.get("EXPIRES"));
                cookie.put("EXPIRES", expires);
            }
            if (cookie.containsKey("DOMAIN")) {
                cookie.put("DOMAIN", ((String) cookie.get("DOMAIN")).toLowerCase());
            } else {
                cookie.put("DOMAIN", host);
            }
            if (!container.containsKey(cookie.get("DOMAIN"))) {
                container.put(cookie.get("DOMAIN"), new Hashtable());
            }
            ((Hashtable) container.get(cookie.get("DOMAIN"))).put(cookie.get("name"), cookie);
        }
    }

    public synchronized String getCookie(String host, String path, boolean secure) {
        StringBuffer cookies = new StringBuffer();
        Enumeration e = container.keys();
        while (e.hasMoreElements()) {
            String domain = (String) e.nextElement();
            Hashtable cookieList = (Hashtable) container.get(domain);
            if (host.endsWith(domain)) {
                Vector names = new Vector();
                Enumeration e2 = cookieList.keys();
                while (e2.hasMoreElements()) {
                    String name = (String) e2.nextElement();
                    Hashtable cookie = (Hashtable) cookieList.get(name);
                    if (cookie.containsKey("EXPIRES") && Calendar.getInstance(utc).after((Calendar) cookie.get("EXPIRES"))) {
                        names.addElement(name);
                    } else if (path.startsWith((String) cookie.get("PATH"))) {
                        if (((secure && cookie.containsKey("SECURE")) || !cookie.containsKey("SECURE")) && !((String) cookie.get("value")).equals("")) {
                            if (cookies.length() != 0) {
                                cookies.append("; ");
                            }
                            cookies.append(cookie.get("name"));
                            cookies.append('=');
                            cookies.append(cookie.get("value"));
                        }
                    }
                }
                for (int i = 0, n = names.size(); i < n; i++) {
                    ((Hashtable) container.get(domain)).remove(names.elementAt(i));
                }
            }
        }
        return cookies.toString();
    }
}
