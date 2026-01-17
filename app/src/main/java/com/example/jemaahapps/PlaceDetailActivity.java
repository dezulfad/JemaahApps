package com.example.jemaahapps;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

public class PlaceDetailActivity extends AppCompatActivity {

    ImageView imagePlace;
    TextView txtName, txtAddress, txtPhone, txtRating, txtWebsite, txtOpenNow;
    Button btnCall;

    String placeId;
    String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    double lat = 0, lng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        imagePlace = findViewById(R.id.imagePlace);
        txtName = findViewById(R.id.txtName);
        txtAddress = findViewById(R.id.txtAddress);
        txtPhone = findViewById(R.id.txtPhone);
        txtRating = findViewById(R.id.txtRating);
        txtWebsite = findViewById(R.id.txtWebsite);
        txtOpenNow = findViewById(R.id.txtOpenNow);
        btnCall = findViewById(R.id.btnCall);

        placeId = getIntent().getStringExtra("place_id");

        loadPlaceDetails();
    }

    private void loadPlaceDetails() {

        String url = "https://maps.googleapis.com/maps/api/place/details/json?" +
                "place_id=" + placeId +
                "&fields=name,rating,formatted_phone_number,formatted_address,opening_hours,website,geometry,photos" +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");

                        txtName.setText(result.optString("name", "N/A"));
                        txtRating.setText("Rating: " + result.optDouble("rating", 0.0));

                        // 📍 GEOMETRY (FOR MAPS)
                        if (result.has("geometry")) {
                            JSONObject location = result.getJSONObject("geometry")
                                    .getJSONObject("location");
                            lat = location.getDouble("lat");
                            lng = location.getDouble("lng");
                        }

                        // 📍 ADDRESS (SPANNABLE)
                        String address = result.optString("formatted_address", "");
                        if (!address.isEmpty()) {

                            String label = "Address: ";
                            String full = label + address;

                            SpannableString ss = new SpannableString(full);

                            ss.setSpan(new ForegroundColorSpan(Color.BLACK),
                                    0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            ss.setSpan(new ForegroundColorSpan(Color.BLUE),
                                    label.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            ss.setSpan(new ClickableSpan() {
                                @Override
                                public void onClick(View widget) {
                                    if (lat != 0 && lng != 0) {
                                        String nav = "google.navigation:q=" + lat + "," + lng;
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(nav));
                                        intent.setPackage("com.google.android.apps.maps");
                                        startActivity(intent);
                                    }
                                }
                            }, label.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            txtAddress.setText(ss);
                            txtAddress.setMovementMethod(LinkMovementMethod.getInstance());

                        } else {
                            txtAddress.setText("Address: Not available");
                        }

                        // 🌐 WEBSITE (SPANNABLE)
                        String website = result.optString("website", "");
                        if (!website.isEmpty()) {

                            String label = "Website: ";
                            String full = label + website;

                            SpannableString ss = new SpannableString(full);

                            ss.setSpan(new ForegroundColorSpan(Color.BLACK),
                                    0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            ss.setSpan(new ForegroundColorSpan(Color.BLUE),
                                    label.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            ss.setSpan(new ClickableSpan() {
                                @Override
                                public void onClick(View widget) {
                                    String url = website.startsWith("http") ? website : "https://" + website;
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                }
                            }, label.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            txtWebsite.setText(ss);
                            txtWebsite.setMovementMethod(LinkMovementMethod.getInstance());

                        } else {
                            txtWebsite.setText("Website: Not available");
                        }

                        // 📞 PHONE (SPANNABLE + BUTTON)
                        String phone = result.optString("formatted_phone_number", "");
                        if (!phone.isEmpty()) {

                            String label = "Contact Number: ";
                            String full = label + phone;

                            SpannableString ss = new SpannableString(full);

                            ss.setSpan(new ForegroundColorSpan(Color.BLACK),
                                    0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            ss.setSpan(new ForegroundColorSpan(Color.BLUE),
                                    label.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            txtPhone.setText(ss);

                            btnCall.setVisibility(View.VISIBLE);
                            btnCall.setOnClickListener(v -> {
                                startActivity(new Intent(
                                        Intent.ACTION_DIAL,
                                        Uri.parse("tel:" + phone)));
                            });

                        } else {
                            txtPhone.setText("Contact Number: Not available for now");
                            btnCall.setVisibility(View.GONE);
                        }

                        // 🕒 OPENING HOURS
                        if (result.has("opening_hours")) {
                            boolean openNow = result.getJSONObject("opening_hours")
                                    .optBoolean("open_now", false);
                            txtOpenNow.setText(openNow ? "Open Now" : "Closed");
                        } else {
                            txtOpenNow.setText("Opening hours not available");
                        }

                        // 🖼 PHOTO
                        if (result.has("photos")) {
                            String photoRef = result.getJSONArray("photos")
                                    .getJSONObject(0)
                                    .getString("photo_reference");

                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                    "?maxwidth=800" +
                                    "&photo_reference=" + photoRef +
                                    "&key=" + API_KEY;

                            Glide.with(this).load(photoUrl).into(imagePlace);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("DETAIL_ERROR", error.toString())
        );

        queue.add(request);
    }
}