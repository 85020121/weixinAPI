package com.hesong.weChatAdapter.message.request;

public class LocationMessage {

    private String Location_X;
    private String Location_Y;
    private double Scale;
    private String Label;
    public String getLocation_X() {
        return Location_X;
    }
    public void setLocation_X(String location_X) {
        Location_X = location_X;
    }
    public String getLocation_Y() {
        return Location_Y;
    }
    public void setLocation_Y(String location_Y) {
        Location_Y = location_Y;
    }
    public double getScale() {
        return Scale;
    }
    public void setScale(double scale) {
        Scale = scale;
    }
    public String getLabel() {
        return Label;
    }
    public void setLabel(String label) {
        Label = label;
    }
    
}
