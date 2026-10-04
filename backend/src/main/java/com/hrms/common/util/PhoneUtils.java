// Normalizes Indian phone numbers to a bare 10-digit form before validation/storage
package com.hrms.common.util;

public final class PhoneUtils {

    public static final String INDIAN_MOBILE_REGEX = "^[6-9]\\d{9}$";

    private PhoneUtils() {}

    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[\\s-]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        return digits;
    }
}
