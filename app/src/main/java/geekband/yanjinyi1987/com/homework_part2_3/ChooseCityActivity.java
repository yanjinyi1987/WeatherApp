package geekband.yanjinyi1987.com.homework_part2_3;

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

import geekband.yanjinyi1987.com.homework_part2_3.service.WeatherService;

public class ChooseCityActivity extends AppCompatActivity {
    public static final String TAG = "ChooseCityActivity";
    public static final int SEND_DATA_TO_MEMORY = 1;
    public static final int WRITE_FINISHED = 2;
    public static final int GET_GLOBAL_CITY_FROM_DB_DONE = 3;
    public static final int SEND_DATA_PACKAGE = 4;

    Handler mHandler;
    Map<String,WeatherService.ProvinceList> cityLists;

    class ServiceHandler extends  Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SEND_DATA_TO_MEMORY: //init ViewPager
                    chooseCityInfos_size = msg.getData().getInt("DATA_SIZE");
                    Message request_start_transfer_msg = new Message();
                    request_start_transfer_msg.what = REQUEST_START_TRANSFER;
                    try {
                        mServiceMessenger.send(request_start_transfer_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case SEND_DATA_PACKAGE:
                    List<WeatherService.CityInfo> dataPackage = (List<WeatherService.CityInfo>) msg.getData().getSerializable("DATA_PACKAGE");
                    Message request__transfer_msg = new Message();
                    if(dataPackage!=null) {
                        choosedCityInfos.addAll(dataPackage);

                        if(choosedCityInfos.size()==chooseCityInfos_size) {
                            request__transfer_msg.what = MainActivity.TRANSFER_DONE;
                        }
                        else {
                            request__transfer_msg.what = MainActivity.REQUEST_START_TRANSFER;
                        }
                    }
                    else {
                        request__transfer_msg.what = MainActivity.RE_TRANSFER;
                    }
                    try {
                        mServiceMessenger.send(request__transfer_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case GET_GLOBAL_CITY_FROM_DB_DONE:
                    Log.i(TAG,"Got Global City Data from Service");
                    //cityLists = (Map<String, WeatherService.ProvinceList>) msg.getData().getSerializable("GLOBAL_CITY_LIST");

                    if(cityLists==null) {
                        //add refresh button
                    }
                    else {
                        initListViews(cityLists);
                    }
                    break;
                case WRITE_FINISHED:
                    Log.i(TAG,"WRITE_FINISHED");
                    Intent intent = new Intent(ChooseCityActivity.this,MainActivity.class);
                    intent.putExtra("Position",
                            ((WeatherService.CityInfo)(msg.getData().getSerializable("CurrentCity"))).getId());
                    startActivityForResult(intent,0); //uiying OnCreate
                    finish();
                    break;
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
            msg.what = MainActivity.SEND_MESSENGER_TO_SERVICE_ChooseCityActivity;
            msg.obj = mMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            onBound = true;
            initViews();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            onBound = false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_city);

        //bindService
        bindService(new Intent(ChooseCityActivity.this,WeatherService.class),
                mServiceConnection,
                BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        Log.i(this.getClass().getSimpleName(),"onDestroy");
        //Log.i(this.getClass().getSimpleName(),"Send Exit App");
        Intent exitAppIntent = new Intent();
        exitAppIntent.setAction("MainActivity.ExitApp");
        //sendBroadcast(exitAppIntent);
        super.onDestroy();
    }

    private void initCityData() {
        //send message to Service to do this;
        Message msg = new Message();
        msg.what = MainActivity.GET_GLOBAL_CITY_LIST_FROM_DB;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private void initViews() {
        initCityData();
    }
//ExpandableListView
    private void initListViews(final Map<String, WeatherService.ProvinceList> cityLists) {
        final List<CityWithInfo> provinceList = new ArrayList<>();
        for (String prov_name:cityLists.keySet()
             ) {
            WeatherService.CityInfo cityInfo = new WeatherService.CityInfo(prov_name);
            provinceList.add(new CityWithInfo(cityInfo,R.drawable.acs));

        }

        Collections.sort(provinceList, new Comparator<CityWithInfo>() {
            @Override
            public int compare(CityWithInfo lhs, CityWithInfo rhs) {
               return lhs.getName().getCity().compareTo(rhs.getName().getCity());
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
                WeatherService.CityInfo chosenCity = cityLists.get(provinceList.get(groupPosition).getName().getCity()).getCities().get(childPosition);
                writeChoosedCitytoDatabase(chosenCity);
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

    private void writeChoosedCitytoDatabase(WeatherService.CityInfo city) {
        //send message to Service to do this;
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putSerializable("WeatherService.CityInfo",city);
        msg.getData().putBundle("Bundle",bundle);
        msg.what = MainActivity.WRITE_CHOSEN_CITY_TO_DB;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

class CityAdapter_Expand extends BaseExpandableListAdapter {
    List<CityWithInfo> groupList;
    Map<String,WeatherService.ProvinceList> childMap;
    private int parentResourceId,childResourceId;
    Context mContext;


    public CityAdapter_Expand(Context context, int parentLayoutResourceId, int childLayoutResourceId
            , List<CityWithInfo> provinces, Map<String,WeatherService.ProvinceList> citiesPerProvince) {
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
        return childMap.get(groupList.get(groupPosition).getName().getCity()).getCities().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        //return null;
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        //return null;
        return childMap.get(groupList.get(groupPosition).getName().getCity()).getCities();
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
        CityWithInfo cityWithInfo = groupList.get(groupPosition);
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


        parentViewHolder.arrowImage.setImageResource(cityWithInfo.getImageId());
        parentViewHolder.provinceName.setText(cityWithInfo.getName().getCity());

        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String cityName = childMap.get(groupList.get(groupPosition).getName().getCity()).getCities().get(childPosition).getCity();
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