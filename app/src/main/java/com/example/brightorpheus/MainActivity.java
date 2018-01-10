package com.example.brightorpheus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity {

    TextView responseText;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        responseText = (TextView)findViewById(R.id.response_text);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor=prefs.edit();

        /*
        *如果是第一次打开APP的话，需要向服务器提交自己的Mac地址，然后保存服务器返回的自己的ID，并将自己的ID保存到
        *起来，方便后面进行调用
         */
        if(prefs.getBoolean("isFirstOpen",true)==true){
            sendMacAddressWithHttpURLConnection();
            editor.putBoolean("isFirstOpen",false).commit();
        }

        /*
        *检查完是否是第一次打开此App后，执行活动切换操作，展示另一个活动
         */
        if(prefs.getString("music",null)==null){
            String data=prefs.getString("Id","0");
            Intent intent = new Intent(MainActivity.this, MusicActivity.class);
            intent.putExtra("extra_data",data);
            startActivity(intent);
            finish();
        }
    }
    /*
    *通过http发送MAC地址给服务器并且将返回的Id保存到本地数据库中
    * 注意有可能出现的问题是返回解析的不是一个单一的数字，而是包括一些状态信息的字符串
     */
    private void sendMacAddressWithHttpURLConnection(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("http://wlsx.huyunfan.cn/add_usr.php");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes("APP_PWD=wlsx123&mac_addr="+getLocalMacAddress());

                    InputStream in = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line=reader.readLine())!=null){
                        response.append(line);
                    }
                    save_Id(response.toString());//保存返回的Id值
                }catch (Exception e)
                {
                    e.printStackTrace();
                }finally {
                    if(reader!=null){
                        try{
                            reader.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if(connection!=null)
                    {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    /*
    *保存本机的Id作为唯一标示，与MAC地址一一对应
     */
    private void save_Id(final String response){
        editor.putString("Id",response).commit();
    }

    /*
    *获取本机MAC地址
     */
    public static String getLocalMacAddress() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);


            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }
        return macSerial;
    }
}
