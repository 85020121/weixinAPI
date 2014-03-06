package com.hesong.weChatAdapter.message.send;

import java.util.List;
import java.util.Map;

import com.hesong.weChatAdapter.model.Article;

public class NewsMessageToSend extends BaseMessage {
    private Map<String, List<Article>> news;

    public Map<String, List<Article>> getNews() {
        return news;
    }

    public void setNews(Map<String, List<Article>> news) {
        this.news = news;
    }
    
}
