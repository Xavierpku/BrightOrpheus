package com.example.brightorpheus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.brightorpheus.util.HttpUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Deque;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by xiezhiyuan on 2018/1/9.
 */

public class MusicActivity extends AppCompatActivity{
    private ScrollView musicLayout;

    private ImageView bingPicImg;

    private WebView webView;

    private int song_Id;

    private Button button_search;

    private EditText editText;

    private int[] song_list = new int[1000];

    private int usr_id;

    private int now_pointer;

    private SharedPreferences.Editor editor;

    private WebView introduction_view;

    private Button add;

    private Button last;

    private Button next;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if(Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }*/
        setContentView(R.layout.activity_music);
        /*
        *每次打开之前先获取指示当前时间的指针
         */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        now_pointer=prefs.getInt("now_pointer",0);
        editor = prefs.edit();


        Intent intent = getIntent();
        String data =intent.getStringExtra("extra_data");
        usr_id=StrtoInt(data);


        musicLayout = (ScrollView)findViewById(R.id.music_layout);
        song_Id = 1;
        button_search = (Button)findViewById(R.id.button_search);
        add = (Button)findViewById(R.id.add_operate);
        last = (Button)findViewById(R.id.last_song);
        next = (Button)findViewById(R.id.next_song);
        editText = (EditText)findViewById(R.id.edit_text);

        webView = (WebView)findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        introduction_view = (WebView) findViewById(R.id.introduction);
        introduction_view.getSettings().setJavaScriptEnabled(true);

        musicLayout.setVisibility(View.VISIBLE);


        song_list[0]=1;
        sendRequestforRecommend();
        song_Id = song_list[now_pointer];
        webview_show(song_Id);

        button_search.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                String inputText = editText.getText().toString();
                String input = null;
                try {
                    input = toUtf8(inputText);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                sendRequestforSearch(input);
            }
        });

        add.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                sendRequestforAdd(song_Id);
                Toast.makeText(MusicActivity.this,"Added it to your collection",Toast.LENGTH_SHORT).show();
            }
        });

        last.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(now_pointer>0) {
                    now_pointer--;
                    song_Id = song_list[now_pointer];
                    Toast.makeText(MusicActivity.this, "Last one", Toast.LENGTH_SHORT).show();
                    webview_show(song_Id);
                }
                else{
                    Toast.makeText(MusicActivity.this, "No more", Toast.LENGTH_SHORT).show();
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                now_pointer++;
                song_Id = song_list[now_pointer];
                Toast.makeText(MusicActivity.this,"Next one",Toast.LENGTH_SHORT).show();
                webview_show(song_Id);
            }
        });
        bingPicImg = (ImageView)findViewById(R.id.bing_pic_img);
        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }
        else{
            loadBingPic();
        }

        editor.putInt("now_pointer",now_pointer);
    }

    /*
    *Unicode转UTF-8
     */
    public String toUtf8(String str) throws UnsupportedEncodingException {
        return new String( URLEncoder.encode(str,"UTF-8"));
    }

    /*
*加载每日一图
 */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MusicActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(MusicActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    /*
    *重写菜单选项
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    /*
    *为菜单的每个选项加上一个监听器
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.Add:
                sendRequestforAdd(song_Id);
                Toast.makeText(MusicActivity.this,"Added it to your collection",Toast.LENGTH_SHORT).show();
                break;
            case R.id.Yesterday:
                if(now_pointer>0) {
                    now_pointer--;
                    song_Id = song_list[now_pointer];
                    Toast.makeText(MusicActivity.this, "Last one", Toast.LENGTH_SHORT).show();
                    webview_show(song_Id);
                }
                else{
                    Toast.makeText(MusicActivity.this, "No more", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tomorrow:
                now_pointer++;
                song_Id = song_list[now_pointer];
                Toast.makeText(MusicActivity.this,"Next one",Toast.LENGTH_SHORT).show();
                webview_show(song_Id);
                break;
            default:
        }
        return true;
    }

    private void webview_show(int Id){
        webView.loadUrl("http://wlsx.huyunfan.cn/music.php?Id="+Id);
        introduction_view.loadUrl("http://wlsx.huyunfan.cn/article.php?Id="+Id);
    }

    /*
    *添加用户喜好专用，无返回
     */
    private  void sendRequestforAdd(final int song_Id){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("http://wlsx.huyunfan.cn/renew.php");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes("APP_PWD=wlsx123&uid="+usr_id+"&item_id="+ song_Id);

                    InputStream in = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line=reader.readLine())!=null){
                        response.append(line);
                    }
                    Toast.makeText(MusicActivity.this,response.toString(),Toast.LENGTH_LONG).show();
                    add_recommend(response.toString());

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
    *推荐功能专用请求，返回推荐的歌曲Id
     */
    private void sendRequestforRecommend(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("http://wlsx.huyunfan.cn/recommend.php");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes("APP_PWD=wlsx123&id="+usr_id);

                    InputStream in = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line=reader.readLine())!=null){
                        response.append(line);
                    }
                    Toast.makeText(MusicActivity.this,response.toString(),Toast.LENGTH_LONG).show();
                    add_recommend(response.toString());

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
    *搜索功能专用请求，返回搜到的页面Id
     */
    private void sendRequestforSearch(final String request){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("http://wlsx.huyunfan.cn/usr_search_app.php");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes("tit="+request+"&Page=1");

                    InputStream in = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line=reader.readLine())!=null){
                        response.append(line);
                    }
                    save_song_Id(response.toString());
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
    *请求推荐专用，返回的推荐歌曲Id存放在song_list中
     */
    private void add_recommend(final String str){
        String []num = str.split(" ");
        for(int i=0;i<num.length;i++)
        song_list[i+now_pointer]=StrtoInt(num[i]);
        Toast.makeText(MusicActivity.this,num[0],Toast.LENGTH_LONG).show();
    }
    /*
    *保存歌曲的编号
     */
    private void save_song_Id(final String Id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                song_Id = StrtoInt(Id);
                webview_show(song_Id);
            }
        });
    }
    /*
    *字符串转数字
     */
    private int StrtoInt(String str){
        str=str.trim();
        String str2="";
        if(str != null && !"".equals(str)){
            for(int i=0;i<str.length();i++){
                if(str.charAt(i)>=48 && str.charAt(i)<=57){
                    str2+=str.charAt(i);
                }
            }
        }
        return Integer.parseInt(str2);
    }
}
