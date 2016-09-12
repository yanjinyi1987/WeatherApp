package geekband.yanjinyi1987.com.homework_part2_3;

import geekband.yanjinyi1987.com.homework_part2_3.service.HeXunWeatherInfo;
import geekband.yanjinyi1987.com.homework_part2_3.service.WeatherService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String GLOBAL_SETTINGS = "GlobalSettings";
    public static final String IS_CITY_LIST_IN_DATABASE = "isCityListInDatabase";
    public static final String IS_CITY_WEATHER_CACHED = "isCityWeatherCached";
    public static final int GET_CHOOSED_CITY_WEATHER = 5;
    public static final int INIT_VIEWPAGER = 4;
    public static final int GET_CITY_LIST = 3;

    //private Message List
    public static final int GLOBAL_FAULT = -2;
    public static final int GOT_GLOBAL_CITY_LIST = 0;

    //Message List
    public static final int SEND_MESSENGER_TO_SERVICE_MainActivity=0;
    public static final int SEND_MESSENGER_TO_SERVICE_ManageCityActivity =1;
    public static final int SEND_MESSENGER_TO_SERVICE_ChooseCityActivity=2;
    public static final int GET_GLOBAL_CITY_LIST_FROM_WEB=3;
    public static final int GET_CHOOSED_CITY_WEATHER_FROM_WEB=4;


    //Broadcast List
    public final String EXIT_APP = "MainActivity.ExitApp";
    public static boolean datachanged=false;

    public static final String TAG = "MainActivity";

    private ArrayList<WeatherService.CityInfo> choosedCityInfos;
    private Map<String,WeatherService.ProvinceList> provinceListMap;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mTestText.setText((String)msg.obj);
                    break;

                case 1: //GET_CITY_WEATHER
                    break;
                case 2: //GET_WEATHER_CONDITION:
                    break;
                case GET_CITY_LIST: //GET_CITY_LIST
                    //json Data -> Java Object
                    //mTestText.setText((String)msg.obj);

//                    Gson gson = new Gson();
//                    CityList cityInfo = gson.fromJson((String)msg.obj,CityList.class);
//
//                    List<CityInfo> cityInfos=cityInfo.city_list;
//                    //save city list to database and set SharedPreference isCityListInDatabase = true
//                    saveCityListToDatabaseAndGenerateObject(cityInfos);

                    break;
                case INIT_VIEWPAGER: //init ViewPager
                    Log.i("MainActivity","INIT_VIEWPAGER");
                    choosedCityInfos = (ArrayList<WeatherService.CityInfo>) msg.obj;
                    for (View view: viewList
                         ) {
                        view.setVisibility(View.INVISIBLE);
                    }
                    viewList.clear();
                    for (WeatherService.CityInfo city: choosedCityInfos
                         ) {
                        String cityName = city.getCity();
                        String cityId = city.getId();
                        cityId=null;
                        initViewPager(cityName,cityId,viewList);
                        Log.i("choosedCityList",city.getCity());
                    }

                    //myPagerAdapter = new MyPagerAdapter(MainActivity.this,viewList);
                    //viewPager.setAdapter(myPagerAdapter);
                    myPagerAdapter.notifyDataSetChanged();
                    if(whichToShowFirst==-1) {
                        whichToShowFirst=0;
                        viewPager.setCurrentItem(viewList.size() - 1);
                    }
                    if(choosedCityInfos !=null&& choosedCityInfos.size()!=0) {
                        Log.i("choosedCityInfos Size","before enter"+ choosedCityInfos.size());
                        getChoosedCityWeather(choosedCityInfos);
                    }
                    break;
                case GET_CHOOSED_CITY_WEATHER:
                    List<SimpleWeatherInfo> simpleWeatherInfo = (List<SimpleWeatherInfo>) msg.obj;
                    int i=0;
                    Log.i("viewList size",String.valueOf(viewList.size()));
                    while(simpleWeatherInfo.size()!=viewList.size()) {
                        setDatatoSharedPreferences(false, GLOBAL_SETTINGS, IS_CITY_WEATHER_CACHED);
                        getChoosedCityWeather(choosedCityInfos);
                    }
                    if(simpleWeatherInfo!=null && simpleWeatherInfo.size()!=0) {
                        for (View view : viewList
                                ) {
                            ((TextView) view.findViewById(R.id.weather_type)).setText(simpleWeatherInfo.get(i).weatherType);
                            ((TextView) view.findViewById(R.id.temperature)).setText(simpleWeatherInfo.get(i).temperature+"°C");
                            i++;
                        }
                    }
                    break;
                case -1:
                    Toast.makeText(MainActivity.this,"网络错误",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    class ServiceHandler extends  Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                //>>>>>>>>>>>>>>
                case GLOBAL_FAULT:
                    break;
                case GOT_GLOBAL_CITY_LIST:
                    mChooseCity.setEnabled(true);
                    mRefreshWeather.setEnabled(true);
                    break;
                case INIT_VIEWPAGER: //init ViewPager
                    Log.i("MainActivity","INIT_VIEWPAGER");
                    choosedCityInfos = (ArrayList<WeatherService.CityInfo>) msg.obj;
                    for (View view: viewList
                            ) {
                        view.setVisibility(View.INVISIBLE);
                    }
                    viewList.clear();
                    for (WeatherService.CityInfo city: choosedCityInfos
                            ) {
                        String cityName = city.getCity();
                        String cityId = city.getId();
                        cityId=null;
                        initViewPager(cityName,cityId,viewList);
                        Log.i(TAG,"choosedCityList "+city.getCity());
                    }

                    myPagerAdapter.notifyDataSetChanged();
                    if(whichToShowFirst==-1) {
                        whichToShowFirst=0;
                        viewPager.setCurrentItem(viewList.size() - 1);
                    }
                    if(choosedCityInfos !=null&& choosedCityInfos.size()!=0) {
                        Log.i(TAG,"choosedCityInfos Size "+"before enter "+ choosedCityInfos.size());
                        getChoosedCityWeather(choosedCityInfos);
                    }
                    break;
                case GET_CHOOSED_CITY_WEATHER:
                    List<SimpleWeatherInfo> simpleWeatherInfo = (List<SimpleWeatherInfo>) msg.obj;
                    int i=0;
                    Log.i("viewList size",String.valueOf(viewList.size()));
                    while(simpleWeatherInfo.size()!=viewList.size()) {
                        setDatatoSharedPreferences(false, GLOBAL_SETTINGS, IS_CITY_WEATHER_CACHED);
                        getChoosedCityWeather(choosedCityInfos);
                    }
                    if(simpleWeatherInfo!=null && simpleWeatherInfo.size()!=0) {
                        for (View view : viewList
                                ) {
                            ((TextView) view.findViewById(R.id.weather_type)).setText(simpleWeatherInfo.get(i).weatherType);
                            ((TextView) view.findViewById(R.id.temperature)).setText(simpleWeatherInfo.get(i).temperature+"°C");
                            i++;
                        }
                    }
                    break;
                case -1:
                    Toast.makeText(MainActivity.this,"网络错误",Toast.LENGTH_SHORT).show();
                    break;
                //<<<<<<<<<<<<<<
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private Messenger mServiceMessenger;
    private Messenger mMessenger = new Messenger(new ServiceHandler());

    private boolean onBound = false;

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG,"Connected to Service");
            Log.i(TAG,"Got Service's Messenger");
            mServiceMessenger = new Messenger(service);
            Log.i(TAG,"Send Messenger to Service directly");
            Message msg = new Message();
            msg.what = SEND_MESSENGER_TO_SERVICE_MainActivity;
            msg.obj = mMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            onBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            onBound = false;
        }
    };


    private Button mChooseCity;
    private EditText mTestText;
    private ViewPager viewPager;
    private List<View> viewList=new ArrayList<>();
    private LayoutInflater inflater;
    private MyPagerAdapter myPagerAdapter;
    private int whichToShowFirst = 0;
    private Button mRefreshWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("MainActivity","onCreate");
        //start the service
        startService(new Intent(MainActivity.this,WeatherService.class));
        //bind the service
        bindService(new Intent(MainActivity.this,WeatherService.class),mServiceConnection,BIND_AUTO_CREATE);
        String cityId = getIntent().getStringExtra("Position");
        if(cityId!=null) {
            whichToShowFirst=-1;
        }
        initViews();
    }

    public void setDatatoSharedPreferences(boolean data,String filename,String key) {
        SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences(filename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key,data);
        editor.apply();
    }

    private void initViews() {
        mChooseCity = (Button) findViewById(R.id.choose_city);
        //mTestText = (EditText) findViewById(R.id.test_text);
        mRefreshWeather = (Button) findViewById(R.id.refresh_weather_data);
//      view pager

        viewPager = (ViewPager) findViewById(R.id.city_weather_view_pager);

        inflater = getLayoutInflater();

        myPagerAdapter = new MyPagerAdapter(MainActivity.this,viewList);
        viewPager.setAdapter(myPagerAdapter);

        getChoosedCity();

        mChooseCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,ManageCityActivity.class));
            }
        });

        mRefreshWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if network error, we need refresh by hand!!!!
                setDatatoSharedPreferences(false,GLOBAL_SETTINGS,IS_CITY_WEATHER_CACHED);
                getChoosedCityWeather(choosedCityInfos);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setDatatoSharedPreferences(false,GLOBAL_SETTINGS,IS_CITY_WEATHER_CACHED);
                getChoosedCityWeather(choosedCityInfos);
            }
        },600000);

        SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_SETTINGS,MODE_PRIVATE);
        Boolean isCityListInDatabase = sharedPreferences.getBoolean(IS_CITY_LIST_IN_DATABASE,false);
        if(!isCityListInDatabase) {
            getCityListandSave(); //save data in handler
            mChooseCity.setEnabled(false);
            mRefreshWeather.setEnabled(false);
        }
    }

    private void getChoosedCityWeather(List<WeatherService.CityInfo> choosedCityIds) {
        //send message to Service to do this;
        Message msg = new Message();
        msg.what = GET_CHOOSED_CITY_WEATHER_FROM_WEB;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Looper.prepare();
//                SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
//                Boolean isCityWeatherCached = sharedPreferences.getBoolean(IS_CITY_WEATHER_CACHED, false);
//                HeXunWeatherInfo heXunWeatherInfo;
//                Gson gson = new Gson();
//                CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
//                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()
//
//                List<SimpleWeatherInfo> simpleWeatherInfoList = new ArrayList<SimpleWeatherInfo>();
//                Message msg_return = new Message();
//                if (!isCityWeatherCached) {
//                    boolean getAllWeathers=true;
//                    ArrayList<String> weatherJsons = new ArrayList<>();
//                    for (WeatherService.CityInfo city : choosedCityInfos
//                            ) {
//                        WeatherHttp weatherHttp = new WeatherHttp(handler);
//                        Message msg = weatherHttp.requestWithoutThread(WeatherHttp.GET_CITY_WEATHER, city.getId());
//                        if (msg.what != -1) {
//                            heXunWeatherInfo = gson.fromJson((String) msg.obj, HeXunWeatherInfo.class);
//                            if (heXunWeatherInfo.heWeatherDS0300.get(0).status.equals("ok")) {
//                                weatherJsons.add((String) msg.obj);
//                                Log.i("MainActivity", String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.fl));
//                                simpleWeatherInfoList.add(new SimpleWeatherInfo(
//                                        city.getCity(),
//                                        city.getId(),
//                                        heXunWeatherInfo.heWeatherDS0300.get(0).now.cond.txt,
//                                        String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.tmp)
//                                ));
//                            }
//                            else {
//                                getAllWeathers=false;
//                                break;
//                            }
//                        } else {
//                            getAllWeathers=false;
//                            Log.i("MainActivity", "Error json");
//                            break;
//                        }
//                    }
//                    if(getAllWeathers==true) {
//                        Log.i("getChoosedWeather",String.valueOf(weatherJsons.size()));
//                        Log.i("getChoosedWeather ","choosedCityInfos"+String.valueOf(choosedCityInfos.size()));
//                        cityListOperations.cacheWeathers(weatherJsons);
//                        setDatatoSharedPreferences(true, GLOBAL_SETTINGS, IS_CITY_WEATHER_CACHED);
//                    }
//                }
//                else {
//                    //read from database
//                    ArrayList<String> weatherJsons = (ArrayList<String>) cityListOperations.getCachedWeathers();
//                    if (weatherJsons!=null && weatherJsons.size() != 0) {
//                        for (String weatherJson:weatherJsons
//                             ) {
//                            heXunWeatherInfo = gson.fromJson(weatherJson, HeXunWeatherInfo.class);
//                            simpleWeatherInfoList.add(new SimpleWeatherInfo(
//                                    heXunWeatherInfo.heWeatherDS0300.get(0).basic.city,
//                                    heXunWeatherInfo.heWeatherDS0300.get(0).basic.id,
//                                    heXunWeatherInfo.heWeatherDS0300.get(0).now.cond.txt,
//                                    String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.tmp)
//                            ));
//                        }
//
//                    }
//                }
//
//                msg_return.what= GET_CHOOSED_CITY_WEATHER;
//                msg_return.obj = simpleWeatherInfoList;
//                handler.sendMessage(msg_return);
//                Looper.loop();
//            }
//        }).start();
    }

    private void getChoosedCity() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<WeatherService.CityInfo> cityInfos;

                Looper.prepare();
                CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()
                cityInfos = cityListOperations.getChoosedCities();

                Message msg = new Message();
                msg.what = INIT_VIEWPAGER;
                msg.obj = cityInfos;
                handler.sendMessage(msg);
                Looper.loop();
            }
        }).start();
    }

    private void initViewPager(String cityName,String cityId,List<View> viewList) {
        View view = inflater.inflate(R.layout.city_weather_view,null);
        ((TextView)view.findViewById(R.id.city_name)).setText(cityName);
        //((TextView)view.findViewById(R.id.weather_type)).setText(cityId);
        viewList.add(view);

    }
    private void updateViewPager(List<View> viewList) {

    }

    private void getCityListandSave(){
        saveCityListToDatabaseAndGenerateObject();
    }
    //no need to update UI
    private void saveCityListToDatabaseAndGenerateObject() {
        //send message to Service to do this;
        Message msg = new Message();
        msg.what = GET_GLOBAL_CITY_LIST_FROM_WEB;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("MainActivity","onStart");
        //update the viewpager
        if(datachanged==true) {
            datachanged=false;
            getChoosedCity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity","onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity","onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("MainActivity","onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity","onDestroy "+this.getTaskId());
        //unbindService
        unbindService(mServiceConnection);
        Log.i("MainActivity","Send Exit App");
        Intent exitAppIntent = new Intent();
        exitAppIntent.setAction(EXIT_APP);
        sendBroadcast(exitAppIntent); //或许是有thread没有被关闭额！！！ 重构啦
    }
}


class MyPagerAdapter extends PagerAdapter {
    Context mContext;
    List<View> mListView;
    public MyPagerAdapter(Context context,List<View> objects) {
        mContext = context;
        mListView = objects;
    }

    @Override
    public int getCount() {
        return mListView.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        //super.destroyItem(container, position, object);
        Log.i("Adapter","destroyItem");
            container.removeView(mListView.get(position));
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        //return super.instantiateItem(container, position);
        Log.i("Adapter","instantiateItem");
        container.addView(mListView.get(position));
        return mListView.get(position);
    }
}
//class CityList {
//    public List<CityInfo> city_list;
//}

class SimpleWeatherInfo {
    public String cityName;
    public String cityId;
    public String weatherType;
    public String temperature;

    public SimpleWeatherInfo(String cityName, String cityId, String weatherType, String temperature) {
        this.cityName = cityName;
        this.cityId = cityId;
        this.weatherType = weatherType;
        this.temperature = temperature;
    }
}