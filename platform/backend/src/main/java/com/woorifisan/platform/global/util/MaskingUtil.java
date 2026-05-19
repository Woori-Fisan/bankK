package com.woorifisan.platform.global.util;

import java.util.regex.Pattern;

// 누가 확장해서 마스킹 우회하는 거 막으려고 final 클래스 씀
public final class MaskingUtil {

    // new MaskingUtil() 막아버리기
    private static final Pattern SSN_PATTERN =
            Pattern.compile("(\\d{6})-?(\\d{7})");
    private static final Pattern ACCOUNT_NO_PATTERN =
            Pattern.compile("\\b(\\d{3,6}[-]?)(\\d{2,6}[-]?)(\\d+)\\b");
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

    /** 텍스트에서 주민번호 패턴을 찾아 마스킹한다. */
    public static String maskSensitiveText(String text) {
        if (text == null) return null;
        return SSN_PATTERN.matcher(text).replaceAll(m -> m.group(1) + "-*******");
    }
}
