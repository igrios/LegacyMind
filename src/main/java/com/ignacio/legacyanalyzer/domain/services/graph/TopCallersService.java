package com.ignacio.legacyanalyzer.domain.services;

import java.util.List;
import com.ignacio.legacyanalyzer.application.dto.TopCallerResponse;

public interface TopCallersService {


    List<TopCallerResponse> getTopCallers();
}
