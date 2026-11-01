package org.sjhstudio.diary;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;
import com.stanfy.gsonxml.XmlParserCreator;

import org.sjhstudio.diary.extensions.ViewExtensionKt;
import org.sjhstudio.diary.fragment.CalendarFragment;
import org.sjhstudio.diary.fragment.GraphFragment;
import org.sjhstudio.diary.fragment.ListFragment;
import org.sjhstudio.diary.fragment.OptionFragment;
import org.sjhstudio.diary.fragment.WriteFragment;
import org.sjhstudio.diary.helper.KMAGrid;
import org.sjhstudio.diary.helper.MyApplication;
import org.sjhstudio.diary.helper.OnRequestListener;
import org.sjhstudio.diary.helper.OnTabItemSelectedListener;
import org.sjhstudio.diary.model.RGResults;
import org.sjhstudio.diary.model.ReverseGeocoder;
import org.sjhstudio.diary.note.Note;
import org.sjhstudio.diary.note.NoteDatabase;
import org.sjhstudio.diary.note.NoteDatabaseCallback;
import org.sjhstudio.diary.utils.ApiKey;
import org.sjhstudio.diary.utils.BaseActivity;
import org.sjhstudio.diary.utils.Constants;
import org.sjhstudio.diary.utils.DialogUtils;
import org.sjhstudio.diary.utils.PermissionUtils;
import org.sjhstudio.diary.utils.Pref;
import org.sjhstudio.diary.utils.Utils;
import org.sjhstudio.diary.weather.WeatherItem;
import org.sjhstudio.diary.weather.WeatherResult;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import kotlin.Unit;

public class MainActivity extends BaseActivity implements OnTabItemSelectedListener,
        AutoPermissionsListener, OnRequestListener, MyApplication.OnResponseListener, NoteDatabaseCallback {

    private static final String LOG = "MainActivity";

    private BottomNavigationView bottomNavigationView;  // 하단 탭
    private ListFragment listFragment;                  // 일기 목록
    private CalendarFragment calendarFragment;          // 기분 달력
    private WriteFragment writeFragment;                // 일기 작성
    private GraphFragment graphFragment;                // 일기 통계
    private OptionFragment optionFragment;              // 더보기

    private LocationManager locationManager;
    private GPSListener gpsListener;                    // 위치 정보를 가져오기 위해 필요한 리스너
    private LowVersionGPSListener lowVersionGPSListener;// 위치 정보를 가져오기 위해 필요한 리스너 (api 29 이전 버전)

    private NoteDatabase db;                            // 일기 목록을 담은 db
    private Location curLocation;                       // 현재 위치
    private Date curDate;                               // 현재 날짜
    private Note updateItem = null;                     // 일기 목록에서 수정할 Note 객체
    private int selectedTabIndex = 0;                   // 현재 선택되어있는 탭 번호 (onSaveInstanceState() 호출시 Bundle 객체로 저장)
    private Date calDate = null;
    private long backPressTime = 0;
    private OnBackPressedCallback backPressedCallback;
    private boolean isSelected2 = false;                // 일기작성 취소시, 다른메뉴를 선택했을 때 setSelectedTabItem() 호출을 위한 flag

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            System.out.println("xxx 화면 회전");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewExtensionKt.enableSystemBarPadding(findViewById(R.id.root));

        // Android 16(타깃 36)부터는 onBackPressed가 불리지 않는다.
        // 뒤로 가기는 콜백으로 받는다.
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        if (Pref.consumePRemovedFontNotice(this)) {
            Toast.makeText(this, R.string.notice_removed_font, Toast.LENGTH_LONG).show();
        }

        if (!Pref.getPPermissionGuide(this)) {
            DialogUtils.INSTANCE.showPermissionGuideDialog(this, () -> {
                AutoPermissions.Companion.loadAllPermissions(this, Constants.REQUEST_ALL_PERMISSIONS);
                Pref.setPPermissionGuide(this, true);
                return Unit.INSTANCE;
            });
        } else {
            AutoPermissions.Companion.loadAllPermissions(this, Constants.REQUEST_ALL_PERMISSIONS);
        }

        // DB
        db = new NoteDatabase(this);        // DB 객체 생성
        db.dbInit(NoteDatabase.DB_NAME);            // 지정된 이름의 일기 DB 생성
        db.createTable(NoteDatabase.NOTE_TABLE);    // Note 테이블 생성 (중복 제외)

        // fragment
        listFragment = new ListFragment();
        graphFragment = new GraphFragment();
        optionFragment = new OptionFragment();
        writeFragment = (WriteFragment) getSupportFragmentManager().findFragmentByTag("write_fragment");
        if (writeFragment == null) writeFragment = new WriteFragment();

        // location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new GPSListener();
        lowVersionGPSListener = new LowVersionGPSListener();

        // bottom navigation
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();

            if (!isSelected2 && selectedTabIndex == 2
                    && writeFragment != null
                    && !(writeFragment.isEmptyContent())
            ) {
                DialogUtils.INSTANCE.showStopWriteDialog(this, () -> {
                    if (writeFragment != null) writeFragment.deleteFilesCache(true);
                    writeFragment = new WriteFragment();

                    int position;

                    if (item.getItemId() == R.id.tab1) position = 0;
                    else if (item.getItemId() == R.id.tab2) position = 1;
                    else if (item.getItemId() == R.id.tab3) position = 2;
                    else if (item.getItemId() == R.id.tab4) position = 3;
                    else position = 4;

                    isSelected2 = true;
                    onTabSelected(position);

                    return Unit.INSTANCE;
                });
            } else {
                if (selectedTabIndex == 2) writeFragment = new WriteFragment();

                isSelected2 = false;
                return setSelectedTabItem(item.getItemId(), transaction);
            }

            return false;
        });

        // savedInstanceState(테마설정, 폰트설정시 이용)
        if (savedInstanceState == null) onTabSelected(0);
        else onTabSelected(savedInstanceState.getInt(Constants.SELECTED_TAB_INDEX));

        // 리시버등록(앱 제거시, 비밀번호삭제)
        registerRemovedReceiver();
    }

    private void registerRemovedReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addDataScheme(getPackageName());

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d("MyReceiver", "onReceive: " + intent);
                    Pref.removePPassword(MainActivity.this);
                }
            };

            registerReceiver(receiver, intentFilter);
        }
    }

    @Override
    public void setIsSelected(Boolean flag) {
        isSelected2 = flag;
    }

    /**
     * Current location and date
     *
     * @param date : date = null → today
     */
    public void getCurrentLocation(Date date) {
        if (date == null) curDate = new Date();                 // 현재 날짜정보
        else curDate = date;                                    // 사용자가 선택한 날짜정보

        String curYear = Utils.INSTANCE.getYearFormat().format(curDate);    // yyyy
        String curMonth = Utils.INSTANCE.getMonthFormat().format(curDate);  // MM
        String curDay = Utils.INSTANCE.getDayFormat().format(curDate);      // dd
        String _date = Utils.INSTANCE.getDateFormat().format(curDate);      // yyyy년 MM월 dd일

        if (writeFragment != null) {
            Log.d(LOG, _date);
            writeFragment.setDateTextView(_date);
            try {
                writeFragment.setCurDate(Integer.parseInt(curYear), Integer.parseInt(curMonth), Integer.parseInt(curDay));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (PermissionUtils.INSTANCE.checkLocationPermission(this)) {
                long minTime = 1000;    // 업데이트 주기 10초
                float minDistance = 0;  // 업데이트 거리간격 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            minTime,
                            minDistance,
                            gpsListener
                    );
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            minTime,
                            minDistance,
                            gpsListener
                    );
                } else {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            minTime,
                            minDistance,
                            lowVersionGPSListener
                    );
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            minTime,
                            minDistance,
                            lowVersionGPSListener
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callCurrentAddress() {
        // Use Naver reverse geocoding
        String url = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc";
        String get = "?coords="
                + curLocation.getLongitude() + "," + curLocation.getLatitude()
                + "&" + "output=json";
        url += get;

        Map<String, String> headers = new HashMap<>();
        headers.put("X-NCP-APIGW-API-KEY-ID", ApiKey.NAVER_MAP_CLIENT_ID);
        headers.put("X-NCP-APIGW-API-KEY", ApiKey.NAVER_MAP_CLIENT_KEY);

        MyApplication.requestWithHeader(
                Constants.REQUEST_REVERSE_GEOCODER,
                Request.Method.GET,
                url,
                headers,
                this
        );
    }

    public void callCurrentWeather() {
        // 현재 위치의 위도, 경도들 이용하여 기상청이 만든 격자포멧으로 변환
        Map<String, Double> map = KMAGrid.getKMAGrid(curLocation.getLatitude(), curLocation.getLongitude());
        double gridX = map.get("X");
        double gridY = map.get("Y");

        String url = "https://www.kma.go.kr/wid/queryDFS.jsp";
        url += "?gridx=" + Math.round(gridX);
        url += "&gridy=" + Math.round(gridY);

        Map<String, String> params = new HashMap<>();

        MyApplication.request(
                Constants.REQUEST_WEATHER_BY_GRID,
                Request.Method.GET,
                url,
                params,
                this
        );
    }

    @Override
    public void stopLocationService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                locationManager.removeUpdates(gpsListener);
            } else {
                locationManager.removeUpdates(lowVersionGPSListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getDateOnly(Date date) {
        if (date == null) curDate = new Date();                             // 현재 날짜정보
        else curDate = date;

        String curYear = Utils.INSTANCE.getYearFormat().format(curDate);    // yyyy
        String curMonth = Utils.INSTANCE.getMonthFormat().format(curDate);  // MM
        String curDay = Utils.INSTANCE.getDayFormat().format(curDate);      // dd
        String _date = Utils.INSTANCE.getDateFormat().format(curDate);      // yyyy년 MM월 dd일

        if (writeFragment != null) {
            try {
                writeFragment.setDateTextView(_date);
                writeFragment.setCurDate(Integer.parseInt(curYear), Integer.parseInt(curMonth), Integer.parseInt(curDay));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDayOfWeek(Date date) {
        String dayOfWeek = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayNum = calendar.get(Calendar.DAY_OF_WEEK);

        switch (dayNum) {
            case 1:
                dayOfWeek = "일";
                break;
            case 2:
                dayOfWeek = "월";
                break;
            case 3:
                dayOfWeek = "화";
                break;
            case 4:
                dayOfWeek = "수";
                break;
            case 5:
                dayOfWeek = "목";
                break;
            case 6:
                dayOfWeek = "금";
                break;
            case 7:
                dayOfWeek = "토";
                break;
        }

        return dayOfWeek;
    }

    @Override
    public void insertDB(Object[] objs) {
        if (db != null) db.insert(NoteDatabase.NOTE_TABLE, objs);
    }

    @Override
    public void insertDB2(Object[] objs) {
        if (db != null) db.insert2(NoteDatabase.NOTE_TABLE, objs);
    }

    @Override
    public ArrayList<Note> selectAllDB() {
        ArrayList<Note> items = new ArrayList<>();
        if (db != null) items = db.selectAll(NoteDatabase.NOTE_TABLE);
        return items;
    }

    @Override
    public ArrayList<Note> selectKeyword(String keyword) {
        ArrayList<Note> items = new ArrayList<>();
        if (db != null) items = db.selectKeyword(NoteDatabase.NOTE_TABLE, keyword);
        return items;
    }

    @Override
    public ArrayList<Note> selectPart(int year, int month) {
        ArrayList<Note> items = new ArrayList<>();
        if (db != null) items = db.selectPart(NoteDatabase.NOTE_TABLE, year, month);
        return items;
    }

    @Override
    public HashMap<Integer, Integer> selectMoodCount(boolean isAll, boolean isYear, boolean isMonth) {
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        if (db != null) hashMap = db.selectMoodCount(isAll, isYear, isMonth);
        return hashMap;
    }

    @Override
    public HashMap<Integer, Integer> selectMoodCountWeek(int weekOfDay) {
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        if (db != null) hashMap = db.selectMoodCountWeek(weekOfDay);
        return hashMap;
    }

    @Override
    public int selectLastYear() {
        if (db != null) return db.selectLastYear();
        return 0;
    }

    @Override
    public int selectAllCount() {
        if (db != null) return db.selectAllCount();
        return 0;
    }

    @Override
    public int selectStarCount() {
        if (db != null) return db.selectStarCount();
        return 0;
    }

    @Override
    public void deleteDB(int id) {
        if (db != null) db.delete(NoteDatabase.NOTE_TABLE, id);
    }

    @Override
    public void updateDB(Note item) {
        if (db != null) db.update(NoteDatabase.NOTE_TABLE, item);
    }

    @Override
    public void updateDB2(Note item) {
        if (db != null) db.update2(NoteDatabase.NOTE_TABLE, item);
    }

    @Override
    public void onTabSelected(int position) {
        switch (position) {
            case 0:
                bottomNavigationView.setSelectedItemId(R.id.tab1);
                break;
            case 1:
                bottomNavigationView.setSelectedItemId(R.id.tab2);
                break;
            case 2:
                bottomNavigationView.setSelectedItemId(R.id.tab3);
                break;
            case 3:
                bottomNavigationView.setSelectedItemId(R.id.tab4);
                break;
            case 4:
                bottomNavigationView.setSelectedItemId(R.id.tab5);
                break;
            default:
                Log.e(LOG, "onTabSelected() error..");
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private Boolean setSelectedTabItem(int id, FragmentTransaction transaction) {
        // WriteFragment 가 아닌 경우 위치탐색 종료(= stopLocationService())
        if (id != R.id.tab3) {
            if (locationManager != null) stopLocationService();
        }

        switch (id) {
            case R.id.tab1:
                selectedTabIndex = 0;
                transaction.replace(R.id.container, listFragment);
                transaction.commit();

                return true;
            case R.id.tab2:
                selectedTabIndex = 1;
                calendarFragment = new CalendarFragment();
                transaction.replace(R.id.container, calendarFragment);
                transaction.commit();

                return true;
            case R.id.tab3:
                selectedTabIndex = 2;

                if (calDate != null) {                          // 기분달력으로부터 넘어온 경우
                    writeFragment.setCalDate(calDate);          // 기분달력에서 가져온 Date 정보 전달
                    calDate = null;                             // calDate 초기화
                }

                if (updateItem != null) {                       // 일기수정 시
                    writeFragment.setUpdateItem(updateItem);    // updateItem 을 WriteFragment 로 전달
                    updateItem = null;                          // updateItem 초기화
                }

                transaction.replace(R.id.container, writeFragment, "write_fragment");
                transaction.commit();

                return true;
            case R.id.tab4:
                selectedTabIndex = 3;
                transaction.replace(R.id.container, graphFragment);
                transaction.commit();

                return true;
            case R.id.tab5:
                selectedTabIndex = 4;
                transaction.replace(R.id.container, optionFragment);
                transaction.commit();

                return true;
        }

        return false;
    }

    /**
     * OnRequestListener
     * (1) onRequest(String command)
     * (2) onRequest(String command, Date date)
     * (3) onRequestDetailActivity(Note item)
     * (4) onRequestWriteFragmentFromCal(Date date) : CalendarFragment -> WriteFragment
     */
    @Override
    public void onRequest(String command) {
        switch (command) {
            case "getCurrentLocation":
                getCurrentLocation(null);
                break;
            case "checkGPS":
                if (!Utils.INSTANCE.checkGPS(this)) {
                    DialogUtils.INSTANCE.showGPSDialog(this, () -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);

                        return Unit.INSTANCE;
                    });
                }

                break;
            case "showStopWriteDialog":
                DialogUtils.INSTANCE.showStopWriteDialog(this, () -> {
                    if (writeFragment != null) writeFragment.deleteFilesCache(true);
                    writeFragment = new WriteFragment();
                    selectedTabIndex = 0;
                    bottomNavigationView.setSelectedItemId(R.id.tab1);

                    return Unit.INSTANCE;
                });

                break;
            default:
                Log.d(LOG, "onRequest() 예외발생..");
                break;
        }
    }

    @Override
    public void onRequest(String command, Date date) {
        if (command.equals("getCurrentLocation")) getCurrentLocation(date);
    }

    @Override
    public void onRequestDetailActivity(Note item) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("item", item);
        detailActivityResult.launch(intent);
    }

    @Override
    public void onRequestWriteFragmentFromCal(Date date) {
        calDate = date;
        onTabSelected(2);
    }

    /**
     * OnResponseListener
     * using Volley
     */
    @Override
    public void onResponse(int requestCode, int responseCode, String response) {
        if (responseCode == Constants.VOLLEY_RESPONSE_OK) {
            switch (requestCode) {
                case Constants.REQUEST_WEATHER_BY_GRID: // 날씨 api
                    XmlParserCreator creator = () -> {
                        try {
                            return XmlPullParserFactory.newInstance().newPullParser();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };

                    GsonXml gsonXml = new GsonXmlBuilder()
                            .setXmlParserCreator(creator)
                            .setSameNameLists(true)
                            .create();
                    try {
                        WeatherResult result = gsonXml.fromXml(response, WeatherResult.class);
                        WeatherItem item = result.body.data.get(0);
                        // 현재 날씨 String
                        String curWeatherStr = item.getWfKor();
                        Log.e(LOG, "getWfKor(): " + curWeatherStr);

                        if (writeFragment != null) writeFragment.setWeatherImageView(curWeatherStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (writeFragment != null) writeFragment.setSwipeRefresh(false);

                    break;
                case Constants.REQUEST_REVERSE_GEOCODER:    // Naver reverse geocoding
                    Gson gson = new Gson();
                    ReverseGeocoder data = gson.fromJson(response, ReverseGeocoder.class);

                    if (!data.getResults().isEmpty()) {
                        RGResults result = data.getResults().get(0);
                        String country = result.getRegion().getArea0().getName();  // 국가
                        String sido = result.getRegion().getArea1().getName(); // 시도
                        String gungu = result.getRegion().getArea2().getName();    // 시군구
                        if (!gungu.isEmpty()) gungu += " ";
                        String dong = result.getRegion().getArea3().getName(); // 읍면동
                        String lee = result.getRegion().getArea4().getName();  // 리

                        if (!dong.isEmpty()) {
                            if (!lee.isEmpty()) {
                                dong += " ";
                            }
                        }

                        String address = gungu + dong;

                        writeFragment.setLocationTextView(address);
                    }

                    break;
            }
        } else {
            Log.e(LOG, "ERROR : Failure response code = " + responseCode);
        }
    }

    @Override
    public void showWriteFragment(Note item) {
        updateItem = item;
        onTabSelected(2);
    }

    private void handleBackPressed() {
        if (bottomNavigationView.getSelectedItemId() == R.id.tab1) {
            if (System.currentTimeMillis() > backPressTime + 2000) {
                backPressTime = System.currentTimeMillis();
                Toast.makeText(this, "뒤로가기 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();

                return;
            }

            // 두 번째로 누르면 콜백을 잠시 끄고 시스템 기본 동작(앱 닫기)에 맡긴다.
            if (System.currentTimeMillis() <= backPressTime + 2000) {
                backPressedCallback.setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                backPressedCallback.setEnabled(true);
            }
        } else if (bottomNavigationView.getSelectedItemId() == R.id.tab3) {
            DialogUtils.INSTANCE.showStopWriteDialog(this, () -> {
                if (writeFragment != null) writeFragment.deleteFilesCache(true);
                writeFragment = new WriteFragment();
                selectedTabIndex = 0;
                bottomNavigationView.setSelectedItemId(R.id.tab1);

                return Unit.INSTANCE;
            });
        } else {
            bottomNavigationView.setSelectedItemId(R.id.tab1);
        }
    }

    /**
     * ActivityResultLauncher
     * (startActivityForResult() is deprecated)
     */
    public final ActivityResultLauncher<CropImageContractOptions> cropImageActivityResult = registerForActivityResult(
            new CropImageContract(), result -> {
                // Crop activity 콜백
                if (result.isSuccessful()) {
                    Log.d(LOG, "xxx cropImageActivityResult: Success");
                    Uri uriContent = result.getUriContent();
                    if (uriContent != null) {
                        String filePath = result.getUriFilePath(this, true);
                        if (writeFragment != null) writeFragment.addPhoto(filePath);
                    }
                } else {
                    String errorMsg = Objects.requireNonNull(result.getError()).toString();
                    Log.d(LOG, "xxx cropImageActivityResult: Fail->" + errorMsg);
                }
            });

    public final ActivityResultLauncher<Intent> cameraResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                // 카메라 콜백
                int resultCode = result.getResultCode();

                if (resultCode == RESULT_OK) {
                    Log.d(LOG, "xxx cameraResult: RESULT_OK");
                    if (writeFragment != null) {
                        CropImageContractOptions options = new CropImageContractOptions(writeFragment.getFileUri(), new CropImageOptions())
                                .setGuidelines(CropImageView.Guidelines.ON);
                        cropImageActivityResult.launch(options);
                    }
                } else {
                    Log.d(LOG, "xxx cameraResult: RESULT_NOT_OK");
                    if (writeFragment != null) {
                        Objects.requireNonNull(getContentResolver()).delete(writeFragment.getFileUri(), null, null);
                    }
                }
            });

    public final ActivityResultLauncher<Intent> albumResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                // 앨범 콜백
                int resultCode = result.getResultCode();
                Intent data = result.getData();

                if (resultCode == RESULT_OK) {
                    Log.d(LOG, "xxx albumResult: RESULT_OK");
                    Uri uri = Objects.requireNonNull(data).getData();
                    CropImageContractOptions options = new CropImageContractOptions(uri, new CropImageOptions())
                            .setGuidelines(CropImageView.Guidelines.ON);
                    cropImageActivityResult.launch(options);
                } else {
                    Log.d(LOG, "xxx albumResult: RESULT_NOT_OK");
                }
            });

    public final ActivityResultLauncher<Intent> fontChangeResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                int resultCode = result.getResultCode();

                if (resultCode == RESULT_OK) recreate();
                else Log.d(LOG, "xxx fontChangeResult: RESULT_NOT_OK");
            });

    final ActivityResultLauncher<Intent> detailActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                int resultCode = result.getResultCode();
                Intent data = result.getData();

                switch (resultCode) {
                    case Constants.DETAIL_ACTIVITY_RESULT_DELETE:
                        Log.d(LOG, "xxx detailActivityResult: 일기삭제됨");
                        int id = Objects.requireNonNull(data).getIntExtra("id", -1);

                        if (id != -1) {
                            deleteDB(id);
                            if (selectedTabIndex == 0)
                                if (listFragment != null) listFragment.update();
                                else if (calendarFragment != null) onTabSelected(1);
                        }

                        break;

                    case Constants.DETAIL_ACTIVITY_RESULT_UPDATE:
                        Log.d(LOG, "xxx detailActivityResult: 일기수정됨");
                        Note item = (Note) (Objects.requireNonNull(data).getSerializableExtra("item"));
                        if (item != null) showWriteFragment(item);
                        else onTabSelected(2);

                        break;

                    default:
                        Log.d(LOG, "xxx detailActivityResult: 예외 응답");
                        break;
                }
            });

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.SELECTED_TAB_INDEX, selectedTabIndex);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(
                this,
                requestCode,
                permissions,
                this
        );
    }

    @Override
    public void onDenied(int i, String[] strings) {
    }

    @Override
    public void onGranted(int i, String[] strings) {
    }

    class GPSListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            curLocation = location;
            callCurrentWeather();
            callCurrentAddress();
            stopLocationService();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    }

    class LowVersionGPSListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            curLocation = location;
            callCurrentWeather();
            callCurrentAddress();
            stopLocationService();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    }
}