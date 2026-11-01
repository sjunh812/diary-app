package org.sjhstudio.diary.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.MPPointF;

import org.sjhstudio.diary.R;
import org.sjhstudio.diary.custom.MyRadioButton;
import org.sjhstudio.diary.note.NoteDatabaseCallback;
import org.sjhstudio.diary.utils.Pref;
import org.sjhstudio.diary.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class GraphFragment extends Fragment {

    private MyRadioButton allButton;
    private MyRadioButton yearButton;
    private MyRadioButton monthButton;
    private TextView moodTitleTextView;
    private TextView moodTotalCountTextView;
    private TextView angryCount;
    private TextView coolCount;
    private TextView cryingCount;
    private TextView illCount;
    private TextView laughCount;
    private TextView mehCount;
    private TextView sadCount;
    private TextView smileCount;
    private TextView yawnCount;
    private ImageView angryImageView;
    private ImageView coolImageView;
    private ImageView cryingImageView;
    private ImageView illImageView;
    private ImageView laughImageView;
    private ImageView mehImageView;
    private ImageView sadImageView;
    private ImageView smileImageView;
    private ImageView yawnImageView;
    private ImageView crown;
    private ImageView crown2;
    private ImageView crown3;
    private ImageView crown4;
    private ImageView crown5;
    private ImageView crown6;
    private ImageView crown7;
    private ImageView crown8;
    private ImageView crown9;
    private LinearLayout textView;
    private TextView describeTextView;
    private TextView moodTextView;
    private FrameLayout backgroundGraphLayout;

    private PieChart chart1;
    private NoteDatabaseCallback callback;

    int[] moodIconRes = {
            R.drawable.ic_mood_angry_small, R.drawable.ic_mood_cool_small, R.drawable.ic_mood_crying_small,
            R.drawable.ic_mood_ill_small, R.drawable.ic_mood_laugh_small, R.drawable.ic_mood_meh_small,
            R.drawable.ic_mood_sad_small, R.drawable.ic_mood_smile_small, R.drawable.ic_mood_yawn_small
    };
    private final ArrayList<Integer> colors = new ArrayList<>();      // 색깔 정보를 담은 배열
    private int curFontIndex = -1;                              // 현재 사용중인 폰트 종류
    private int selectRadioIndex = 0;                           // 전체: 0, 올해: 1, 이번달: 2 (default: 전체)
    private int maxMoodIndex = -1;                              // 제일 많은 기분 종류
    private int maxCount = -1;                                  // 제일 많은 기분의 개수

    private Animation translateRightAnim;
    private Animation moodAnimation;
    private Animation crownAnimation;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NoteDatabaseCallback) {
            callback = (NoteDatabaseCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        clearResource();
    }

    private void clearResource() {
        if (callback != null) callback = null;
        clearAnimation();
    }

    private void clearAnimation() {
        crown.clearAnimation();
        crown2.clearAnimation();
        crown3.clearAnimation();
        crown4.clearAnimation();
        crown5.clearAnimation();
        crown6.clearAnimation();
        crown7.clearAnimation();
        crown8.clearAnimation();
        crown9.clearAnimation();
        angryImageView.clearAnimation();
        coolImageView.clearAnimation();
        cryingImageView.clearAnimation();
        illImageView.clearAnimation();
        laughImageView.clearAnimation();
        mehImageView.clearAnimation();
        sadImageView.clearAnimation();
        smileImageView.clearAnimation();
        yawnImageView.clearAnimation();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_graph,
                container,
                false
        );

        curFontIndex = Pref.getPFontKey(requireContext());  // 폰트 정보
        initAnimation();                                    // 애니메이션 초기화
        initChartUI(rootView);                              // 차트 초기화
        initUI(rootView);                                   // UI 초기화
        initRadioButton(rootView);                          // 라디오 버튼 초기화

        return rootView;
    }

    private void initAnimation() {
        translateRightAnim = AnimationUtils.loadAnimation(getContext(), R.anim.translate_right_animation);
        moodAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.mood_icon_animation);
        crownAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.crown_icon_animation);
        translateRightAnim.setDuration(350);    // 타이틀 애니메이션 주기 -> 0.35초
    }

    private void initChartUI(View rootView) {
        chart1 = rootView.findViewById(R.id.chart1);
        chart1.setUsePercentValues(true);
        chart1.getDescription().setEnabled(false);  // 추가 설명 x
        chart1.setDrawHoleEnabled(false);   // 중간 원 x
        chart1.setExtraOffsets(5, 10, 5, 10);
        chart1.setHighlightPerTapEnabled(false);    // 선택 시, 확대효과 x

//        chart1.setTransparentCircleColor(getResources().getColor(R.color.white)); // 중간원과 바깥원 사이의 얇은 투명원의 색상 결정
//        chart1.setTransparentCircleAlpha(110);    // 중간원과 바깥원 사이의 얇은 투명원의 알파 값 결정
//        chart1.setTransparentCircleRadius(66f);   // 중간원과 바깥원 사이의 얇은 투명원의 반지름
//        chart1.setHoleRadius(58f);    // 중간원의 반지름
//        chart1.setHoleColor(getResources().getColor(R.color.azure2));
//        chart1.setDrawCenterText(true);

        Legend legend1 = chart1.getLegend();    // 그래프의 구성요소들을 추가로 명시하는지 여부
        legend1.setEnabled(false);  // 추가 구성요소 명시 false
        chart1.setEntryLabelColor(Color.WHITE); // entry label 색상
//        chart1.setEntryLabelTextSize(12f);    // entry label 크기
        chart1.animateXY(1200, 1200);   // 차트 애니메이션
    }

    private void initUI(View rootView) {
        TextView titleTextView = rootView.findViewById(R.id.titleTextView);
        titleTextView.startAnimation(translateRightAnim);

        moodTotalCountTextView = rootView.findViewById(R.id.moodTotalCountTextView);
        moodTitleTextView = rootView.findViewById(R.id.moodTitleTextView);
        angryCount = rootView.findViewById(R.id.angryCount);
        coolCount = rootView.findViewById(R.id.coolCount);
        cryingCount = rootView.findViewById(R.id.cryingCount);
        illCount = rootView.findViewById(R.id.illCount);
        laughCount = rootView.findViewById(R.id.laughCount);
        mehCount = rootView.findViewById(R.id.mehCount);
        sadCount = rootView.findViewById(R.id.sadCount);
        smileCount = rootView.findViewById(R.id.smileCount);
        yawnCount = rootView.findViewById(R.id.yawnCount);
        crown = rootView.findViewById(R.id.crown);
        crown2 = rootView.findViewById(R.id.crown2);
        crown3 = rootView.findViewById(R.id.crown3);
        crown4 = rootView.findViewById(R.id.crown4);
        crown5 = rootView.findViewById(R.id.crown5);
        crown6 = rootView.findViewById(R.id.crown6);
        crown7 = rootView.findViewById(R.id.crown7);
        crown8 = rootView.findViewById(R.id.crown8);
        crown9 = rootView.findViewById(R.id.crown9);
        angryImageView = rootView.findViewById(R.id.angryImageView);
        coolImageView = rootView.findViewById(R.id.coolImageView);
        cryingImageView = rootView.findViewById(R.id.cryingImageView);
        illImageView = rootView.findViewById(R.id.illImageView);
        laughImageView = rootView.findViewById(R.id.laughImageView);
        mehImageView = rootView.findViewById(R.id.mehImageView);
        sadImageView = rootView.findViewById(R.id.sadImageView);
        smileImageView = rootView.findViewById(R.id.smileImageView);
        yawnImageView = rootView.findViewById(R.id.yawnImageView);
        textView = rootView.findViewById(R.id.textView);
        describeTextView = rootView.findViewById(R.id.describeTextView);
        moodTextView = rootView.findViewById(R.id.moodTextView);
        backgroundGraphLayout = rootView.findViewById(R.id.background_graph);
    }

    @SuppressLint("SetTextI18n")
    private void initRadioButton(View rootView) {
        allButton = rootView.findViewById(R.id.allButton);
        yearButton = rootView.findViewById(R.id.yearButton);
        monthButton = rootView.findViewById(R.id.monthButton);
        RadioGroup radioGroup = rootView.findViewById(R.id.radioGroup);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            HashMap<Integer, Integer> hashMap = null;

            if (checkedId == R.id.allButton) {
                moodTitleTextView.setText("전체");
                selectRadioIndex = 0;
                hashMap = callback.selectMoodCount(true, false, false);
                describeTextView.setText("전체 통계 중 제일 많은 기분은 ");
            } else if (checkedId == R.id.yearButton) {
                moodTitleTextView.setText(Utils.INSTANCE.getCurrentYear() + "년");
                selectRadioIndex = 1;
                hashMap = callback.selectMoodCount(false, true, false);
                describeTextView.setText("올해 통계 중 제일 많은 기분은 ");
            } else if (checkedId == R.id.monthButton) {
                moodTitleTextView.setText(Utils.INSTANCE.getCurrentMonth() + "월");
                selectRadioIndex = 2;
                hashMap = callback.selectMoodCount(false, false, true);
                describeTextView.setText("이번달 통계 중 제일 많은 기분은 ");
            }

            chart1.setCenterTextTypeface(getCurTypeFace());
            chart1.setCenterTextSize(17f);
            setData1(hashMap);
        });

        setSelectedRadioButton();       // 선택된 라디오버튼 index 에 따라 라디오버튼 Checked 활성화
    }

    private void setSelectedRadioButton() {
        switch (selectRadioIndex) {
            case 0:
                allButton.setChecked(true);
                break;
            case 1:
                yearButton.setChecked(true);
                break;
            case 2:
                monthButton.setChecked(true);
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void setData1(HashMap<Integer, Integer> hashMap) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        int totalCount = 0; // 총 기분 수 (전체, 올해, 이번달마다 달라지는 값이므로 호출마다 초기화)
        maxMoodIndex = -1;  // 제일 많은 기분 종류
        maxCount = -1;      // 제일 많은 기분의 개수
        colors.clear();     // 파이차트를 구성할 색깔배열 clear (전체, 올해, 이번달마다 달라지는 값이므로 clear 필요)

        for (int i = 0; i < 9; i++) {
            int count = 0;

            if (hashMap.containsKey(i)) {
                count = hashMap.get(i);
                setMoodCount(i, count);
                totalCount += count;
                addColor(i);                // 기분 종류에 맞게 색깔 설정
                entries.add(new PieEntry(
                        /* value */ count,
                        /* label */ "",
                        /* icon */ ContextCompat.getDrawable(requireContext(), moodIconRes[i]))
                );
            } else {
                setMoodCount(i, count);     // 개수 0
            }
        }

        moodTotalCountTextView.setText("(총 " + totalCount + "건)");        // 총 기분 개수
        setCrownImage();                                        // 제일 많은 개수를 가진 기분에 왕관이미지를 추가

        PieDataSet dataSet = new PieDataSet(entries, "기분별 비율");
        dataSet.setDrawIcons(true);                             // 아이콘 표시 여부
        dataSet.setSliceSpace(10f);                             // 그래프 간격
        dataSet.setIconsOffset(new MPPointF(0, 55));      // 아이콘 offset
//        dataSet.setSelectionShift(5f);                       // 특정부분 선택시 확대효과 크기
        dataSet.setColors(colors);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value) + "%";
            }
        });

        PieData data = new PieData(dataSet);
        data.setValueTextSize(15f);                         // 그래프 내 text 크기
        data.setValueTextColor(Color.WHITE);                // 그래프 내 text 색상
        data.setValueTypeface(getCurTypeFace());            // 그래프 내 text 폰트

        chart1.setData(data);
        chart1.invalidate();

        if (totalCount == 0) backgroundGraphLayout.setVisibility(View.GONE);
        else backgroundGraphLayout.setVisibility(View.VISIBLE);
    }

    private void setMoodCount(int moodIndex, int count) {
        if (maxCount < count) {
            maxCount = count;
            maxMoodIndex = moodIndex;
        } else if (maxCount == count) {      // 중복 값이 있는 max 라면 예외처리
            maxMoodIndex = -1;
        }

        switch (moodIndex) {
            case 0:
                angryCount.setText(String.valueOf(count));
                break;
            case 1:
                coolCount.setText(String.valueOf(count));
                break;
            case 2:
                cryingCount.setText(String.valueOf(count));
                break;
            case 3:
                illCount.setText(String.valueOf(count));
                break;
            case 4:
                laughCount.setText(String.valueOf(count));
                break;
            case 5:
                mehCount.setText(String.valueOf(count));
                break;
            case 6:
                sadCount.setText(String.valueOf(count));
                break;
            case 7:
                smileCount.setText(String.valueOf(count));
                break;
            case 8:
                yawnCount.setText(String.valueOf(count));
                break;
        }
    }

    private void setCrownImage() {
        clearAnimation();
        crown.setVisibility(View.GONE);
        crown2.setVisibility(View.GONE);
        crown3.setVisibility(View.GONE);
        crown4.setVisibility(View.GONE);
        crown5.setVisibility(View.GONE);
        crown6.setVisibility(View.GONE);
        crown7.setVisibility(View.GONE);
        crown8.setVisibility(View.GONE);
        crown9.setVisibility(View.GONE);

        if (maxMoodIndex == -1) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);

            switch (maxMoodIndex) {
                case 0:
                    crown.setVisibility(View.VISIBLE);
                    angryImageView.startAnimation(moodAnimation);
                    crown.startAnimation(crownAnimation);

                    moodTextView.setText("'화남'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                    break;
                case 1:
                    crown2.setVisibility(View.VISIBLE);
                    coolImageView.startAnimation(moodAnimation);
                    crown2.startAnimation(crownAnimation);

                    moodTextView.setText("'쿨'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
                    break;
                case 2:
                    crown3.setVisibility(View.VISIBLE);
                    cryingImageView.startAnimation(moodAnimation);
                    crown3.startAnimation(crownAnimation);

                    moodTextView.setText("'슬픔'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.skyblue));
                    break;
                case 3:
                    crown4.setVisibility(View.VISIBLE);
                    illImageView.startAnimation(moodAnimation);
                    crown4.startAnimation(crownAnimation);

                    moodTextView.setText("'아픔'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.lightgreen));
                    break;
                case 4:
                    crown5.setVisibility(View.VISIBLE);
                    laughImageView.startAnimation(moodAnimation);
                    crown5.startAnimation(crownAnimation);

                    moodTextView.setText("'웃음'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow));
                    break;
                case 5:
                    crown6.setVisibility(View.VISIBLE);
                    mehImageView.startAnimation(moodAnimation);
                    crown6.startAnimation(crownAnimation);

                    moodTextView.setText("'보통'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
                    break;
                case 6:
                    crown7.setVisibility(View.VISIBLE);
                    sadImageView.startAnimation(moodAnimation);
                    crown7.startAnimation(crownAnimation);

                    moodTextView.setText("'나쁨'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
                case 7:
                    crown8.setVisibility(View.VISIBLE);
                    smileImageView.startAnimation(moodAnimation);
                    crown8.startAnimation(crownAnimation);

                    moodTextView.setText("'좋음'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange));
                    break;
                case 8:
                    crown9.setVisibility(View.VISIBLE);
                    yawnImageView.startAnimation(moodAnimation);
                    crown9.startAnimation(crownAnimation);

                    moodTextView.setText("'피곤'");
                    moodTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.pink));
                    break;
            }
        }
    }

    private void addColor(int moodIndex) {
        switch (moodIndex) {
            case 0:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_red));
                break;
            case 1:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_blue));
                break;
            case 2:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_skyblue));
                break;
            case 3:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_green));
                break;
            case 4:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_yellow));
                break;
            case 5:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_gray));
                break;
            case 6:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_black));
                break;
            case 7:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_orange));
                break;
            case 8:
                colors.add(ContextCompat.getColor(requireContext(), R.color.pastel_pink));
                break;
        }
    }

    private Typeface getCurTypeFace() {
        return switch (curFontIndex) {
            case 100 -> Typeface.SANS_SERIF;
            case 0 -> Typeface.createFromAsset(requireContext().getAssets(), "main_font.ttf");
            case 1 -> Typeface.createFromAsset(requireContext().getAssets(), "font.otf");
            case 2 -> Typeface.createFromAsset(requireContext().getAssets(), "font2.ttf");
            case 3 -> Typeface.createFromAsset(requireContext().getAssets(), "font3.ttf");
            case 4 -> Typeface.createFromAsset(requireContext().getAssets(), "font4.ttf");
            case 7 -> Typeface.createFromAsset(requireContext().getAssets(), "font7.ttf");
            case 8 -> Typeface.createFromAsset(requireContext().getAssets(), "font8.ttf");
            case 9 -> Typeface.createFromAsset(requireContext().getAssets(), "font9.ttf");
            case 10 -> Typeface.createFromAsset(requireContext().getAssets(), "font10.ttf");
            case 11 -> Typeface.createFromAsset(requireContext().getAssets(), "font11.ttf");
            case 12 -> Typeface.createFromAsset(requireContext().getAssets(), "font12.ttf");
            case 13 -> Typeface.createFromAsset(requireContext().getAssets(), "font13.ttf");
            default -> Typeface.createFromAsset(requireContext().getAssets(), "main_font.ttf");
        };
    }

//    private Drawable resizeDrawable(int res) {
//        BitmapDrawable drawable = (BitmapDrawable)getResources().getDrawable(res);
//        Bitmap bitmap = drawable.getBitmap();
//        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 55, 55, true);
//        Drawable newDrawable = new BitmapDrawable(context.getResources(), newBitmap);
//
//        return newDrawable;
//    }
}
