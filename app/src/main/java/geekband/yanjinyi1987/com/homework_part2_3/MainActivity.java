package geekband.yanjinyi1987.com.homework_part2_3;

import geekband.yanjinyi1987.com.homework_part2_3.service.HeXunWeatherInfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
    public final String EXIT_APP = "MainActivity.ExitApp";
    public static boolean datachanged=false;

    private ArrayList<CityInfo> choosedCityInfos;
    private Map<String,ProvinceList> provinceListMap;
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
                    choosedCityInfos = (ArrayList<CityInfo>) msg.obj;
                    for (View view: viewList
                         ) {
                        view.setVisibility(View.INVISIBLE);
                    }
                    viewList.clear();
                    for (CityInfo city: choosedCityInfos
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
        }
    }

    private void getChoosedCityWeather(final List<CityInfo> choosedCityInfos) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
                Boolean isCityWeatherCached = sharedPreferences.getBoolean(IS_CITY_WEATHER_CACHED, false);
                HeXunWeatherInfo heXunWeatherInfo;
                Gson gson = new Gson();
                CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()

                List<SimpleWeatherInfo> simpleWeatherInfoList = new ArrayList<SimpleWeatherInfo>();
                Message msg_return = new Message();
                if (!isCityWeatherCached) {
                    boolean getAllWeathers=true;
                    ArrayList<String> weatherJsons = new ArrayList<>();
                    for (CityInfo city : choosedCityInfos
                            ) {
                        WeatherHttp weatherHttp = new WeatherHttp(handler);
                        Message msg = weatherHttp.requestWithoutThread(WeatherHttp.GET_CITY_WEATHER, city.getId());
                        if (msg.what != -1) {
                            heXunWeatherInfo = gson.fromJson((String) msg.obj, HeXunWeatherInfo.class);
                            if (heXunWeatherInfo.heWeatherDS0300.get(0).status.equals("ok")) {
                                weatherJsons.add((String) msg.obj);
                                Log.i("MainActivity", String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.fl));
                                simpleWeatherInfoList.add(new SimpleWeatherInfo(
                                        city.getCity(),
                                        city.getId(),
                                        heXunWeatherInfo.heWeatherDS0300.get(0).now.cond.txt,
                                        String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.tmp)
                                ));
                            }
                            else {
                                getAllWeathers=false;
                                break;
                            }
                        } else {
                            getAllWeathers=false;
                            Log.i("MainActivity", "Error json");
                            break;
                        }
                    }
                    if(getAllWeathers==true) {
                        Log.i("getChoosedWeather",String.valueOf(weatherJsons.size()));
                        Log.i("getChoosedWeather ","choosedCityInfos"+String.valueOf(choosedCityInfos.size()));
                        cityListOperations.cacheWeathers(weatherJsons);
                        setDatatoSharedPreferences(true, GLOBAL_SETTINGS, IS_CITY_WEATHER_CACHED);
                    }
                }
                else {
                    //read from database
                    ArrayList<String> weatherJsons = (ArrayList<String>) cityListOperations.getCachedWeathers();
                    if (weatherJsons!=null && weatherJsons.size() != 0) {
                        for (String weatherJson:weatherJsons
                             ) {
                            heXunWeatherInfo = gson.fromJson(weatherJson, HeXunWeatherInfo.class);
                            simpleWeatherInfoList.add(new SimpleWeatherInfo(
                                    heXunWeatherInfo.heWeatherDS0300.get(0).basic.city,
                                    heXunWeatherInfo.heWeatherDS0300.get(0).basic.id,
                                    heXunWeatherInfo.heWeatherDS0300.get(0).now.cond.txt,
                                    String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.tmp)
                            ));
                        }

                    }
                }

                msg_return.what= GET_CHOOSED_CITY_WEATHER;
                msg_return.obj = simpleWeatherInfoList;
                handler.sendMessage(msg_return);
                Looper.loop();
            }
        }).start();
    }

    private void getChoosedCity() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<CityInfo> cityInfos;

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg;
                WeatherHttp weatherHttp = new WeatherHttp(handler);
                msg=weatherHttp.requestWithoutThread(WeatherHttp.GET_CITY_LIST,"allchina");

                if(msg.what==GET_CITY_LIST) {
                    Gson gson = new Gson();
                    CityList cityList = gson.fromJson((String) msg.obj, CityList.class);

                    List<CityInfo> cityInfos = cityList.city_list;

                    CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
                    //这一步创建了数据库
                    //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();
                    try {
                        cityListOperations.saveData(cityInfos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        //set SharedPreference isCityListInDatabase = true
                        setDatatoSharedPreferences(true, GLOBAL_SETTINGS, IS_CITY_LIST_IN_DATABASE);
                    }
                }
            }

            private void setDatatoSharedPreferences(boolean data,String filename,String key) {
                SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences(filename, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(key,data);
                editor.apply();
            }
        }).start();
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

//ProvinceList -> province1 province2 ...
//                    city1     city1
//                    city2     city2
//                    city3     city3
//                    ...       ...


//class ProvinceList {
//    private String provinceName;
//    private List<CityInfo> cities;
//
//    public ProvinceList(String provinceName) {
//        this.provinceName = provinceName;
//        cities = new ArrayList<>();
//    }
//
//    public String getProvinceName() {
//        return provinceName;
//    }
//
//    public List<CityInfo> getCities() {
//        return cities;
//    }
//
//    public void setProvinceName(String provinceName) {
//        this.provinceName = provinceName;
//    }
//
//    public void setCities(List<CityInfo> cities) {
//        this.cities = cities;
//    }
//}
//
//class CityInfo {
//    private String city; //city
//    private String cnty; //country
//    private String id;   //cityId
//    private String lat;  //latitude
//    private String lon;  //longitude
//    private String prov; //province
//
//    public CityInfo() {
//
//    }
//
//    public CityInfo(String city) {
//        this(city,null,null,null,null,null);
//    }
//
//    public CityInfo(String city, String cnty, String id, String lat, String lon, String prov) {
//        this.city=city;
//        this.cnty=cnty;
//        this.id=id;
//        this.lat=lat;
//        this.lon=lon;
//        this.prov=prov;
//    }
//
//    public String getCity() {
//        return city;
//    }
//
//    public String getCnty() {
//        return cnty;
//    }
//
//    public String getId() {
//        return id;
//    }
//
//    public String getLat() {
//        return lat;
//    }
//
//    public String getLon() {
//        return lon;
//    }
//
//    public String getProv() {
//        return prov;
//    }
//
//    public void setCity(String city) {
//        this.city = city;
//    }
//
//    public void setCnty(String cnty) {
//        this.cnty = cnty;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public void setLat(String lat) {
//        this.lat = lat;
//    }
//
//    public void setLon(String lon) {
//        this.lon = lon;
//    }
//
//    public void setProv(String prov) {
//        this.prov = prov;
//    }
//}