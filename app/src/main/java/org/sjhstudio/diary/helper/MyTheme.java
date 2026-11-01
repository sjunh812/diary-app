package org.sjhstudio.diary.helper;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import org.sjhstudio.diary.R;
import org.sjhstudio.diary.utils.Pref;

public class MyTheme {

    public static void applyTheme(@NonNull Context context) {
        int font;
        int fontSize;
        int mode;

        font = Pref.getPFontKey(context);
        mode = Pref.getPModeKey(context);
        fontSize = Pref.getPFontSize(context);
        applyDarkMode(context, mode);
        applyTheme(context, font, fontSize);
    }

    public static void applyDarkMode(Context context, int modeIndex) {
        Pref.setPModeKey(context, modeIndex);

        if (modeIndex == 0) {   // 시스템 설정에 따른 테마모드
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            }
        } else if (modeIndex == 1) {    // 라이트 모드
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (modeIndex == 2) {    // 다크 모드
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static void applyTheme(@NonNull Context context, int font, int fontSize) {
        Pref.setPFontKey(context, font);
        Pref.setPFontSize(context, fontSize);

        switch (font) {
            case 100:
                context.setTheme(R.style.Theme_BasicDiaryProject);
                break;
            case -1:
                context.setTheme(R.style.Theme_MainDiaryProject);
                break;
            case 1:
                context.setTheme(R.style.Theme_DiaryProject);
                break;
            case 2:
                context.setTheme(R.style.Theme_DiaryProject_2);
                break;
            case 3:
                context.setTheme(R.style.Theme_DiaryProject_3);
                break;
            case 4:
                context.setTheme(R.style.Theme_DiaryProject_4);
                break;
            case 7:
                context.setTheme(R.style.Theme_DiaryProject_7);
                break;
            case 8:
                context.setTheme(R.style.Theme_DiaryProject_8);
                break;
            case 9:
                context.setTheme(R.style.Theme_DiaryProject_9);
                break;
            case 10:
                context.setTheme(R.style.Theme_DiaryProject_10);
                break;
            case 11:
                context.setTheme(R.style.Theme_DiaryProject_11);
                break;
            case 12:
                context.setTheme(R.style.Theme_DiaryProject_12);
                break;
            case 13:
                context.setTheme(R.style.Theme_DiaryProject_13);
                break;
            default:
                context.setTheme(R.style.Theme_MainDiaryProject);
                break;
        }
    }
}
