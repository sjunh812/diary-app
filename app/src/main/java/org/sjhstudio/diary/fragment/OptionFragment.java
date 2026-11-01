package org.sjhstudio.diary.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.sjhstudio.diary.AlarmActivity;
import org.sjhstudio.diary.BackupActivity;
import org.sjhstudio.diary.DarkModeActivity;
import org.sjhstudio.diary.FontActivity;
import org.sjhstudio.diary.MainActivity;
import org.sjhstudio.diary.PasswordSettingsActivity;
import org.sjhstudio.diary.R;
import org.sjhstudio.diary.SettingsActivity;
import org.sjhstudio.diary.helper.AppHelper;
import org.sjhstudio.diary.note.NoteDatabaseCallback;
import org.sjhstudio.diary.utils.Pref;

public class OptionFragment extends Fragment {

    private NoteDatabaseCallback callback;  // DB 접근을 위한 callback

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NoteDatabaseCallback) callback = (NoteDatabaseCallback) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (callback != null) callback = null;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View rootView = inflater.inflate(
                R.layout.fragment_option,
                container,
                false
        );

        setTitleTextView(rootView);
        setCountTextView(rootView);
        setCurFontText(rootView);

        Button donateButton = rootView.findViewById(R.id.btn_donate);
        donateButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://qr.kakaopay.com/FciEjScDL"));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(intent);
            } catch (Exception e) {

            }
        });

        /* 폰트 설정 */
        RelativeLayout fontLayout = rootView.findViewById(R.id.fontLayout);
        fontLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FontActivity.class);
            ((MainActivity) requireActivity()).fontChangeResult.launch(intent);
        });

        /* 알림 설정 */
        RelativeLayout noticeLayout = rootView.findViewById(R.id.noticeLayout);
        noticeLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AlarmActivity.class);
            startActivity(intent);
        });

        /* 다크모드 설정 */
        RelativeLayout darkmodeLayout = rootView.findViewById(R.id.darkmodeLayout);
        darkmodeLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DarkModeActivity.class);
            startActivity(intent);
        });

        /* 기타설정 */
        RelativeLayout settingsLayout = rootView.findViewById(R.id.settings_layout);
        settingsLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        /* 잠금설정 */
        RelativeLayout passwordLayout = rootView.findViewById(R.id.passwordlayout);
        passwordLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PasswordSettingsActivity.class);
            requireActivity().startActivity(intent);
        });

        /* 백업 및 복원하기 */
        RelativeLayout backupLayout = rootView.findViewById(R.id.backupLayout);
        backupLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BackupActivity.class);
            startActivity(intent);
        });

        /* 스토어 리뷰 */
        RelativeLayout reviewLayout = rootView.findViewById(R.id.reviewLayout);
        reviewLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + requireContext().getPackageName()));
            startActivity(intent);
        });

        /* 피드백 보내기 */
        RelativeLayout ideaLayout = rootView.findViewById(R.id.ideaLayout);
        ideaLayout.setOnClickListener(v -> {
            AppHelper helper = new AppHelper(getContext());
            String[] email = {"sjunh812@naver.com"};
            String appVersion = helper.getVersionName();
            String osVersion = helper.getOsName();
            String modelName = helper.getModelName();
            String contents = "안녕하세요!\n" +
                    "소중한 의견을 보내주셔서 감사합니다 :)\n" +
                    "신중하게 검토 후 답변드리겠습니다.\n" +
                    "----------------------------\n" +
                    "앱 버전 : " + appVersion +
                    "\n기기명 : " + modelName +
                    "\n안드로이드 OS : " + osVersion +
                    "\n----------------------------\n";

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("plain/Text");
            intent.putExtra(Intent.EXTRA_EMAIL, email);
            intent.putExtra(Intent.EXTRA_SUBJECT, "[" + getString(R.string.app_name) + "] " + getString(R.string.report));
            intent.putExtra(Intent.EXTRA_TEXT, contents);
            intent.setType("message/rfc822");
            startActivity(intent);
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setTitleTextView(View rootView) {
        Animation translateRightAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.translate_right_animation);
        translateRightAnim.setDuration(350);
        TextView titleTextView = rootView.findViewById(R.id.titleTextView);
        titleTextView.startAnimation(translateRightAnim);
    }

    @SuppressLint("SetTextI18n")
    private void setCountTextView(View rootView) {
        TextView allCountTextView = rootView.findViewById(R.id.allCountTextView);
        TextView starCountTextView = rootView.findViewById(R.id.starCountTextView);
        int allCount = callback.selectAllCount();   // 작성한 일기 총 개수
        int starCount = callback.selectStarCount(); // 즐겨찾기 총 개수

        allCountTextView.setText(allCount + "개");
        starCountTextView.setText(starCount + "개");
    }

    @SuppressLint("SetTextI18n")
    private void setCurFontText(View rootView) {
        // 현재 선택한 폰트종류
        int curFontIndex = Pref.getPFontKey(requireContext());
        TextView curFontTextView = rootView.findViewById(R.id.curFontTextView);

        switch (curFontIndex) {
            case 100:
                curFontTextView.setText("시스템 서체");
                break;
            case 0:
                curFontTextView.setText("프리텐다드");
                break;
            case 1:
                curFontTextView.setText("국립공원 꼬미체");
                break;
            case 2:
                curFontTextView.setText("교보 손글씨체");
                break;
            case 3:
                curFontTextView.setText("점꼴체");
                break;
            case 4:
                curFontTextView.setText("넥슨 배찌체");
                break;
            case 7:
                curFontTextView.setText("심경하체");
                break;
            case 8:
                curFontTextView.setText("강원교육모두체");
                break;
            case 9:
                curFontTextView.setText("쿠키런체");
                break;
            case 10:
                curFontTextView.setText("온글잎 만두몽키체");
                break;
            case 11:
                curFontTextView.setText("온글잎 윤우체");
                break;
            case 12:
                curFontTextView.setText("코트라 희망체");
                break;
            case 13:
                curFontTextView.setText("ACC 어린이 마음고운체");
                break;
            default:
                curFontTextView.setText("프리텐다드");
                break;
        }
    }

}
