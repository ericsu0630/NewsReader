package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<Story> stories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = findViewById(R.id.listView);
        ArrayList<String> titles = new ArrayList<>();
        DownloadArticles downloadArticles = new DownloadArticles();

        try {
            stories = downloadArticles.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Warning", "Download failed!");
        }

        for(int i = 0; i < stories.size(); i++){
            titles.add(stories.get(i).title);
        }

        ArrayAdapter<String> arrayAdapter =  new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),WebViewActivity.class);
                intent.putExtra("url", stories.get(position).url);
                startActivity(intent);
            }
        });
    }

    //Returns the JSON data retrieved from a URL
    public String getJSON(String u){
        StringBuilder json = new StringBuilder();
        try{
            URL url = new URL(u);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            InputStream inputStream = httpURLConnection.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream);
            int data;
            do{
                data = reader.read();
                char c = (char) data;
                json.append(c);
            }while(data!=-1);
        }catch(Exception e){
            e.printStackTrace();
            Log.i("Warning", "GET JSON failed!");
        }
        return String.valueOf(json);
    }

    //Downloads the top 10 article IDs from a list of 500
    public class DownloadArticles extends AsyncTask<String, Void, ArrayList<Story>>{

        @Override
        protected ArrayList<Story> doInBackground(String... strings) {
            ArrayList<Story> stories = new ArrayList<>();
                String json = getJSON(strings[0]); //gets list of article IDs
                String title, jsonUrl, contentUrl;
            try {
                JSONArray articleIDs = new JSONArray(String.valueOf(json));
                for(int i=0;i<20;i++){ //downloads the top ten stories out of 500 article IDs
                    jsonUrl = "https://hacker-news.firebaseio.com/v0/item/"+articleIDs.get(i)+".json?print=pretty";
                    json = getJSON(jsonUrl); //gets article data using article ID
                    JSONObject storyData = new JSONObject(json);
                    //if the article doesn't contain a title or url then skip it
                    if(storyData.has("url") && storyData.has("title")){
                        title = storyData.getString("title");
                        contentUrl = storyData.getString("url");
                        Story story = new Story(title, contentUrl); //construct a new story with article title and url
                        stories.add(story);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Warning","JSON handling failed");
            }
            return stories;
        }

    }

    //Story constructor class
    public static class Story {
        String title;
        String url;
        public Story(String t, String u){
            title = t;
            url = u;
        }
    }
}
