package org.sjhstudio.diary;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import org.sjhstudio.diary.extensions.ViewExtensionKt;
import org.sjhstudio.diary.helper.MyTheme;
import org.sjhstudio.diary.utils.BaseActivity;
import org.sjhstudio.diary.utils.Pref;

import java.util.Objects;

public class FontActivity extends BaseActivity {

    private static final String TAG = "FontActivity";

    private int curFontIndex = 0;
    private int curFontSizeIndex = 1;
    private int selectedFontIndex = -1;
    private int selectedFontSizeIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font);

        ViewExtensionKt.enableSystemBarPadding(findViewById(R.id.root));

        curFontIndex = Pref.getPFontKey(this);
        curFontSizeIndex = Pref.getPFontSize(this);

        init();
        setPrefs(); // 폰트종류 및 크기에 대한 prefs
    }

    private void init() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("폰트설정");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        initFontRadioBtn();
        initFontSizeRadioBtn();
        initBottom();
    }

    private void initFontRadioBtn() {
        ((RadioGroup) findViewById(R.id.font_radio_group)).setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.systemFontButton:
                    selectedFontIndex = 100;
                    break;
                case R.id.btn_font_main:
                    selectedFontIndex = -1;
                    break;
                case R.id.btn_font_1:
                    selectedFontIndex = 1;
                    break;
                case R.id.btn_font_2:
                    selectedFontIndex = 2;
                    break;
                case R.id.btn_font_3:
                    selectedFontIndex = 3;
                    break;
                case R.id.btn_font_4:
                    selectedFontIndex = 4;
                    break;
                case R.id.btn_font_7:
                    selectedFontIndex = 7;
                    break;
                case R.id.btn_font_8:
                    selectedFontIndex = 8;
                    break;
                case R.id.btn_font_9:
                    selectedFontIndex = 9;
                    break;
                case R.id.btn_font_10:
                    selectedFontIndex = 10;
                    break;
                case R.id.btn_font_11:
                    selectedFontIndex = 11;
                    break;
                case R.id.btn_font_12:
                    selectedFontIndex = 12;
                    break;
                case R.id.btn_font_13:
                    selectedFontIndex = 13;
                    break;
            }
        });
    }

    private void initFontSizeRadioBtn() {
        ((RadioGroup) findViewById(R.id.font_size_radio_group)).setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.ss_size_radio_btn:
                    selectedFontSizeIndex = 0;
                    break;
                case R.id.s_size_radio_btn:
                    selectedFontSizeIndex = 1;
                    break;
                case R.id.m_size_radio_btn:
                    selectedFontSizeIndex = 2;
                    break;
                case R.id.l_size_radio_btn:
                    selectedFontSizeIndex = 3;
                    break;
                case R.id.ll_size_radio_btn:
                    selectedFontSizeIndex = 4;
                    break;
            }
        });
    }

    private void initBottom() {
        ((Button) findViewById(R.id.okButton)).setOnClickListener(v -> {
            MyTheme.applyTheme(getApplicationContext(), selectedFontIndex, selectedFontSizeIndex);
            setResult(RESULT_OK, getIntent());
            finish();
        });

        ((Button) findViewById(R.id.cancelButton)).setOnClickListener(v -> {
            finish();
        });
    }

    private void setPrefs() {
        switch (curFontIndex) {
            case 100:
                ((RadioButton) findViewById(R.id.systemFontButton)).setChecked(true);
                break;
            case -1:
                ((RadioButton) findViewById(R.id.btn_font_main)).setChecked(true);
                break;
            case 1:
                ((RadioButton) findViewById(R.id.btn_font_1)).setChecked(true);
                break;
            case 2:
                ((RadioButton) findViewById(R.id.btn_font_2)).setChecked(true);
                break;
            case 3:
                ((RadioButton) findViewById(R.id.btn_font_3)).setChecked(true);
                break;
            case 4:
                ((RadioButton) findViewById(R.id.btn_font_4)).setChecked(true);
                break;
            case 7:
                ((RadioButton) findViewById(R.id.btn_font_7)).setChecked(true);
                break;
            case 8:
                ((RadioButton) findViewById(R.id.btn_font_8)).setChecked(true);
                break;
            case 9:
                ((RadioButton) findViewById(R.id.btn_font_9)).setChecked(true);
                break;
            case 10:
                ((RadioButton) findViewById(R.id.btn_font_10)).setChecked(true);
                break;
            case 11:
                ((RadioButton) findViewById(R.id.btn_font_11)).setChecked(true);
                break;
            case 12:
                ((RadioButton) findViewById(R.id.btn_font_12)).setChecked(true);
                break;
            case 13:
                ((RadioButton) findViewById(R.id.btn_font_13)).setChecked(true);
                break;
        }

        switch (curFontSizeIndex) {
            case 0:
                ((RadioButton) findViewById(R.id.ss_size_radio_btn)).setChecked(true);
                break;
            case 1:
                ((RadioButton) findViewById(R.id.s_size_radio_btn)).setChecked(true);
                break;
            case 2:
                ((RadioButton) findViewById(R.id.m_size_radio_btn)).setChecked(true);
                break;
            case 3:
                ((RadioButton) findViewById(R.id.l_size_radio_btn)).setChecked(true);
                break;
            case 4:
                ((RadioButton) findViewById(R.id.ll_size_radio_btn)).setChecked(true);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {   // 툴바 왼쪽 돌아가기 버튼 선택 시
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}