package com.galaxy.android.main;

import android.content.Context;

import com.galaxy.asia.bean.ApkInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainPresenter implements MainContract.Presenter {

    private MainContract.View view;

    @Override
    public void loadGooglePlay(Context context, List<String> titles, HashMap<String, List<ApkInfo>> map) {
        OkHttpClient client = new OkHttpClient();

        String lan = context.getResources().getConfiguration().locale.getLanguage();

        Request request = new Request.Builder()
                .url("https://play.google.com/store/apps/top")
                .header("accept-language", lan)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String ret = response.body().string();
            Document document = Jsoup.parse(ret);
            Elements elements = document.getElementsByClass("ZmHEEd");
            Elements titleElements = document.getElementsByTag("h2");
            titles.clear();
            for (int c = 0; c < titleElements.size(); c++) {
                titles.add(titleElements.get(c).text());
            }
            for (int i = 0; i < elements.size(); i++) {
                Element category = elements.get(i);

                Elements child = category.getElementsByClass("WHE7ib");
                List<ApkInfo> apkCategory = new ArrayList<>();
                for (int j = 0; j < child.size(); j++) {
                    String url = child.get(j).getElementsByClass("wXUyZd").get(0).getElementsByTag("a").get(0).attr("href");
                    url = "https://play.google.com" + url;
                    // name
                    String appName = child.get(j).getElementsByClass("b8cIId").get(0).text();
                    // developer name
                    String developerName = child.get(j).getElementsByClass("b8cIId").get(1).text();
                    // image
                    String imgUrl = child.get(j).getElementsByClass("T75of").get(0).attr("data-src");

                    ApkInfo info = new ApkInfo();
                    info.setName(appName);
                    info.setLogo(imgUrl);
                    info.setUrl(url);
                    info.setDeveloperName(developerName);
                    apkCategory.add(info);
                }
                map.put(titles.get(i), apkCategory);
            }
            if (view != null) {
                view.onGooglePlayLoaded();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void takeView(MainContract.View view) {
        this.view = view;
    }

    @Override
    public void dropView() {
        this.view = null;
    }
}
