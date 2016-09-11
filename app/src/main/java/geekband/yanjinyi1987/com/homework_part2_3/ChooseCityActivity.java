package geekband.yanjinyi1987.com.homework_part2_3;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ChooseCityActivity extends AppCompatActivity {

    public static final int WRITE_FINISHED = 2;
    Handler mHandler;
    Map<String,ProvinceList> cityLists;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_city);

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        cityLists = (Map<String, ProvinceList>) msg.obj;
                        initListViews(cityLists);
                        break;
                    case WRITE_FINISHED:
                        Log.i("ChoosedCityActivity","start MainActivity");
                        Intent intent = new Intent(ChooseCityActivity.this,MainActivity.class);
                        intent.putExtra("Position",((CityList)msg.obj).getId());
                        startActivityForResult(intent,0); //uiying OnCreate
                        finish();
                        break;
                    default:
                        break;
                }
            }
        };

        initViews();
    }


    @Override
    protected void onDestroy() {
        Log.i(this.getClass().getSimpleName(),"onDestroy");
        Log.i(this.getClass().getSimpleName(),"Send Exit App");
        Intent exitAppIntent = new Intent();
        exitAppIntent.setAction("MainActivity.ExitApp");
        sendBroadcast(exitAppIntent);
        super.onDestroy();
    }

    private void initCityData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Message msg = new Message();
                CityListOperations cityListOperations = new CityListOperations(ChooseCityActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();

                Map<String,ProvinceList> cityLists = cityListOperations.sendDatatoMemory();

                msg.what = 1;
                msg.obj = cityLists;

                mHandler.sendMessage(msg);
                Looper.loop();
            }
        }).start();
    }
    private void initViews() {
        initCityData();
    }
//ExpandableListView
    private void initListViews(final Map<String, ProvinceList> cityLists) {
        final List<City> provinceList = new ArrayList<>();
        for (String prov_name:cityLists.keySet()
             ) {
            provinceList.add(new City(prov_name,R.drawable.acs));

        }

        Collections.sort(provinceList, new Comparator<City>() {
            @Override
            public int compare(City lhs, City rhs) {
               return lhs.getName().compareTo(rhs.getName());
            }
        });

        CityAdapter_Expand cityAdapterM = new CityAdapter_Expand(ChooseCityActivity.this,
                R.layout.parent_list_view_item,
                R.layout.child_list_view_item,
                provinceList,
                cityLists);
        ExpandableListView listView = (ExpandableListView) findViewById(R.id.all_china_cities_listview);
        listView.setGroupIndicator(null);
        listView.setAdapter(cityAdapterM);

        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return false;
            }
        });

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                //pass data to ManageCityActivity
                CityList choosedCity = cityLists.get(provinceList.get(groupPosition).getName()).getCities().get(childPosition);
                writeChoosedCitytoDatabase(choosedCity);
                setDatatoSharedPreferences(false,MainActivity.GLOBAL_SETTINGS,MainActivity.IS_CITY_WEATHER_CACHED);
                return false;
            }
        });

    }

    public void setDatatoSharedPreferences(boolean data,String filename,String key) {
        SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences(filename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key,data);
        editor.apply();
    }

    private void writeChoosedCitytoDatabase(final CityList city) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Message msg = new Message();
                Log.i("ChoosedCityActivity","save data");
                long result=-1;
                CityListOperations cityListOperations = new CityListOperations(ChooseCityActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();
                while((result=cityListOperations.saveChoosedCity(city))==-1) {
                    Log.i("ChooseCityActivity","error");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                msg.what= WRITE_FINISHED;
                msg.obj = city;
                mHandler.sendMessage(msg);
                Looper.loop();
            }
        }).start();
    }
}


class City {
    private String name;
    private int imageId;

    public City(String name,int imageId) {
        this.name=name;
        this.imageId=imageId;
    }

    public String getName() {
        return name;
    }

    public int getImageId() {
        return imageId;
    }
}


class CityAdapter_Expand extends BaseExpandableListAdapter {
    List<City> groupList;
    Map<String,ProvinceList> childMap;
    private int parentResourceId,childResourceId;
    Context mContext;


    public CityAdapter_Expand(Context context,int parentLayoutResourceId,int childLayoutResourceId
            ,List<City> provinces,Map<String,ProvinceList> citiesPerProvince) {
        parentResourceId = parentLayoutResourceId;
        childResourceId = childLayoutResourceId;
        groupList = provinces;
        childMap = citiesPerProvince;
        mContext = context;
    }
    @Override
    public int getGroupCount() {
//        return 0;
        return groupList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
//        return 0;
        return childMap.get(groupList.get(groupPosition).getName()).getCities().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        //return null;
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        //return null;
        return childMap.get(groupList.get(groupPosition).getName()).getCities();
    }

    @Override
    public long getGroupId(int groupPosition) {
        //return 0;
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        //return 0;
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        City city = groupList.get(groupPosition);
        View view;
        ParentViewHolder parentViewHolder;
        if(convertView==null) {
            view = LayoutInflater.from(mContext).inflate(parentResourceId,null);
            parentViewHolder = new ParentViewHolder();
            parentViewHolder.arrowImage = (ImageView) view.findViewById(R.id.listview_item_png);
            parentViewHolder.provinceName = (TextView) view.findViewById(R.id.listview_item_text);
            view.setTag(parentViewHolder); //save the viewHolder in view
        }
        else {
            view =convertView;
            parentViewHolder = (ParentViewHolder) view.getTag();
        }


        parentViewHolder.arrowImage.setImageResource(city.getImageId());
        parentViewHolder.provinceName.setText(city.getName());

        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String cityName = childMap.get(groupList.get(groupPosition).getName()).getCities().get(childPosition).getCity();
        View view;
        ChildViewHolder childViewHolder;
        if(convertView==null) {
            view = LayoutInflater.from(mContext).inflate(childResourceId,null);
            childViewHolder = new ChildViewHolder();
            childViewHolder.cityName = (TextView) view.findViewById(R.id.listview_item_text);
            view.setTag(childViewHolder); //save the viewHolder in view
        }
        else {
            view =convertView;
            childViewHolder = (ChildViewHolder) view.getTag();
        }

        childViewHolder.cityName.setText(cityName);

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        //return false;
        return true;
    }

    class ParentViewHolder {
        TextView provinceName;
        ImageView arrowImage;
    }

    class ChildViewHolder {
        TextView cityName;
    }
}