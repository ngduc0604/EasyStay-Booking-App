package com.example.hotelbookingapp.models;

public class Search {
    private String hotelName;
    private String dateCheckIn;
    private String dateCheckOut;

    public Search() {
    }

    public Search(String hotelName, String dateCheckIn, String dateCheckOut) {
        this.hotelName = hotelName;
        this.dateCheckIn = dateCheckIn;
        this.dateCheckOut = dateCheckOut;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getDateCheckIn() {
        return dateCheckIn;
    }

    public void setDateCheckIn(String dateCheckIn) {
        this.dateCheckIn = dateCheckIn;
    }

    public String getDateCheckOut() {
        return dateCheckOut;
    }

    public void setDateCheckOut(String dateCheckOut) {
        this.dateCheckOut = dateCheckOut;
    }
}
