package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<Story> stories = new ArrayList<>();
    ArrayList<String> titles = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    ProgressBar progressBar, loadingSpinner;
    JSONArray articleIDs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        DownloadArticles downloadArticles = new DownloadArticles();
        arrayAdapter =  new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        progressBar.setMax(20);

        try {
            downloadArticles.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Error", "Download failed!");
        }

    }

    //Method that returns the JSON data retrieved from a URL
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
            Log.i("Error", "GET JSON failed!");
        }
        return String.valueOf(json);
    }

    //Method to fetch the next 20 articles and add them to the listView
    public void loadArticles(){
        String title, jsonUrl, contentUrl;
        try {
            for (int i = titles.size(); i < titles.size()+20; i++) { //downloads the top 20 stories out of 500 article IDs
                jsonUrl = "https://hacker-news.firebaseio.com/v0/item/" + articleIDs.get(i) + ".json?print=pretty";
                String json = getJSON(jsonUrl); //gets article data using article ID
                JSONObject storyData = new JSONObject(json);

                //if the article doesn't contain a title or url then skip it
                if (storyData.has("url") && storyData.has("title")) {
                    title = storyData.getString("title");
                    contentUrl = storyData.getString("url");
                    Story story = new Story(title, contentUrl); //construct a new story with article title and url
                    stories.add(story);
                }
                progressBar.setProgress(i-titles.size());
            }
        }catch(Exception e){
            e.printStackTrace();
            Log.i("Error", "JSON handling failed");
        }
    }

    //Method to populate the listView
    public void loadTitles(){
        for(int i = 0; i < stories.size(); i++){
            titles.add(stories.get(i).title);
        }
        titles.add("More stories...");
    }

    //Downloads the top 10 article IDs from a list of 500
    public class DownloadArticles extends AsyncTask<String, Void, ArrayList<Story>>{

        @Override
        protected void onPreExecute() {
            arrayAdapter.notifyDataSetChanged();
            loadingSpinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<Story> doInBackground(String... strings) {
            String json = getJSON(strings[0]); //gets list of 500 article IDs
            try {
                articleIDs = new JSONArray(String.valueOf(json)); //stores list of 500 article IDs
                Log.i("Article IDs", articleIDs.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Error","Store JSON failed");
            }
            loadArticles();
            return stories;
        }

        @Override
        protected void onPostExecute(ArrayList<Story> s) {
            stories = s;
            arrayAdapter.notifyDataSetChanged();
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            loadingSpinner.setVisibility(View.GONE);
            loadTitles();
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if(position==titles.size()-1){ //if the last item is clicked, load more data
                        Log.i("debug info","Loading more items...");
                        titles.remove(position);

                        //loadArticles()

                        arrayAdapter.notifyDataSetChanged();
                    }else {
                        Log.i("debug info","opening webView...");
                        Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                        intent.putExtra("url", stories.get(position).url);
                        startActivity(intent);
                    }

                }
            });
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
