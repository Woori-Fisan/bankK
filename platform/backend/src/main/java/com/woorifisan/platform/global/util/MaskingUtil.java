package com.woorifisan.platform.global.util;

import java.util.regex.Pattern;

// 누가 확장해서 마스킹 우회하는 거 막으려고 final 클래스 씀
public final class MaskingUtil {

    // new MaskingUtil() 막아버리기
    private static final Pattern SSN_PATTERN =
            Pattern.compile("(\\d{6})-?(\\d{7})");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\d{2,3})[-.]?(\\d{3,4})[-.]?(\\d{4})");

    private MaskingUtil() {}

    // static 메서드만.. 인스턴스 없이 MaskingUtil.maskSsn(...) 바로 호출 가능너
    public static String maskSsn(String value) {
        if (value == null) return null;
        return SSN_PATTERN.matcher(value).replaceAll(m -> m.group(1) + "-*******");
    }

    public static String maskAccountNo(String value) {
        if (value == null) return null;
        int len = value.length();
        if (len <= 4) return "*".repeat(len);
        return value.substring(0, 4) + "*".repeat(len - 4);
    }

    public static String maskPhoneNo(String value) {
        if (value == null) return null;
        return PHONE_PATTERN.matcher(value).replaceAll(m -> m.group(1) + "-****-" + m.group(3));
    }

    /** 성명 마스킹: 2자→김*, 3자→김*리, 4자 이상→김**리 */
    public static String maskName(String name) {
        if (name == null) return null;
        int len = name.length();
        if (len == 1) return "*";
        if (len == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(len - 2) + name.charAt(len - 1);
    }
}
