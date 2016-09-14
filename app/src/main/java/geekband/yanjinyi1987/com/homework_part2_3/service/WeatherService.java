package geekband.yanjinyi1987.com.homework_part2_3.service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import geekband.yanjinyi1987.com.homework_part2_3.ChooseCityActivity;
import geekband.yanjinyi1987.com.homework_part2_3.MainActivity;
import geekband.yanjinyi1987.com.homework_part2_3.ManageCityActivity;

/**
 * Created by lexkde on 16-9-11.
 */

public class WeatherService extends Service {
    public static final String TAG = "WeatherService";
    private boolean bindToMainActivity = false;
    private boolean bindToManageCityActivity = false;
    private boolean bindToChooseCityActivity = false;

    private Handler mHttpHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                default:
                    super.handleMessage(msg);
            }
        }
    };
    class ClientHandler extends  Handler {
        @Override
        public void handleMessage(Message msg) {
            int startIndex=0,data_length=0,endIndex=0;
            switch(msg.what) {
                case MainActivity.SEND_MESSENGER_TO_SERVICE_MainActivity:
                    Log.i(TAG,"Got MainActivity's messenger");
                    mClientMessenger_MainActivity = (Messenger) msg.obj;
                    bindToMainActivity= true;
                    if(mClientMessenger_MainActivity==null) {
                        Log.i(TAG,"Got MainActivity's messenger failed");
                        bindToMainActivity=false;
                    }
                    break;

                case MainActivity.SEND_MESSENGER_TO_SERVICE_ManageCityActivity:
                    Log.i(TAG,"Got ManageCityActivity's messenger");
                    mClientMessenger_ManageCityActivity = (Messenger) msg.obj;
                    bindToManageCityActivity= true;
                    if(mClientMessenger_ManageCityActivity ==null) {
                        Log.i(TAG,"Got ManageCityActivity's messenger failed");
                        bindToManageCityActivity=false;
                    }
                    break;

                case MainActivity.SEND_MESSENGER_TO_SERVICE_ChooseCityActivity:
                    Log.i(TAG,"Got ChooseCityActivity's messenger");
                    mClientMessenger_ChooseCityActivity = (Messenger) msg.obj;
                    bindToChooseCityActivity= true;
                    if(mClientMessenger_ChooseCityActivity==null) {
                        Log.i(TAG,"Got ChooseCityActivity's messenger failed");
                        bindToChooseCityActivity=false;
                    }
                    break;
                case MainActivity.GET_GLOBAL_CITY_LIST_FROM_WEB:
                    Log.i(TAG,"GET_GLOBAL_CITY_LIST_FROM_WEB");
                    Message CITY_LIST_returnToMainactivy_msg = new Message();
                    SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.GLOBAL_SETTINGS
                            ,MODE_PRIVATE);
                    Boolean isCityListInDatabase = sharedPreferences.getBoolean(MainActivity.IS_CITY_LIST_IN_DATABASE
                            ,false);
                    if(!saveCityListToDatabaseAndGenerateObject() && isCityListInDatabase==false) {
                        CITY_LIST_returnToMainactivy_msg.what = MainActivity.GLOBAL_FAULT;
                    }
                    else {
                        CITY_LIST_returnToMainactivy_msg.what = MainActivity.GOT_GLOBAL_CITY_LIST;
                        setDatatoSharedPreferences(true,MainActivity.GLOBAL_SETTINGS,
                                MainActivity.IS_CITY_LIST_IN_DATABASE
                                );

                    }
                    try {
                        mClientMessenger_MainActivity.send(CITY_LIST_returnToMainactivy_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MainActivity.GET_CHOOSED_CITY_WEATHER_FROM_WEB:
                    Log.i(TAG,"GET_CHOOSED_CITY_WEATHER_FROM_WEB");
                    Message CITY_WEATHER_returnToMainActivity_msg = new Message();
                    List<SimpleWeatherInfo> simpleWeatherInfoList = new ArrayList<>();
                    if(!getChoosedCityWeather(chosedCities,simpleWeatherInfoList)) {
                        CITY_WEATHER_returnToMainActivity_msg.what=
                                MainActivity.GET_CHOOSED_CITY_WEATHER_FROM_WEB_FAILED;
                    }
                    else {
                        CITY_WEATHER_returnToMainActivity_msg.what=
                                MainActivity.GET_CHOOSED_CITY_WEATHER_FROM_WEB_SUCCED;
                        CITY_WEATHER_returnToMainActivity_msg.getData().putSerializable("WeatherInfo",
                                (Serializable) simpleWeatherInfoList);
                    }

                    try {
                        mClientMessenger_MainActivity.send(CITY_WEATHER_returnToMainActivity_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MainActivity.GET_CHOOSED_CITY_FROM_DB:
                    Log.i(TAG,"GET_CHOOSED_CITY_FROM_DB");
                    chosedCities= (ArrayList<CityInfo>) getChoosedCityFromDB();
                    int fromWhich = msg.getData().getInt("Identify_ID");
                    Message CHOSED_CITY_returnToMainActivity_msg = new Message();
                    if(chosedCities!=null) {
                        CHOSED_CITY_returnToMainActivity_msg.what =
                                MainActivity.GET_CHOOSED_CITY_FROM_DB_SUCCED;
                        CHOSED_CITY_returnToMainActivity_msg.getData().putSerializable("CHOSED_CITY_LIST",
                                (Serializable) chosedCities);
                    }
                    else {
                        CHOSED_CITY_returnToMainActivity_msg.what =
                                MainActivity.GET_CHOOSED_CITY_FROM_DB_FAILED;
                    }
                    if(fromWhich==1) {
                        try {
                            mClientMessenger_ManageCityActivity.send(CHOSED_CITY_returnToMainActivity_msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            mClientMessenger_MainActivity.send(CHOSED_CITY_returnToMainActivity_msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MainActivity.RE_TRANSFER:
                    startIndex = endIndex - data_length;
                case MainActivity.REQUEST_START_TRANSFER:

                    Message cityInfos_data_package = new Message();

                    cityInfos_data_package.what =
                            ChooseCityActivity.SEND_DATA_PACKAGE;
                    if(data_length==0) {
                        endIndex = chosedCities.size();
                    }
                    else {
                        endIndex = startIndex+data_length;
                        if(endIndex>chosedCities.size()) {
                            endIndex = chosedCities.size();
                        }
                    }
                    cityInfos_data_package.getData().putSerializable("DATA_PACKAGE",
                            (Serializable) (new ArrayList<>(chosedCities.subList(startIndex,endIndex))));
                    try {
                        mClientMessenger_ChooseCityActivity.send(cityInfos_data_package);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    startIndex = endIndex;
                    break;
                case MainActivity.TRANSFER_DONE:
                    Message CHOSED_CITY_returnToMainActivity_Done_msg = new Message();
                    CHOSED_CITY_returnToMainActivity_Done_msg.what =
                            ChooseCityActivity.GET_GLOBAL_CITY_FROM_DB_DONE;
                    try {
                        mClientMessenger_ChooseCityActivity.send(CHOSED_CITY_returnToMainActivity_Done_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MainActivity.GET_GLOBAL_CITY_LIST_FROM_DB:
                    ////data is too large, split it to small pieces and send
                    Log.i(TAG,"GET_GLOBAL_CITY_LIST_FROM_DB");
                    Message GET_GLOBAL_CITY_LIST_FROM_DB_returnToChooseCityActivity_msg = new Message();
                    Map<String,WeatherService.ProvinceList> AllChinaCities;
                    AllChinaCities=initCityData();
                    Log.i(TAG,"GET_GLOBAL_CITY_LIST_FROM_DB  Processing...");
                    GET_GLOBAL_CITY_LIST_FROM_DB_returnToChooseCityActivity_msg.what =
                            ChooseCityActivity.SEND_DATA_TO_MEMORY;

                    //send data size first
                    GET_GLOBAL_CITY_LIST_FROM_DB_returnToChooseCityActivity_msg.getData().putInt("DATA_SIZE",AllChinaCities.size());
                    data_length = AllChinaCities.size()/5;
                    GET_GLOBAL_CITY_LIST_FROM_DB_returnToChooseCityActivity_msg.getData().putSerializable("GLOBAL_CITY_LIST",
                            (Serializable) AllChinaCities);
                    try {
                        mClientMessenger_ChooseCityActivity.send(GET_GLOBAL_CITY_LIST_FROM_DB_returnToChooseCityActivity_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MainActivity.WRITE_CHOSEN_CITY_TO_DB:
                    Log.i(TAG,"WRITE_CHOSEN_CITY_TO_DB");
                    Message WRITE_CHOSEN_CITY_TO_DB_returnToChooseCityActivity_msg = new Message();
                    WRITE_CHOSEN_CITY_TO_DB_returnToChooseCityActivity_msg.what =
                            ChooseCityActivity.WRITE_FINISHED;
                    Bundle bundle = new Bundle();
                    bundle = msg.getData().getBundle("Bundle");
                    CityInfo city = (CityInfo) bundle.getSerializable("WeatherService.CityInfo");
                    WRITE_CHOSEN_CITY_TO_DB_returnToChooseCityActivity_msg.getData().putSerializable("CurrentCity",
                            city);
                    if(writeChoosedCitytoDatabase(city)) {
                        try {
                            mClientMessenger_ChooseCityActivity.send(WRITE_CHOSEN_CITY_TO_DB_returnToChooseCityActivity_msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MainActivity.DELETE_CHOSEN_CITY_FROM_DB:
                    Log.i(TAG,"DELETE_CHOSEN_CITY_FROM_DB");
                    Message DELETE_CHOSEN_CITY_FROM_DB_returnToManageCityActivity_msg = new Message();

                    String cityId =msg.getData().getString("CityId");
                    Log.i(TAG,cityId);
                    if(deleteChoosedCitiesFromDatabase(cityId)) {
                        DELETE_CHOSEN_CITY_FROM_DB_returnToManageCityActivity_msg.what =
                                ManageCityActivity.DELETE_CHOSEN_CITY_FROM_DB_SUCCED;
                    }
                    else {
                        DELETE_CHOSEN_CITY_FROM_DB_returnToManageCityActivity_msg.what =
                                ManageCityActivity.DELETE_CHOSEN_CITY_FROM_DB_FAILED;
                    }
                    try {
                        mClientMessenger_ManageCityActivity.send(DELETE_CHOSEN_CITY_FROM_DB_returnToManageCityActivity_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    ArrayList<WeatherService.CityInfo> chosedCities;

    private Messenger mClientMessenger_MainActivity;
    private Messenger mClientMessenger_ChooseCityActivity;
    private Messenger mClientMessenger_ManageCityActivity;
    Messenger mMessenger = new Messenger(new ClientHandler());


    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"onBind");
        return mMessenger.getBinder();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onBind");
        super.onDestroy();
    }



//    ProvinceList -> province1 province2 ...
//                    city1     city1
//                    city2     city2
//                    city3     city3
//                    ...       ...

    public static class CityList {
        public List<CityInfo> city_info;
    }
    public static class ProvinceList implements Serializable{
        private String provinceName;
        private List<CityInfo> cities;

        public ProvinceList(String provinceName) {
            this.provinceName = provinceName;
            cities = new ArrayList<>();
        }

        public String getProvinceName() {
            return provinceName;
        }

        public List<CityInfo> getCities() {
            return cities;
        }

        public void setProvinceName(String provinceName) {
            this.provinceName = provinceName;
        }

        public void setCities(List<CityInfo> cities) {
            this.cities = cities;
        }
    }

    public static class CityInfo implements Serializable{
        private String city; //city
        private String cnty; //country
        private String id;   //cityId
        private String lat;  //latitude
        private String lon;  //longitude
        private String prov; //province

        public CityInfo() {

        }

        public CityInfo(String city) {
            this(city,null,null,null,null,null);
        }

        public CityInfo(String city, String cnty, String id, String lat, String lon, String prov) {
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

    public static class SimpleWeatherInfo implements Serializable{
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

    //no need to update UI
    private boolean saveCityListToDatabaseAndGenerateObject() {
        boolean result = false;

        final WeatherHttp weatherHttp = new WeatherHttp(mHttpHandler);
        Thread threadHttp = new Thread(new Runnable() {
            @Override
            public void run() {
                weatherHttp.requestWithoutThread(WeatherHttp.GET_CITY_LIST,"allchina");
            }
        });
        threadHttp.start();
        try {
            threadHttp.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg = WeatherHttp.mHttpResultMsg;
        Log.i(TAG, "We got "+(String) msg.obj);
        Log.i(TAG,"We got "+String.valueOf(msg.what));
        if (msg.what == WeatherHttp.GET_CITY_LIST) {
            Gson gson = new Gson();
            WeatherService.CityList cityList = gson.fromJson((String) msg.obj, WeatherService.CityList.class);

            List<WeatherService.CityInfo> cityInfos = cityList.city_info;

            CityListOperations cityListOperations = new CityListOperations(WeatherService.this);

            try {
                if (cityListOperations.saveData(cityInfos)) {
                    //set SharedPreference isCityListInDatabase = true
                    Log.i(TAG,"Write SharedPreferences IS_CITY_LIST_IN_DATABASE");
                    setDatatoSharedPreferences(true,
                            MainActivity.GLOBAL_SETTINGS,
                            MainActivity.IS_CITY_LIST_IN_DATABASE);
                    result = true;
                } else {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                return result;
            }
        }
        else {
            return false;
        }
    }

    private boolean setDatatoSharedPreferences(boolean data,String filename,String key) {
        boolean result=false;
        try {
            SharedPreferences sharedPreferences = WeatherService.this.getSharedPreferences(filename, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(key, data);
            editor.apply();
            result=true;
        }catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            return result;
        }
    }


    private boolean getChoosedCityWeather(List<WeatherService.CityInfo> chosenCityInfos,
                                          List<SimpleWeatherInfo> simpleWeatherInfoList) {
        boolean getAllWeathers=true;
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.GLOBAL_SETTINGS, MODE_PRIVATE);
        Boolean isCityWeatherCached = sharedPreferences.getBoolean(MainActivity.IS_CITY_WEATHER_CACHED, false);
        HeXunWeatherInfo heXunWeatherInfo;
        Gson gson = new Gson();
        CityListOperations cityListOperations = new CityListOperations(WeatherService.this);
        //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()

        //List<SimpleWeatherInfo> simpleWeatherInfoList = new ArrayList<>();
        Message msg_return = new Message();
        if (!isCityWeatherCached) {
            ArrayList<String> weatherJsons = new ArrayList<>();
            for (final WeatherService.CityInfo city : chosenCityInfos
                    ) {
                final WeatherHttp weatherHttp = new WeatherHttp(mHttpHandler);
                Thread threadHttp = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        weatherHttp.requestWithoutThread(WeatherHttp.GET_CITY_WEATHER, city.getId());
                    }
                });
                try {
                    threadHttp.start();
                    threadHttp.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msg = WeatherHttp.mHttpResultMsg;
                if (msg.what != WeatherHttp.NETWORK_ERROR) {
                    heXunWeatherInfo = gson.fromJson((String) msg.obj, HeXunWeatherInfo.class);
                    if (heXunWeatherInfo.heWeatherDS0300.get(0).status.equals("ok")) { //check status here
                        weatherJsons.add((String) msg.obj);
                        Log.i("MainActivity", String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.fl));
                        simpleWeatherInfoList.add(new SimpleWeatherInfo(
                                city.getCity(),
                                city.getId(),
                                heXunWeatherInfo.heWeatherDS0300.get(0).now.cond.txt,
                                String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.tmp)
                        ));
                    } else {
                        getAllWeathers = false;
                        break;
                    }
                } else {
                    getAllWeathers = false;
                    Log.i("MainActivity", "Error json");
                    break;
                }
            }
            if(getAllWeathers==true) {
                Log.i("getChoosedWeather",String.valueOf(weatherJsons.size()));
                Log.i("getChoosedWeather ","choosedCityInfos"+String.valueOf(chosenCityInfos.size()));

                if(cityListOperations.cacheWeathers(weatherJsons)>0) {
                    setDatatoSharedPreferences(true,
                            MainActivity.GLOBAL_SETTINGS,
                            MainActivity.IS_CITY_WEATHER_CACHED);
                }
            }
        }
        else {
            //read from database
            ArrayList<String> weatherJsons = (ArrayList<String>) cityListOperations.getCachedWeathers();
            if (weatherJsons!=null && weatherJsons.size() != 0) {
                for (String weatherJson:weatherJsons
                        ) {
                    heXunWeatherInfo = gson.fromJson(weatherJson, HeXunWeatherInfo.class);
                    if(heXunWeatherInfo!=null) {
                        simpleWeatherInfoList.add(new SimpleWeatherInfo(
                                heXunWeatherInfo.heWeatherDS0300.get(0).basic.city,
                                heXunWeatherInfo.heWeatherDS0300.get(0).basic.id,
                                heXunWeatherInfo.heWeatherDS0300.get(0).now.cond.txt,
                                String.valueOf(heXunWeatherInfo.heWeatherDS0300.get(0).now.tmp)
                        ));
                    }
                    else {
                        //
                        Log.i(TAG,"DB not contain the weather info");
                    }
                }
            }
            else {
                getAllWeathers=false;
            }
        }
        return getAllWeathers;
    }

    private List<WeatherService.CityInfo> getChoosedCityFromDB() {
        boolean result = false;
        List<WeatherService.CityInfo> cityInfos;
        CityListOperations cityListOperations = new CityListOperations(WeatherService.this);
        //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()
        cityInfos = cityListOperations.getChoosedCities();
        return cityInfos;
    }

    private Map<String,WeatherService.ProvinceList> initCityData() {

        //Message msg = new Message();
        CityListOperations cityListOperations = new CityListOperations(WeatherService.this);
        //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();

        Map<String,WeatherService.ProvinceList> cityLists = cityListOperations.sendDatatoMemory();
        return cityLists;
    }

    private boolean writeChoosedCitytoDatabase(final WeatherService.CityInfo city) {
        Message msg = new Message();
        Log.i("ChoosedCityActivity","save data");
        long result=-1;
        CityListOperations cityListOperations = new CityListOperations(WeatherService.this);
        //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();
        result=cityListOperations.saveChoosedCity(city);
        if(result==-1) {
            return false;
        }
        else {
            return true;
        }
    }

    public boolean deleteChoosedCitiesFromDatabase(final String cityId) {
        long result=0;
        CityListOperations cityListOperations = new CityListOperations(WeatherService.this);
        //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();
        result = cityListOperations.syncdelete(cityId);
        if(result<=0) {
            return false;
        }
        else {
            return true;
        }
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
                    CityListOperations.TABLE_HE_XUN_CITY_LIST +
                    "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_COUNTRY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_LATITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_LONGITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_PROVINCE + " varchar(20) not null"+
                    ")"
            );


            db.execSQL("create table " +
                    CityListOperations.TABLE_CHOSEN_CITY_LIST +
                    "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_COUNTRY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_LATITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_LONGITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_PROVINCE + " varchar(20) not null, "+
                    CityListOperations.SECTION_WEATHER_JSON + " text null" +
                    ")"
            );
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
    public static final String TABLE_CHOSEN_CITY_LIST = "ChoosedCityList";
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

    public long insert_to_table_city(SQLiteDatabase db, String table_name, WeatherService.CityInfo city) {
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

    public boolean insert_to_table_cityLists(SQLiteDatabase db, String table_name, List<WeatherService.CityInfo> cityList) {
        for (WeatherService.CityInfo city : cityList
                ) {
            if(insert_to_table_city(db, table_name, city)==-1) {
                return false;
            }
        }
        return true;
    }

    public boolean saveData(List<WeatherService.CityInfo> cityLists) {
        Log.i(this.getClass().getSimpleName(),"save global city list into DB");
        synchronized (helper) {
            boolean result = false;
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM "+TABLE_HE_XUN_CITY_LIST);
                if(insert_to_table_cityLists(db, TABLE_HE_XUN_CITY_LIST, cityLists)) {
                    db.setTransactionSuccessful();
                    result=true;
                }
                else {
                    result = false;
                }
            } catch (SQLiteException e) {
                result = false;
                e.printStackTrace();
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return result;
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

    public Map<String, WeatherService.ProvinceList> sendDatatoMemory() {
        Log.i(this.getClass().getSimpleName(),"read global city list from db");
        Map<String, WeatherService.ProvinceList> provinceLists = new HashMap<>(); //define a Map
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_HE_XUN_CITY_LIST, null, null, null, null, null, null); //get all rows
            String currentZXS = null;
            try {
                if (cursor.moveToFirst()) {
                    do {
                        WeatherService.CityInfo cityInfo = new WeatherService.CityInfo(cursor.getString(0),//city
                                cursor.getString(1),//country
                                cursor.getString(2),//cityId
                                cursor.getString(3),//latitude
                                cursor.getString(4),//longitude
                                cursor.getString(5));//province

                        if (isZXS(cityInfo.getCity())) {
                            currentZXS = cityInfo.getCity();
                            provinceLists.put(currentZXS, new WeatherService.ProvinceList(currentZXS));
                        }

                        if (TextUtils.equals("直辖市", cityInfo.getProv()) || TextUtils.equals("特别行政区", cityInfo.getProv())) {
                            provinceLists.get(currentZXS).getCities().add(cityInfo);
                        } else {
                            if (provinceLists.get(cityInfo.getProv()) == null) {
                                provinceLists.put(cityInfo.getProv(), new WeatherService.ProvinceList(cityInfo.getProv()));
                            }
                            provinceLists.get(cityInfo.getProv()).getCities().add(cityInfo);
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

    public List<WeatherService.CityInfo> getChoosedCities() {
        List<WeatherService.CityInfo> cities = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST, null, null, null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    do {
                        WeatherService.CityInfo city = new WeatherService.CityInfo(cursor.getString(0),//city
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
            } catch (Exception e) {
                e.printStackTrace();
                cities = null;
            } finally {
                cursor.close();
                db.close();
                return cities;
            }
        }
    }

    public long saveChoosedCity(WeatherService.CityInfo city) {
        long insertResult = -1;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST,
                    null,
                    SECTION_CITY_ID + "=" + "\"" + city.getId() + "\"", null, null, null, null);
            try {
                db.beginTransaction();
                if (cursor.moveToFirst() == false) {
                    insertResult = insert_to_table_city(db, TABLE_CHOSEN_CITY_LIST, city);
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
                result = db.delete(TABLE_CHOSEN_CITY_LIST, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
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
                    insertResult=-1;
                    Gson gson = new Gson();
                    HeXunWeatherInfo heXunWeatherInfo = gson.fromJson(weatherJson, HeXunWeatherInfo.class);
                    String cityId = heXunWeatherInfo.heWeatherDS0300.get(0).basic.id;
                    String cityName = heXunWeatherInfo.heWeatherDS0300.get(0).basic.city;
                    Log.i("cacheWeathers",cityName);
                    Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST,
                            null,
                            SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(SECTION_WEATHER_JSON, weatherJson);
                        insertResult = db.update(TABLE_CHOSEN_CITY_LIST,
                                contentValues,
                                SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                                null);
                    }
                    cursor.close();
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return insertResult;
            }
        }

    }


    public int syncdelete(String cityId) {
        int result = 0;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                result = db.delete(TABLE_CHOSEN_CITY_LIST, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return result;
            }
        }
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
                result = db.update(TABLE_CHOSEN_CITY_LIST, cValue, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return result;
            }
        }
    }

    public List<String> getCachedWeathers() {
        ArrayList<String> weatherJson = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST, null,
                    null,
                    null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    do {
                        weatherJson.add(cursor.getString(6));
                    }while(cursor.moveToNext());
                } else {
                    //Toast.makeText(mContext,"您还没有选择城市!",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
                return weatherJson;
            }
        }
    }
}

class WeatherHttp{
    public static final String APIKey = "3e9f8bab0ec34c37b49f4cb1652c956d";
    public static final int GET_CITY_WEATHER=1;
    public static final int GET_WEATHER_CONDITION=2;
    public static final int GET_CITY_LIST=3;
    public static final int NETWORK_ERROR = -1;
    public static Message mHttpResultMsg = new Message();
    private Handler mHandler;
    public WeatherHttp() {
        this(null);
    }

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

    public void requestWithThread(final int requestType, final String addition_data){
        synchronized (mHttpResultMsg) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String urlStr = null;
                    URL url = null;
                    HttpURLConnection httpsURLConnection = null;
                    InputStream jsonIn = null;
                    Message msg = mHttpResultMsg;
                    msg.what = 0; //iniital
                    StringBuilder response = null;
                    Log.i("Network", "start");
                    switch (requestType) {
                        case GET_CITY_WEATHER:
                            urlStr = getWeatherUrl(addition_data);
                            msg.what = 1;
                            break;
                        case GET_WEATHER_CONDITION:
                            urlStr = getWeatherConditionUrl(addition_data);
                            Log.i("request", urlStr);
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
                        httpsURLConnection = (HttpsURLConnection) url.openConnection();
                        httpsURLConnection.setRequestMethod("GET");
                        httpsURLConnection.setConnectTimeout(8000);
                        httpsURLConnection.setReadTimeout(8000);
                        jsonIn = httpsURLConnection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(jsonIn));
                        response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    /*return json data*/
                    } catch (Exception e) {
                        Log.i("WeatherHttp", "IOException in getInputStream");
                        msg.what = NETWORK_ERROR; //error
                        e.printStackTrace();
                    } finally {
                        if (httpsURLConnection != null) {
                            httpsURLConnection.disconnect();
                        }
                    }

                    if (msg.what != NETWORK_ERROR) {
                        //msg.what = 0; //success
                        msg.obj = response.toString();
                        Log.i("request", (String) msg.obj);
                    }
                    //mHttpResultMsg = msg;
                }

            }).start();
        }
    }

    public void requestWithoutThread(final int requestType, final String addition_data){
        String urlStr = null;
        URL url = null;
        HttpURLConnection httpsURLConnection = null;
        InputStream jsonIn = null;
        Message msg = mHttpResultMsg;
        msg.what = 0; //iniital
        StringBuilder response = null;
        Log.i("Network", "start");
        switch (requestType) {
            case GET_CITY_WEATHER:
                urlStr = getWeatherUrl(addition_data);
                msg.what = 1;
                break;
            case GET_WEATHER_CONDITION:
                urlStr = getWeatherConditionUrl(addition_data);
                Log.i("request", urlStr);
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
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setConnectTimeout(8000);
            httpsURLConnection.setReadTimeout(8000);
            jsonIn = httpsURLConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(jsonIn));
            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
                    /*return json data*/
        } catch (Exception e) {
            Log.i("WeatherHttp", "IOException in getInputStream");
            msg.what = NETWORK_ERROR; //error
            e.printStackTrace();
        } finally {
            if (httpsURLConnection != null) {
                httpsURLConnection.disconnect();
            }
        }

        if (msg.what != NETWORK_ERROR) {
            //msg.what = 0; //success
            msg.obj = response.toString();
            Log.i("request", (String) msg.obj);
        }
        //mHttpResultMsg = msg;
    }
}