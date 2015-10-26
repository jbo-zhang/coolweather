package activity;

import java.security.PublicKey;

import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

import android.app.Activity;
import android.app.DownloadManager.Query;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.coolweather.R;

public class WeatherActivity extends Activity implements OnClickListener {
	private LinearLayout weatherInfoLayout;
	
	private TextView cityNameText;
	private TextView publishText;
	private TextView weatherDespText;
	private TextView temp1Text;
	private TextView temp2Text;
	private TextView currentDateText;
	
	private Button switchCity;	//切换城市
	private Button refreshWeather;	//更新天气
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		// 初始化各控件
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		
		String countyCode = getIntent().getStringExtra("county_code");
		if(!TextUtils.isEmpty(countyCode)) {
			//有县级代号时去查询天气
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		} else {
			// 没有县级代号时就直接显示本地天气
			showWeather();
		}
		switchCity.setOnClickListener( this);
		refreshWeather.setOnClickListener(this);
		
		
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.switch_city :
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
		case R.id.refresh_weather :
			publishText.setText("同步中...");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if(!TextUtils.isEmpty(weatherCode)) {
				queryWeatherInfo(weatherCode);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * 查询县级代号对应的天气代号。
	 */
	private void queryWeatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");
	}
	
	/**
	 * 查询天气代号所对应的天气
	 */
	private void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		System.out.println("address-------" + address);
		queryFromServer(address, "weatherCode");
	}
	/**
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息
	 */
	private void queryFromServer(final String address, final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(final String response) {
				if("countyCode".equals(type)) {
					if(!TextUtils.isEmpty(response)) {
						//从服务器返回的数据中解析出天气代号
						String[] array = response.split("\\|");
						if(array != null && array.length == 2) {
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}
					}
				} else if("weatherCode".equals(type)) {
					//处理服务器返回的天气信息
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							showWeather();
						}
					});
				}
			}
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						publishText.setText("同步失败");
					}
				});
			}
		});
	}
	
	/**
	 * 从SharedPreferences文件中读取存储的天气信息，并显示到街面上
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		
	}
	/**
		同样，这个活动中的代码也非常长，我们还是一步步梳理下。在 onCreate()方法中仍然
	先是去获取一些控件的实例，然后会尝试从 Intent 中取出县级代号，如果可以取到就会调用
	queryWeatherCode()方法，如果不能取到则会调用 showWeather()方法，我们先来看下可以取
	到的情况。
		queryWeatherCode()方法中并没有几行代码，仅仅是拼装了一个地址，然后调用
	queryFromServer()方法来查询县级代号所对应的天气代号。服务器返回的数据仍然会回调到
	onFinish()方法中，这里对返回的数据进行解析，然后将解析出来的天气代号传入到
	queryWeatherInfo()方法中。
		queryWeatherInfo()方法也非常简单， 同样是拼装了一个地址， 然后调用 queryFromServer()
	方法来查询天气代号所对应的天气信息。由于天气信息是以 JSON 格式返回的，因此我们在
	handleWeatherResponse() 方 法 中 使 用 JSONObject 将 数 据 全 部 解 析 出 来 ， 然 后 调 用
	saveWeatherInfo()方法将所有的天气信息都存储到 SharedPreferences 文件中。注意除了天气
	信息之外，我们还存储了一个 city_selected 标志位，以此来辨别当前是否已经选中了一个城
	市。最后会去调用 showWeather()方法来将所有的天气信息显示到界面上，showWeather()方
	法中的逻辑很简单，就是从 SharedPreferences 文件中将数据读取出来，然后一一设置到界面
	上即可。
		刚才分析的是在 onCreate()方法中可以取到县级代号的情况，那么不能取到的时候呢？
	原来就是直接调用 showWeather()方法来显示本地存储的天气信息就可以了。
	*/
}
