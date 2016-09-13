package geekband.yanjinyi1987.com.homework_part2_3;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import geekband.yanjinyi1987.com.homework_part2_3.service.WeatherService;

public class ManageCityActivity extends AppCompatActivity {
    public static final String TAG = "ManagerActivity";
    public static int mClickingPostion=-1;
    //private Message List
    public static final int GLOBAL_FAULT = -2;
    public static final int GOT_GLOBAL_CITY_LIST = 0;
    public static final int DELETE_CHOSEN_CITY_FROM_DB_FAILED=1;
    public static final int DELETE_CHOSEN_CITY_FROM_DB_SUCCED=2;
    public static final int GET_CHOOSED_CITY_FROM_DB_FAILED=3;
    public static final int GET_CHOOSED_CITY_FROM_DB_SUCCED=4; //equal to INIT_VIEWPAGER
    public static final int INIT_VIEWPAGER = 4;
    private Button mChooseCityButton;
    private ListView mChoosedCities;
    List<WeatherService.CityInfo> cityInfos;

    class ServiceHandler extends  Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case GET_CHOOSED_CITY_FROM_DB_SUCCED:
                    Log.i(TAG,"GET_CHOOSED_CITY_FROM_DB_SUCCED");
                    cityInfos = (List<WeatherService.CityInfo>)(msg.getData().getSerializable(
                            "CHOSED_CITY_LIST"));
                    initListViews(cityInfos);
                    break;
                case GET_CHOOSED_CITY_FROM_DB_FAILED:
                    Log.i(TAG,"GET_CHOOSED_CITY_FROM_DB_FAILED");
                    //add refresh button
                    break;
                case DELETE_CHOSEN_CITY_FROM_DB_SUCCED:
                    Log.i(TAG,"DELETE_CHOSEN_CITY_FROM_DB_SUCCED");
                    if(mClickingPostion!=-1) {
                        cityWithInfoList.remove(mClickingPostion);
                        //delete the data in the database
                        mChosenCityAdapter.notifyDataSetChanged();
                        //
                        MainActivity.datachanged = true;
                    }
                    break;
                case DELETE_CHOSEN_CITY_FROM_DB_FAILED:
                    Log.i(TAG,"DELETE_CHOSEN_CITY_FROM_DB_FAILED");
                    MainActivity.datachanged = false;
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    public Messenger mServiceMessenger;
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
            msg.what = MainActivity.SEND_MESSENGER_TO_SERVICE_ManageCityActivity;
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

    private Button mFinishEditButton;
    private final List<CityWithInfo> cityWithInfoList = new ArrayList<>();
    private ChoosedCityAdapter mChosenCityAdapter;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "MainActivity.ExitApp":
                    Log.i(this.getClass().getSimpleName(),"got Message, exiting");
                    finish();
                    break;
                default:
                    break;
            }
        }
    };

    /**/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_city);
        Log.i("ManagerCityActivity","onCreate");
        //bind Service
        bindService(new Intent(ManageCityActivity.this,WeatherService.class),mServiceConnection,BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("MainActivity.ExitApp");
        registerReceiver(broadcastReceiver,intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("ManagerCityActivity","onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ManagerCityActivity","onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("ManagerCityActivity","onStop");
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
        Log.i(TAG,"onDestroy");
    }

    private  void initViews() {
        mChooseCityButton = (Button) findViewById(R.id.add_city_button);
        mChoosedCities = (ListView) findViewById(R.id.choosed_cities_listview);
        mFinishEditButton = (Button) findViewById(R.id.finish_edit_button);

        mChooseCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ManageCityActivity.this,ChooseCityActivity.class));
                finish(); //end this activity
            }
        });

        mFinishEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (CityWithInfo cityWithInfo: cityWithInfoList
                        ) {
                    cityWithInfo.setImageId(-1);
                }

                mChosenCityAdapter.notifyDataSetChanged();
                mFinishEditButton.setVisibility(View.INVISIBLE);
                mChooseCityButton.setEnabled(true);
            }
        });

        mChosenCityAdapter = new ChoosedCityAdapter(ManageCityActivity.this,
                R.layout.parent_list_view_item,
                cityWithInfoList);
        mChoosedCities.setAdapter(mChosenCityAdapter);

        getChoosedCitiesFromDatabase();


    }

    private  void initListViews(List<WeatherService.CityInfo> cities) {
        mChosenCityAdapter.clear();
        for (WeatherService.CityInfo city: cities
             ) {
            cityWithInfoList.add(new CityWithInfo(city,-1));
        }
        mChosenCityAdapter.notifyDataSetChanged();
        //long press to show delete signature and manipulate the database

        mChoosedCities.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mChooseCityButton.setEnabled(false);
                mFinishEditButton.setVisibility(View.VISIBLE);
                for (CityWithInfo cityWithInfo: cityWithInfoList
                     ) {
                    cityWithInfo.setImageId(R.drawable.error);
                }

                mChosenCityAdapter.notifyDataSetChanged();

                return false;
            }
        });

    }

    private void getChoosedCitiesFromDatabase() {
        //send message to Service to do this;
        Message msg = new Message();
        msg.what = MainActivity.GET_CHOOSED_CITY_FROM_DB;
        msg.getData().putInt("Identify_ID",1); //msg from ManagerCityActivity
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}



class ChoosedCityAdapter extends ArrayAdapter<CityWithInfo> {
    int resourceId;
    ManageCityActivity context;
    List<CityWithInfo> cityWithInfoList;


    public ChoosedCityAdapter(Context context, int resource, List<CityWithInfo> objects) {
        super(context, resource, objects);
        resourceId=resource;
        cityWithInfoList=objects;
        this.context = (ManageCityActivity) context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        CityWithInfo city = getItem(position);
        View view;
        ParentViewHolder parentViewHolder;
        if(convertView==null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId,null);
            parentViewHolder = new ParentViewHolder();
            parentViewHolder.arrowImage = (ImageView) view.findViewById(R.id.listview_item_png);
            parentViewHolder.provinceName = (TextView) view.findViewById(R.id.listview_item_text);
            view.setTag(parentViewHolder); //save the viewHolder in view
            parentViewHolder.arrowImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(getContext(),"you click the png",Toast.LENGTH_SHORT).show();
                    if(cityWithInfoList.get(position).getImageId()!=-1) {
                        String cityId = new String(cityWithInfoList.get(position).getName().getId());
                        //database operation
                        deleteChoosedCitiesFromDatabase(cityId);
                        ManageCityActivity.mClickingPostion = position;
                    }
                }
            });
        }
        else {
            view =convertView;
            parentViewHolder = (ParentViewHolder) view.getTag();
        }
        if(city.getImageId()!=-1) {
            parentViewHolder.arrowImage.setImageResource(city.getImageId());
            parentViewHolder.arrowImage.setVisibility(View.VISIBLE);
        }
        else {
            parentViewHolder.arrowImage.setVisibility(View.INVISIBLE);
        }
        parentViewHolder.provinceName.setText(city.getName().getCity());

        return view;
    }

    class ParentViewHolder {
        TextView provinceName;
        ImageView arrowImage;
    }

    public void deleteChoosedCitiesFromDatabase(String cityId) {
        //send message to Service to do this;
        Message msg = new Message();
        msg.what = MainActivity.DELETE_CHOSEN_CITY_FROM_DB;
        msg.getData().putString("CityId",cityId);
        try {
            context.mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

class CityWithInfo {
    private WeatherService.CityInfo city;
    private int imageId;

    public CityWithInfo(WeatherService.CityInfo city, int imageId) {
        this.city=city;
        this.imageId=imageId;
    }

    public WeatherService.CityInfo getName() {
        return city;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }
}
