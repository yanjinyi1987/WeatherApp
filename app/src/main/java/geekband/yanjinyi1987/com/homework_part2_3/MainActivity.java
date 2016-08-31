package geekband.yanjinyi1987.com.homework_part2_3;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    public static final String GLOBAL_SETTINGS = "GlobalSettings";
    public static final String IS_CITY_LIST_IN_DATABASE = "isCityListInDatabase";
    public static final String IS_CITY_WEATHER_CACHED = "isCityWeatherCached";
    public static final int GET_CHOOSED_CITY_WEATHER = 5;
    public static final int INIT_VIEWPAGER = 4;
    public static final int GET_CITY_LIST = 3;
    public static boolean datachanged=false;

    private ArrayList<CityList> choosedCityLists;
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
//                    CityInfo cityInfo = gson.fromJson((String)msg.obj,CityInfo.class);
//
//                    List<CityList> cityLists=cityInfo.city_info;
//                    //save city list to database and set SharedPreference isCityListInDatabase = true
//                    saveCityListToDatabaseAndGenerateObject(cityLists);

                    break;
                case INIT_VIEWPAGER: //init ViewPager
                    Log.i("MainActivity","INIT_VIEWPAGER");
                    choosedCityLists = (ArrayList<CityList>) msg.obj;
                    for (View view: viewList
                         ) {
                        view.setVisibility(View.INVISIBLE);
                    }
                    viewList.clear();
                    for (CityList city: choosedCityLists
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
                    if(choosedCityLists!=null&&choosedCityLists.size()!=0) {
                        Log.i("choosedCityLists Size","before enter"+choosedCityLists.size());
                        getChoosedCityWeather(choosedCityLists);
                    }
                    break;
                case GET_CHOOSED_CITY_WEATHER:
                    List<SimpleWeatherInfo> simpleWeatherInfo = (List<SimpleWeatherInfo>) msg.obj;
                    int i=0;
                    Log.i("viewList size",String.valueOf(viewList.size()));
                    while(simpleWeatherInfo.size()!=viewList.size()) {
                        setDatatoSharedPreferences(false, GLOBAL_SETTINGS, IS_CITY_WEATHER_CACHED);
                        getChoosedCityWeather(choosedCityLists);
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
                getChoosedCityWeather(choosedCityLists);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setDatatoSharedPreferences(false,GLOBAL_SETTINGS,IS_CITY_WEATHER_CACHED);
                getChoosedCityWeather(choosedCityLists);
            }
        },600000);

        SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_SETTINGS,MODE_PRIVATE);
        Boolean isCityListInDatabase = sharedPreferences.getBoolean(IS_CITY_LIST_IN_DATABASE,false);
        if(!isCityListInDatabase) {
            getCityListandSave(); //save data in handler
        }
    }

    private void getChoosedCityWeather(final List<CityList> choosedCityLists) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
                Boolean isCityWeatherCached = sharedPreferences.getBoolean(IS_CITY_WEATHER_CACHED, false);
                WeatherInfo weatherInfo;
                Gson gson = new Gson();
                CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()

                List<SimpleWeatherInfo> simpleWeatherInfoList = new ArrayList<SimpleWeatherInfo>();
                Message msg_return = new Message();
                if (!isCityWeatherCached) {
                    boolean getAllWeathers=true;
                    ArrayList<String> weatherJsons = new ArrayList<>();
                    for (CityList city : choosedCityLists
                            ) {
                        WeatherHttp weatherHttp = new WeatherHttp(handler);
                        Message msg = weatherHttp.requestWithoutThread(WeatherHttp.GET_CITY_WEATHER, city.getId());
                        if (msg.what != -1) {
                            weatherInfo = gson.fromJson((String) msg.obj, WeatherInfo.class);
                            if (weatherInfo.heWeatherDS0300.get(0).status.equals("ok")) {
                                weatherJsons.add((String) msg.obj);
                                Log.i("MainActivity", String.valueOf(weatherInfo.heWeatherDS0300.get(0).now.fl));
                                simpleWeatherInfoList.add(new SimpleWeatherInfo(
                                        city.getCity(),
                                        city.getId(),
                                        weatherInfo.heWeatherDS0300.get(0).now.cond.txt,
                                        String.valueOf(weatherInfo.heWeatherDS0300.get(0).now.tmp)
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
                        Log.i("getChoosedWeather ","choosedCityLists"+String.valueOf(choosedCityLists.size()));
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
                            weatherInfo = gson.fromJson(weatherJson, WeatherInfo.class);
                            simpleWeatherInfoList.add(new SimpleWeatherInfo(
                                    weatherInfo.heWeatherDS0300.get(0).basic.city,
                                    weatherInfo.heWeatherDS0300.get(0).basic.id,
                                    weatherInfo.heWeatherDS0300.get(0).now.cond.txt,
                                    String.valueOf(weatherInfo.heWeatherDS0300.get(0).now.tmp)
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
                List<CityList> cityLists;

                Looper.prepare();
                CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()
                cityLists = cityListOperations.getChoosedCities();

                Message msg = new Message();
                msg.what = INIT_VIEWPAGER;
                msg.obj = cityLists;
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
                    CityInfo cityInfo = gson.fromJson((String) msg.obj, CityInfo.class);

                    List<CityList> cityLists = cityInfo.city_info;

                    CityListOperations cityListOperations = new CityListOperations(MainActivity.this);
                    //这一步创建了数据库
                    //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();
                    try {
                        cityListOperations.saveData(cityLists);
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
        Log.i("MainActivity","onDestroy");
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
class CityInfo {
    public List<CityList> city_info;
}

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

class WeatherInfo {
    @SerializedName("HeWeather data service 3.0")
    public List<HeWeatherDS0300> heWeatherDS0300;

    public static class HeWeatherDS0300 {
        public Aqi aqi;
        public Basic basic;
        public List<DailyForecast> daily_forecase;
        public List<HourlyForecast> hourly_forecase;
        public Now now;
        public String status;
        public Suggestion suggestion;

        public static class Aqi {
            City city;
            public static class City{
                int aqi;
                int co;
                int no2;
                int o3;
                int pm10;
                int pm25;
                String qlty;
                int so2;
            }
        }

        public static class Basic {
            String city;
            String cnty;
            String id;
            Float lat;
            Float lon;
            Update update;
            public static class Update {
                String loc;
                String utc;
            }
        }

        public static class DailyForecast {
            Astro astro;
            Cond cond;
            String date;
            int hum;
            Float pcpn;
            int pop;
            int pres;
            Temperature tmp;
            int vis;
            Wind wind;

            public static class Astro {
                String sr;
                String ss;
            }

            public static class Cond {
                int code_d;
                int code_n;
                String txt_d;
                String txt_n;
            }

            public static class Temperature {
                int max;
                int min;
            }
            public static class Wind {
                int deg;
                String dir;
                String sc;
                int spd;
            }
        }
        public static class HourlyForecast {
            String date;
            int hum;
            int pop;
            int pres;
            int tmp;
            Wind wind;
            public static class Wind {
                int deg;
                String dir;
                String sc;
                int spd;
            }
        }

        public static class Now {
            Cond cond;
            int fl;
            int hum;
            Float pcpn;
            int pres;
            int tmp;
            int vis;
            Wind wind;
            public static class Cond {
                int code;
                String txt;
            }
            public static class Wind {
                int deg;
                String dir;
                String sc;
                int spd;
            }
        }
        public static class Suggestion {
            Comf comf;
            Cw cw;
            Drsg drsg;
            Flu flu;
            Sport sport;
            Trav trav;
            UV uv;

            public static class Comf{
                String brf;
                String txt;
            }

            public static class Cw{
                String brf;
                String txt;
            }

            public static class Drsg{
                String brf;
                String txt;
            }

            public static class Flu{
                String brf;
                String txt;
            }

            public static class Sport{
                String brf;
                String txt;
            }

            public static class Trav{
                String brf;
                String txt;
            }

            public static class UV{
                String brf;
                String txt;
            }
        }
    }
}

class WeatherHttp{
    public static final String APIKey = "3e9f8bab0ec34c37b49f4cb1652c956d";
    public static final int GET_CITY_WEATHER=0;
    public static final int GET_WEATHER_CONDITION=1;
    public static final int GET_CITY_LIST=2;

    private Handler mHandler;

    public WeatherHttp(Handler handler) {
        mHandler=handler;
    }


    private String getWeatherUrl(String cityId) {
        return "https://api.heweather.com/x3/weather?cityid="+cityId+"&key="+APIKey;
    }
/*search 	景天气代码列表类型 	全部天气代码：allcond*/
    private String getWeatherConditionUrl(String type) {
        return "https://api.heweather.com/x3/condition?search="+type+"&key="+APIKey;
    }
/*search 	城市列表类型 	国内城市：allchina、 热门城市：hotworld、 全部城市：allworld*/
    private String getCityListUrl(String type) {
        return "https://api.heweather.com/x3/citylist?search="+type+"&key="+APIKey;
    }
/*
* 0
* 1
* 2
* */
    public void request(final int requestType, final String addition_data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String urlStr=null;
                URL url=null;
                HttpURLConnection httpsURLConnection=null;
                InputStream jsonIn=null;
                Message msg = new Message();
                msg.what=0; //iniital
                StringBuilder response=null;
                Log.i("Network","start");
                switch (requestType) {
                    case GET_CITY_WEATHER:
                        urlStr = getWeatherUrl(addition_data);
                        msg.what = 1;
                        break;
                    case GET_WEATHER_CONDITION:
                        urlStr = getWeatherConditionUrl(addition_data);
                        msg.what = 2;
                        break;
                    case GET_CITY_LIST:
                        urlStr = getCityListUrl(addition_data);
                        msg.what = 3;
                        break;
                    default:
                        break;
                }
                Log.i("request",urlStr);
                try {
                    url = new URL(urlStr);
                } catch (MalformedURLException e) {
                    Log.i("MainActivity","MalformedURL");
                    msg.what = -1; //error
                    e.printStackTrace();
                }

                try {
                    httpsURLConnection = (HttpsURLConnection)url.openConnection();
                } catch (IOException e) {
                    Log.i("MainActivity","IOException in openConnection");
                    msg.what = -1; //error
                    e.printStackTrace();
                }

                try {
                    httpsURLConnection.setRequestMethod("GET");
                } catch (ProtocolException e) {
                    Log.i("MainActivity","ProtocolException in setRequestMethod");
                    msg.what = -1; //error
                    e.printStackTrace();
                }
                httpsURLConnection.setConnectTimeout(8000);
                httpsURLConnection.setReadTimeout(8000);

                try {
                    jsonIn = httpsURLConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(jsonIn));
                    response = new StringBuilder();
                    String line;
                    while((line = reader.readLine())!=null) {
                        response.append(line);
                    }
                    /*return json data*/
                } catch (IOException e) {
                    Log.i("MainActivity","IOException in getInputStream");
                    msg.what = -1; //error
                    e.printStackTrace();
                }
                if(httpsURLConnection!=null) {
                    httpsURLConnection.disconnect();
                }
                if(msg.what!=-1) {
                    //msg.what = 0; //success

                    msg.obj = response.toString();
                    Log.i("request",(String)msg.obj);
                    mHandler.sendMessage(msg);
                }
            }
        }).start();


    }


    public Message requestWithoutThread(final int requestType, final String addition_data){
                String urlStr=null;
                URL url=null;
                HttpURLConnection httpsURLConnection=null;
                InputStream jsonIn=null;
                Message msg = new Message();
                msg.what=0; //iniital
                StringBuilder response=null;
                Log.i("Network","start");
                switch (requestType) {
                    case GET_CITY_WEATHER:
                        urlStr = getWeatherUrl(addition_data);
                        msg.what = 1;
                        break;
                    case GET_WEATHER_CONDITION:
                        urlStr = getWeatherConditionUrl(addition_data);
                        Log.i("request",urlStr);
                        msg.what = 2;
                        break;
                    case GET_CITY_LIST:
                        urlStr = getCityListUrl(addition_data);
                        msg.what = 3;
                        break;
                    default:
                        break;
                }

                try {
                    url = new URL(urlStr);
                } catch (MalformedURLException e) {
                    Log.i("MainActivity","MalformedURL");
                    msg.what = -1; //error
                    e.printStackTrace();
                }

                try {
                    httpsURLConnection = (HttpsURLConnection)url.openConnection();
                } catch (IOException e) {
                    Log.i("MainActivity","IOException in openConnection");
                    msg.what = -1; //error
                    e.printStackTrace();
                }

                try {
                    httpsURLConnection.setRequestMethod("GET");
                } catch (ProtocolException e) {
                    Log.i("MainActivity","ProtocolException in setRequestMethod");
                    msg.what = -1; //error
                    e.printStackTrace();
                }
                httpsURLConnection.setConnectTimeout(8000);
                httpsURLConnection.setReadTimeout(8000);

                try {
                    jsonIn = httpsURLConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(jsonIn));
                    response = new StringBuilder();
                    String line;
                    while((line = reader.readLine())!=null) {
                        response.append(line);
                    }
                    /*return json data*/
                } catch (IOException e) {
                    Log.i("MainActivity","IOException in getInputStream");
                    msg.what = -1; //error
                    e.printStackTrace();
                }
                if(httpsURLConnection!=null) {
                    httpsURLConnection.disconnect();
                }
                if(msg.what!=-1) {
                    //msg.what = 0; //success
                    msg.obj = response.toString();
                    Log.i("request",(String)msg.obj);
                }
        return msg;
    }
}

class ProvinceList {
    private String provinceName;
    private List<CityList> cities;

    public ProvinceList(String provinceName) {
        this.provinceName = provinceName;
        cities = new ArrayList<>();
    }

    public String getProvinceName() {
        return provinceName;
    }

    public List<CityList> getCities() {
        return cities;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public void setCities(List<CityList> cities) {
        this.cities = cities;
    }
}

class CityList {
    private String city; //city
    private String cnty; //country
    private String id;   //cityId
    private String lat;  //latitude
    private String lon;  //longitude
    private String prov; //province

    public CityList() {

    }

    public CityList(String city,String cnty,String id,String lat,String lon,String prov) {
        this.city=city;
        this.cnty=cnty;
        this.id=id;
        this.lat=lat;
        this.lon=lon;
        this.prov=prov;
    }

    public String getCity() {
        return city;
    }

    public String getCnty() {
        return cnty;
    }

    public String getId() {
        return id;
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public String getProv() {
        return prov;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCnty(String cnty) {
        this.cnty = cnty;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public void setProv(String prov) {
        this.prov = prov;
    }
}

class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, CityListOperations.DB_NAME,null,1);
    }

    private static DBHelper mInstance; //single instance
    public synchronized  static DBHelper getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new DBHelper(context);
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table " +
                    CityListOperations.TABLE_HE_XUN_CITY_LIST + "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_COUNTRY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_LATITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_LONGITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_PROVINCE + " varchar(20) not null)");


            db.execSQL("create table " +
                    CityListOperations.TABLE_CHOOSED_CITY_LIST + "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_COUNTRY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_LATITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_LONGITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_PROVINCE + " varchar(20) not null)");

            db.execSQL("create table " +
                    CityListOperations.TABLE_WEATHER_CACHE + "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_WEATHER_JSON + " text" + ")");
        }catch (SQLiteException e) {
            e.printStackTrace();
        }
        finally {
            Log.i("SQLite","add table successfully");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}


class CityListOperations {
    public static final String DB_NAME = "cityList.db";
    public static final String TABLE_HE_XUN_CITY_LIST = "HeXunCityList";
    public static final String TABLE_CHOOSED_CITY_LIST = "ChoosedCityList";
    public static final String TABLE_WEATHER_CACHE = "ChoosedCityWeatherCache";
    public static final String SECTION_CITY = "city";
    public static final String SECTION_COUNTRY = "country";
    public static final String SECTION_CITY_ID = "cityId";
    public static final String SECTION_LATITUDE = "latitude";
    public static final String SECTION_LONGITUDE = "longitude";
    public static final String SECTION_PROVINCE = "province";
    public static final String SECTION_WEATHER_JSON = "weather_json";

    private DBHelper helper;
    private SQLiteDatabase db;

    Context mContext;

    public CityListOperations(Context context) {
        helper = DBHelper.getInstance(context);
        db = helper.getWritableDatabase();
        mContext = context;
    }

    public long insert_to_table_city(SQLiteDatabase db, String table_name, CityList city) {
        //实例化常量值
        ContentValues cValue = new ContentValues();
        //添加用户名
        cValue.put(SECTION_CITY, city.getCity());
        //添加密码
        cValue.put(SECTION_COUNTRY, city.getCnty());
        //
        cValue.put(SECTION_CITY_ID, city.getId());
        //
        cValue.put(SECTION_LATITUDE, city.getLat());
        //
        cValue.put(SECTION_LONGITUDE, city.getLon());
        //
        cValue.put(SECTION_PROVINCE, city.getProv());

        //调用insert()方法插入数据
        return db.insert(table_name, null, cValue);
    }

    public void insert_to_table_cityLists(SQLiteDatabase db, String table_name, List<CityList> cityList) {
        for (CityList city : cityList
                ) {
            insert_to_table_city(db, table_name, city);
        }
    }

    public void saveData(List<CityList> cityLists) {
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            db.beginTransaction();
            try {
                insert_to_table_cityLists(db, TABLE_HE_XUN_CITY_LIST, cityLists);
                db.setTransactionSuccessful();
            } catch (SQLiteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }

        }
    }

    public void saveSingleData(CityList city) {
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            db.beginTransaction();
            try {
                insert_to_table_city(db, TABLE_HE_XUN_CITY_LIST, city);
                db.setTransactionSuccessful();
            } catch (SQLiteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }

        }
    }

    private boolean isZXS(String cityName) {
        if (TextUtils.equals("北京", cityName) ||
                TextUtils.equals("上海", cityName) ||
                TextUtils.equals("天津", cityName) ||
                TextUtils.equals("重庆", cityName) ||
                TextUtils.equals("香港", cityName) ||
                TextUtils.equals("澳门", cityName)) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, ProvinceList> sendDatatoMemory() {
        Map<String, ProvinceList> provinceLists = new HashMap<>(); //define a Map
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_HE_XUN_CITY_LIST, null, null, null, null, null, null); //get all rows
            String currentZXS = null;
            try {
                if (cursor.moveToFirst()) {
                    do {
                        CityList cityList = new CityList(cursor.getString(0),//city
                                cursor.getString(1),//country
                                cursor.getString(2),//cityId
                                cursor.getString(3),//latitude
                                cursor.getString(4),//longitude
                                cursor.getString(5));//province

                        if (isZXS(cityList.getCity())) {
                            currentZXS = cityList.getCity();
                            provinceLists.put(currentZXS, new ProvinceList(currentZXS));
                        }

                        if (TextUtils.equals("直辖市", cityList.getProv()) || TextUtils.equals("特别行政区", cityList.getProv())) {
                            provinceLists.get(currentZXS).getCities().add(cityList);
                        } else {
                            if (provinceLists.get(cityList.getProv()) == null) {
                                provinceLists.put(cityList.getProv(), new ProvinceList(cityList.getProv()));
                            }
                            provinceLists.get(cityList.getProv()).getCities().add(cityList);
                        }
                    } while (cursor.moveToNext());
                } else {
                    Toast.makeText(mContext, "获取地区数据失败!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
            }
        }
        return provinceLists;
    }

    public List<CityList> getChoosedCities() {
        List<CityList> cities = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOOSED_CITY_LIST, null, null, null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    do {
                        CityList city = new CityList(cursor.getString(0),//city
                                cursor.getString(1),//country
                                cursor.getString(2),//cityId
                                cursor.getString(3),//latitude
                                cursor.getString(4),//longitude
                                cursor.getString(5));//province

                        cities.add(city);
                    } while (cursor.moveToNext());
                } else {
                    Toast.makeText(mContext, "您还没有选择城市!", Toast.LENGTH_SHORT).show();
                }
                Log.i("getChoosedCities size", String.valueOf(cities.size()));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
            }
        }
        return cities;
    }

    public long saveChoosedCity(CityList city) {
        long insertResult = -1;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOOSED_CITY_LIST,
                    null,
                    SECTION_CITY_ID + "=" + "\"" + city.getId() + "\"", null, null, null, null);
            try {
                db.beginTransaction();
                if (cursor.moveToFirst() == false) {
                    insertResult = insert_to_table_city(db, TABLE_CHOOSED_CITY_LIST, city);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                cursor.close();
                db.close();
            }
        }
        return insertResult;
    }

    public int deleteChoosedCity(String cityId) {
        int result = 0;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                result = db.delete(TABLE_CHOOSED_CITY_LIST, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
        return result;
    }

    public long cacheWeather(String cityName, String cityId, String weatherJson) {

        long insertResult = -1;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_WEATHER_CACHE,
                    null,
                    SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                    null, null, null, null);
            try {
                db.beginTransaction();
                if (!cursor.moveToFirst()) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SECTION_CITY, cityName);
                    contentValues.put(SECTION_CITY_ID, cityId);
                    contentValues.put(SECTION_WEATHER_JSON, weatherJson);
                    insertResult = db.insert(TABLE_WEATHER_CACHE, null, contentValues);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                cursor.close();
                db.close();
            }
        }
        return insertResult;
    }

    public long cacheWeathers(ArrayList<String> weatherJsons) {

        long insertResult = -1;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                for (String weatherJson: weatherJsons
                        ) {
                    Gson gson = new Gson();
                    WeatherInfo weatherInfo = gson.fromJson(weatherJson, WeatherInfo.class);
                    String cityId = weatherInfo.heWeatherDS0300.get(0).basic.id;
                    String cityName = weatherInfo.heWeatherDS0300.get(0).basic.city;
                    Log.i("cacheWeathers",cityName);
                    Cursor cursor = db.query(TABLE_WEATHER_CACHE,
                            null,
                            SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(SECTION_CITY, cityName);
                        contentValues.put(SECTION_CITY_ID, cityId);
                        contentValues.put(SECTION_WEATHER_JSON, weatherJson);
                        insertResult = db.insert(TABLE_WEATHER_CACHE, null, contentValues);
                    }
                    cursor.close();
                }

                db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                    db.close();
                }
            }
        return insertResult;
    }


    public int syncdelete(String cityId) {
        int result = 0,result1=0,result2=0;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                result1 = db.delete(TABLE_WEATHER_CACHE, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                result2 = db.delete(TABLE_CHOOSED_CITY_LIST, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
        if(result1==0 || result2==0) {
            result=0;
        }
        else {
            result=result1+result2;
        }
        return result;
    }

    public int deleteCachedWeather(String cityId) {
        int result = 0;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                result = db.delete(TABLE_WEATHER_CACHE, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
        return result;
    }

    public int updateCachedWeather(String cityId, String weatherJson) {
        //实例化常量值
        int result = 0;
        ContentValues cValue = new ContentValues();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            cValue.put(SECTION_WEATHER_JSON, weatherJson);
            try {
                db.beginTransaction();
                //调用update()方法插入数据
                result = db.update(TABLE_WEATHER_CACHE, cValue, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
        return result;
    }

    public String getCachedWeather(String cityId) {
        String weatherJson = null;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_WEATHER_CACHE, null,
                    SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                    null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    weatherJson = cursor.getString(2);
                } else {
                    //Toast.makeText(mContext,"您还没有选择城市!",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
            }
        }
        return weatherJson;
    }

    public List<String> getCachedWeathers() {
        ArrayList<String> weatherJson = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_WEATHER_CACHE, null,
                    null,
                    null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    do {
                        weatherJson.add(cursor.getString(2));
                    }while(cursor.moveToNext());
                } else {
                    //Toast.makeText(mContext,"您还没有选择城市!",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
            }
        }
        return weatherJson;
    }
}

//class CityListOperations extends SQLiteOpenHelper {
//    public static final String DB_NAME = "cityList.db";
//    public static final String TABLE_HE_XUN_CITY_LIST = "HeXunCityList";
//    public static final String TABLE_CHOOSED_CITY_LIST = "ChoosedCityList";
//    public static final String TABLE_WEATHER_CACHE = "ChoosedCityWeatherCache";
//    public static final String SECTION_CITY = "city";
//    public static final String SECTION_COUNTRY = "country";
//    public static final String SECTION_CITY_ID = "cityId";
//    public static final String SECTION_LATITUDE = "latitude";
//    public static final String SECTION_LONGITUDE = "longitude";
//    public static final String SECTION_PROVINCE = "province";
//    public static final String SECTION_WEATHER_JSON= "weather_json";
//
//    Context mContext;
//
//    public CityListOperations(Context context) {
//        super(context, DB_NAME,null,1);
//        mContext=context;
//    }
////    private String city; //city
////    private String cnty; //country
////    private String id;   //cityId
////    private String lat;  //latitude
////    private String lon;  //longitude
////    private String prov; //province
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("create table " + TABLE_HE_XUN_CITY_LIST + "(" + SECTION_CITY + " varchar(20) not null, " +
//                SECTION_COUNTRY + " varchar(20) not null, " +
//                SECTION_CITY_ID + " varchar(20) not null, " +
//                SECTION_LATITUDE + " varchar(20) not null, " +
//                SECTION_LONGITUDE + " varchar(20) not null, " +
//                SECTION_PROVINCE + " varchar(20) not null)");
//
//
//        db.execSQL("create table " + TABLE_CHOOSED_CITY_LIST + "(" + SECTION_CITY + " varchar(20) not null, " +
//                SECTION_COUNTRY + " varchar(20) not null, " +
//                SECTION_CITY_ID + " varchar(20) not null, " +
//                SECTION_LATITUDE + " varchar(20) not null, " +
//                SECTION_LONGITUDE + " varchar(20) not null, " +
//                SECTION_PROVINCE + " varchar(20) not null)");
//
//        db.execSQL("create table " + TABLE_WEATHER_CACHE + "(" + SECTION_CITY + " varchar(20) not null, " +
//                SECTION_CITY_ID + " varchar(20) not null, " +
//                SECTION_WEATHER_JSON+" text"+")");
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        Log.i("MainActivity","onUpgrade");
//    }
//
//    public long insert_to_table_city(SQLiteDatabase db, String table_name,CityList city) {
//        //实例化常量值
//        ContentValues cValue = new ContentValues();
//        //添加用户名
//        cValue.put(SECTION_CITY,city.getCity());
//        //添加密码
//        cValue.put(SECTION_COUNTRY,city.getCnty());
//        //
//        cValue.put(SECTION_CITY_ID,city.getId());
//        //
//        cValue.put(SECTION_LATITUDE,city.getLat());
//        //
//        cValue.put(SECTION_LONGITUDE,city.getLon());
//        //
//        cValue.put(SECTION_PROVINCE,city.getProv());
//
//        //调用insert()方法插入数据
//        return db.insert(table_name,null,cValue);
//    }
//
//    public void insert_to_table_cityLists(SQLiteDatabase db, String table_name,List<CityList> cityList) {
//        for (CityList city: cityList
//             ) {
//            insert_to_table_city(db,table_name,city);
//        }
//    }
//
//    public void saveData(SQLiteDatabase db,List<CityList> cityLists) {
//        insert_to_table_cityLists(db,TABLE_HE_XUN_CITY_LIST,cityLists);
//    }
//    private boolean isZXS(String cityName) {
//        if(TextUtils.equals("北京",cityName) ||
//                TextUtils.equals("上海",cityName) ||
//                TextUtils.equals("天津",cityName) ||
//                TextUtils.equals("重庆",cityName) ||
//                TextUtils.equals("香港",cityName) ||
//                TextUtils.equals("澳门",cityName)) {
//            return true;
//        }
//        else {
//            return false;
//        }
//    }
//    public Map<String,ProvinceList> sendDatatoMemory(SQLiteDatabase db) {
//        Cursor cursor = db.query(TABLE_HE_XUN_CITY_LIST,null,null,null,null,null,null); //get all rows
//        Map<String,ProvinceList> provinceLists = new HashMap<>(); //define a Map
//        String currentZXS=null;
//        if(cursor.moveToFirst()) {
//            do {
//                CityList cityList = new CityList(cursor.getString(0),//city
//                        cursor.getString(1),//country
//                        cursor.getString(2),//cityId
//                        cursor.getString(3),//latitude
//                        cursor.getString(4),//longitude
//                        cursor.getString(5));//province
//
//                if(isZXS(cityList.getCity())) {
//                    currentZXS = cityList.getCity();
//                    provinceLists.put(currentZXS,new ProvinceList(currentZXS));
//                }
//
//                if(TextUtils.equals("直辖市",cityList.getProv()) || TextUtils.equals("特别行政区",cityList.getProv())) {
//                    provinceLists.get(currentZXS).getCities().add(cityList);
//                }
//                else {
//                    if (provinceLists.get(cityList.getProv()) == null) {
//                        provinceLists.put(cityList.getProv(), new ProvinceList(cityList.getProv()));
//                    }
//                    provinceLists.get(cityList.getProv()).getCities().add(cityList);
//                }
//            }while(cursor.moveToNext());
//        }
//        else {
//            Toast.makeText(mContext,"获取地区数据失败!",Toast.LENGTH_LONG).show();
//        }
//        cursor.close();
//        return provinceLists;
//    }
//
//    public List<CityList> getChoosedCities(SQLiteDatabase db) {
//        Cursor cursor = db.query(TABLE_CHOOSED_CITY_LIST,null,null,null,null,null,null); //get all rows
//        List<CityList> cities = new ArrayList<>();
//        if(cursor.moveToFirst()) {
//            do {
//                CityList city = new CityList(cursor.getString(0),//city
//                        cursor.getString(1),//country
//                        cursor.getString(2),//cityId
//                        cursor.getString(3),//latitude
//                        cursor.getString(4),//longitude
//                        cursor.getString(5));//province
//
//                cities.add(city);
//            }while(cursor.moveToNext());
//        }
//        else {
//            Toast.makeText(mContext,"您还没有选择城市!",Toast.LENGTH_SHORT).show();
//        }
//        Log.i("getChoosedCities size",String.valueOf(cities.size()));
//        cursor.close();
//        return cities;
//    }
//
//    public long saveChoosedCity(SQLiteDatabase db,CityList city) {
//        long insertResult=-1;
//        Cursor cursor = db.query(CityListOperations.TABLE_CHOOSED_CITY_LIST,
//                null,
//                CityListOperations.SECTION_CITY_ID + "=" +"\""+city.getId()+"\"",null,null,null,null);
//        if(cursor.moveToFirst()==false) {
//            insertResult=insert_to_table_city(db, CityListOperations.TABLE_CHOOSED_CITY_LIST, city);
//        }
//        cursor.close();
//        return insertResult;
//    }
//    public int  deleteChoosedCity(SQLiteDatabase db,String cityId) {
//        return db.delete(TABLE_CHOOSED_CITY_LIST,SECTION_CITY_ID+"="+"\""+cityId+"\"",null);
//    }
//
//    public long cacheWeather(SQLiteDatabase db,String cityName,String cityId,String weatherJson) {
//        long insertResult=-1;
//        Cursor cursor = db.query(TABLE_WEATHER_CACHE,null,SECTION_CITY_ID+"="+"\""+cityId+"\"",null,null,null,null); //get all rows
//        if(!cursor.moveToFirst()) {
//            ContentValues contentValues = new ContentValues();
//            contentValues.put(SECTION_CITY, cityName);
//            contentValues.put(SECTION_CITY_ID, cityId);
//            contentValues.put(SECTION_WEATHER_JSON, weatherJson);
//            insertResult=db.insert(TABLE_WEATHER_CACHE, null, contentValues);
//        }
//        cursor.close();
//        return insertResult;
//    }
//
//    public int deleteCachedWeather(SQLiteDatabase db,String cityId) {
//        return db.delete(TABLE_WEATHER_CACHE,SECTION_CITY_ID+"="+"\""+cityId+"\"",null);
//    }
//
//    public int updateCachedWeather(SQLiteDatabase db,String cityId,String weatherJson) {
//        //实例化常量值
//        ContentValues cValue = new ContentValues();
//
//        cValue.put(SECTION_WEATHER_JSON,weatherJson);
//        //调用insert()方法插入数据
//        return db.update(TABLE_WEATHER_CACHE,cValue,SECTION_CITY_ID+"="+"\""+cityId+"\"",null);
//    }
//
//    public String getCachedWeather(SQLiteDatabase db,String cityId) {
//        Cursor cursor = db.query(TABLE_WEATHER_CACHE,null,SECTION_CITY_ID+"="+"\""+cityId+"\"",null,null,null,null); //get all rows
//        String weatherJson=null;
//        if(cursor.moveToFirst()) {
//            weatherJson = cursor.getString(2);
//        }
//        else {
//            //Toast.makeText(mContext,"您还没有选择城市!",Toast.LENGTH_SHORT).show();
//        }
//        cursor.close();
//        return weatherJson;
//    }
//}