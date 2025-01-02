package com.lee.osakacity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class YoutubeResponse {
    private List<YouTubeSearchItem> items = new ArrayList<>();

}
