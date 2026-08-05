package com.company.assistant.shuttle;

import com.company.assistant.routing.Coordinate;

import java.util.List;

public record RouteMatchResponse(List<Coordinate> coordinates) {}
