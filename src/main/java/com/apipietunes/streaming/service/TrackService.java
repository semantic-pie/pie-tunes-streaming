package com.apipietunes.streaming.service;

import com.apipietunes.streaming.service.dto.CoverData;
import com.apipietunes.streaming.service.dto.TrackData;
import com.apipietunes.streaming.util.Range;

public interface TrackService {
    CoverData getTrackCoverById(String id);
    TrackData getRangedTrackFileById(String id, Range range);
} 
