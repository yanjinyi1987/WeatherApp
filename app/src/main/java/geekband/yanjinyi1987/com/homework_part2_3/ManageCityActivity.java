package geekband.yanjinyi1987.com.homework_part2_3;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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

    private Button mChooseCityButton;
    private ListView mChoosedCities;
    List<WeatherService.CityInfo> cityInfos;

    class ServiceHandler extends  Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
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
            msg.what = SEND_MESSENGER_TO_SERVICE_ManageCityActivity;
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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    cityInfos = (List<WeatherService.CityInfo>) msg.obj;
                    initListViews(cityInfos);
                    break;
                default:
                    break;
            }
        }
    };
    private Button mFinishEditButton;
    private final List<CityWithInfo> cityWithInfoList = new ArrayList<>();
    private ChoosedCityAdapter choosedCityAdapter;

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
        initViews();
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
        Log.i(this.getClass().getSimpleName(),"onDestroy");
    }

    private  void initViews() {
        mChooseCityButton = (Button) findViewById(R.id.add_city_button);
        mChoosedCities = (ListView) findViewById(R.id.choosed_cities_listview);
        mFinishEditButton = (Button) findViewById(R.id.finish_edit_button);

        mChooseCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ManageCityActivity.this,ChooseCityActivity.class));
            }
        });

        mFinishEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (CityWithInfo cityWithInfo: cityWithInfoList
                        ) {
                    cityWithInfo.setImageId(-1);
                }

                choosedCityAdapter.notifyDataSetChanged();
                mFinishEditButton.setVisibility(View.INVISIBLE);
            }
        });

        choosedCityAdapter = new ChoosedCityAdapter(ManageCityActivity.this,
                R.layout.parent_list_view_item,
                cityWithInfoList);
        mChoosedCities.setAdapter(choosedCityAdapter);

        getChoosedCitiesFromDatabase();


    }

    private  void initListViews(List<WeatherService.CityInfo> cities) {
        choosedCityAdapter.clear();
        for (WeatherService.CityInfo city: cities
             ) {
            cityWithInfoList.add(new CityWithInfo(city,-1));
        }
        choosedCityAdapter.notifyDataSetChanged();
        //long press to show delete signature and manipulate the database

        mChoosedCities.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mFinishEditButton.setVisibility(View.VISIBLE);
                for (CityWithInfo cityWithInfo: cityWithInfoList
                     ) {
                    cityWithInfo.setImageId(R.drawable.error);
                }

                choosedCityAdapter.notifyDataSetChanged();

                return false;
            }
        });

    }

    private void getChoosedCitiesFromDatabase() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<WeatherService.CityInfo> cityInfos;

                Looper.prepare();
                Message msg = new Message();
                CityListOperations cityListOperations = new CityListOperations(ManageCityActivity.this);
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase(); //database is locked, need wait()
                cityInfos = cityListOperations.getChoosedCities();

                msg.what = 1;
                msg.obj = cityInfos;

                mHandler.sendMessage(msg);

                Looper.loop();
            }
        }).start();
    }
}



class ChoosedCityAdapter extends ArrayAdapter<CityWithInfo> {
    int resourceId;
    List<CityWithInfo> cityWithInfoList;
    public ChoosedCityAdapter(Context context, int resource, List<CityWithInfo> objects) {
        super(context, resource, objects);
        resourceId=resource;
        cityWithInfoList=objects;
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
                        cityWithInfoList.remove(position);
                        //delete the data in the database
                        ChoosedCityAdapter.this.notifyDataSetChanged();
                        //database operation
                        deleteChoosedCitiesFromDatabase(cityId);
                        //
                        MainActivity.datachanged=true;

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

    public void deleteChoosedCitiesFromDatabase(final String cityId) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();
                CityListOperations cityListOperations = new CityListOperations(getContext());
                //SQLiteDatabase cityListDatabase = cityListOperations.getWritableDatabase();
                while(cityListOperations.syncdelete(cityId)==0) {
                    Log.i("ManagCityActivity", "delete error!");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//                Message msg = new Message();
//                msg.what = 1;
//                msg.obj = cityInfos;
//                mHandler.sendMessage(msg);

                Looper.loop();
            }
        }).start();
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
