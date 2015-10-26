package activity;

import java.util.ArrayList;
import java.util.List;

import model.City;
import model.County;
import model.Province;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.R;

import db.CoolWeatherDB;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	 
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	
	private List<Province> provinceList;	//省列表
	private List<City> cityList;	//市列表
	private List<County> countyList;	//县列
	private Province selectedProvince;	//选中的省份	
	private City selectedCity;	//选中的城市
	private int currentLevel;	//当前选中的级别
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int index, long arg3) {
				if(currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}
			}
		});
		queryProvinces();	//加载省级数据
	}
	
	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if(provinceList.size() > 0) {
			dataList.clear();
			for(Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}
	}
	

	/**
	 * 查询选中省内的市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities()	{
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else { 
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	
	/**
	* 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	*/
	private void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0) {
			dataList.clear();
			for(County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 * @param object
	 * @param string
	 */
	private void queryFromServer(final String code, final String type) {
		String address;
		if(!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result = false;
				if("province".equals(type)) {
					result = Utility.handleProvincesResponse(coolWeatherDB, response);
				} else if("city".equals(type)) {
					result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				} else if("county".equals(type)) {
					result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if(result) {
					// 通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						public void run() {
							closeProgressDialog();
							if("province".equals(type)) {
								queryProvinces();
							} else if("city".equals(type)) {
								queryCities();
							} else if("county".equals(type)) {
								queryCounties();
							}
							
						}
					});
				}
			}
			@Override
			public void onError(Exception e) {
				// 通过runOnUiThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if(progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if(progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表，省列表，还是直接退出。
	 */
	@Override
	public void onBackPressed() {
		if(currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			finish();
		}
	}
	
	/**
		 * 这个类里的代码虽然非常多，可是逻辑却不复杂，我们来慢慢理一下。在 onCreate()方
	法中先是获取到了一些控件的实例，然后去初始化了 ArrayAdapter，将它设置为 ListView 的
	适配器。之后又去获取到了 CoolWeatherDB 的实例，并给 ListView 设置了点击事件，到这
	里我们的初始化工作就算是完成了。
		在 onCreate()方法的最后，调用了 queryProvinces()方法，也就是从这里开始加载省级数
	据的。queryProvinces()方法的内部会首先调用 CoolWeatherDB 的 loadProvinces()方法来从数
	据库中读取省级数据，如果读取到了就直接将数据显示到界面上，如果没有读取到就调用
	queryFromServer()方法来从服务器上查询数据。
		queryFromServer()方法会先根据传入的参数来拼装查询地址，这个地址就是我们在 14.1
	节分析过的。确定了查询地址之后，接下来就调用 HttpUtil 的 sendHttpRequest()方法来向服
	务器发送请求，响应的数据会回调到 onFinish()方法中，然后我们在这里去调用 Utility 的
	handleProvincesResponse()方法来解析和处理服务器返回的数据，并存储到数据库中。接下来
	的一步很关键，在解析和处理完数据之后，我们再次调用了 queryProvinces()方法来重新加载
	省级数据，由于 queryProvinces()方法牵扯到了 UI 操作，因此必须要在主线程中调用，这里
	借助了 runOnUiThread()方法来实现从子线程切换到主线程，它的实现原理其实也是基于异
	步消息处理机制的。现在数据库中已经存在了数据，因此调用 queryProvinces()就会直接将数
	据显示到界面上了。
		当你点击了某个省的时候会进入到 ListView 的 onItemClick()方法中， 这个时候会根据当
	前的级别来判断是去调用 queryCities()方法还是 queryCounties()方法，queryCities()方法是去
	查询市级数据，而 queryCounties()方法是去查询县级数据，这两个方法内部的流程和
	queryProvinces()方法基本相同，这里就不重复讲解了。
		另外还有一点需要注意，我们重写了 onBackPressed()方法来覆盖默认 Back 键的行为，
	这里会根据当前的级别来判断是返回市级列表、省级列表、还是直接退出。
	 */
}
