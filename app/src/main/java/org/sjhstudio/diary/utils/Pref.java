package org.sjhstudio.diary.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Pref {

    // Pref1
    public static final String PREF_NAME = "pref";
    public static final String ASK_LOCATION = "ask_location";   // GPS 알림
    public static final String FONT_KEY = "font_key";
    public static final String MODE_KEY = "mode_key";
    public static final String PASSWORD = "password";
    public static final String FONT_SIZE = "font_size"; // 폰트크기
    public static final String PERMISSION_GUIDE = "permission_guide";   // 권한안내(최초 1회만)
    public static final String USE_PW = "use_pw";   // 비밀번호 사용
    public static final String FINGER_PRINT = "finger_print";   // 지문 사용
    public static final String SKIP_NOTE = "skip_note"; // 일기요약(3줄)
    public static final String REMOVED_FONT_NOTICE = "removed_font_notice"; // 뺀 글꼴을 바꿨다는 안내(1회)

    // Pref2
    public static final String PREF_NAME_2 = "pref2";   // 알림관련
    public static final String USE_ALARM = "is_alarm_key";   // 알림기능 사용여부
    public static final String ALARM_HOUR = "hour_key";   // 알림 시간
    public static final String ALARM_MINUTE = "minute_key";   // 알림 분

    // GPS 요청여부
    public static Boolean getPAskLocation(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getBoolean(ASK_LOCATION, true);

        return true;
    }
    public static void setPAskLocation(Context context, boolean flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putBoolean(ASK_LOCATION, flag).apply();
    }

    // 폰트
    public static int getPFontKey(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) {
            int key = pref.getInt(FONT_KEY, -1);
            int replacedKey = replaceRemovedFontKey(key);

            if (replacedKey != key) {
                pref.edit().putInt(FONT_KEY, replacedKey).putBoolean(REMOVED_FONT_NOTICE, true).commit();
            }

            return replacedKey;
        }

        return 0;
    }

    // 1.7.5에서 뺀 777 글꼴(5 미니콩다방체, 6 꼬마나비체)을 가장 비슷한 글꼴로 바꾼다.
    // 새 앱의 기록 이전 대응표와 같다.
    private static int replaceRemovedFontKey(int key) {
        if (key == 5) return 1;     // 국립공원 꼬미체
        if (key == 6) return 12;    // 코트라 희망체
        return key;
    }

    // 뺀 글꼴 안내를 아직 안 보여 줬으면 true를 돌려주고 본 것으로 남긴다.
    public static boolean consumePRemovedFontNotice(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null && pref.getBoolean(REMOVED_FONT_NOTICE, false)) {
            pref.edit().putBoolean(REMOVED_FONT_NOTICE, false).apply();
            return true;
        }

        return false;
    }
    public static void setPFontKey(Context context, int key) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putInt(FONT_KEY, key).commit();
    }

    // 테마
    public static int getPModeKey(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getInt(MODE_KEY, 0);

        return 0;
    }
    public static void setPModeKey(Context context, int key) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putInt(MODE_KEY, key).commit();
    }

    // 폰트크기
    public static int getPFontSize(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getInt(FONT_SIZE, 2);  // 2가 보통 사이즈

        return 0;
    }
    public static void setPFontSize(Context context, int size) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putInt(FONT_SIZE, size).apply();
    }

    // 권한안내
    public static Boolean getPPermissionGuide(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getBoolean(PERMISSION_GUIDE, false);

        return false;
    }
    public static void setPPermissionGuide(Context context, Boolean flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putBoolean(PERMISSION_GUIDE, flag).apply();
    }

    // 비밀번호
    public static String getPPassword(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getString(PASSWORD, "");

        return "";
    }
    public static void setPPassword(Context context, String pw) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putString(PASSWORD, pw).apply();
    }
    public static void removePPassword(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().remove(PASSWORD).apply();
    }

    // 비밀번호 사용
    public static Boolean getPUsePw(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getBoolean(USE_PW, false);

        return false;
    }
    public static void setPUsePw(Context context, Boolean flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putBoolean(USE_PW, flag).apply();
    }

    // 지문사용
    public static Boolean getPFingerPrint(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getBoolean(FINGER_PRINT, false);

        return false;
    }
    public static void setPFingerPrint(Context context, Boolean flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putBoolean(FINGER_PRINT, flag).apply();
    }

    // 일기 3줄 보기
    public static Boolean getPSkipNote(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) return pref.getBoolean(SKIP_NOTE, true);

        return true;
    }
    public static void setPSkipNote(Context context, Boolean flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putBoolean(SKIP_NOTE, flag).apply();
    }

    /**
     * Pref1
     * ============================================================================================
     * Pref2
     */

    // 알림 사용여부
    public static Boolean getPUseAlarm(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME_2, Context.MODE_PRIVATE);
        if(pref != null) return pref.getBoolean(USE_ALARM, false);

        return false;
    }
    public static void setPUseAlarm(Context context, Boolean flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME_2, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putBoolean(USE_ALARM, flag).apply();
    }

    // 알림 시(hour)
    public static int getPAlarmHour(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME_2, Context.MODE_PRIVATE);
        if(pref != null) return pref.getInt(ALARM_HOUR, 22);

        return 22;
    }
    public static void setPAlarmHour(Context context, int flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME_2, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putInt(ALARM_HOUR, flag).apply();
    }

    // 알림 분(minute)
    public static int getPAlarmMinute(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME_2, Context.MODE_PRIVATE);
        if(pref != null) return pref.getInt(ALARM_MINUTE, 0);

        return 0;
    }
    public static void setPAlarmMinute(Context context, int flag) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME_2, Context.MODE_PRIVATE);
        if(pref != null) pref.edit().putInt(ALARM_MINUTE, flag).apply();
    }

}
