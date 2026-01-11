package com.example.jemaahapps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

    String placeId;
    String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    double lat = 0, lng = 0; // for navigation

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

        placeId = getIntent().getStringExtra("place_id");

        loadPlaceDetails();
        setupClickableItems();
    }

    private void setupClickableItems() {

        // 📞 MAKE PHONE CLICKABLE
        txtPhone.setOnClickListener(v -> {
            String phone = txtPhone.getText().toString();
            if (!phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            }
        });

        // 🌐 MAKE WEBSITE CLICKABLE
        txtWebsite.setOnClickListener(v -> {
            String url = txtWebsite.getText().toString();
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browser);
        });

        // 🗺️ MAKE ADDRESS CLICKABLE → OPEN GOOGLE MAPS NAVIGATION
        txtAddress.setOnClickListener(v -> {
            if (lat != 0 && lng != 0) {
                String nav = "google.navigation:q=" + lat + "," + lng;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(nav));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            }
        });
    }

    private void loadPlaceDetails() {
        String url = "https://maps.googleapis.com/maps/api/place/details/json?" +
                "place_id=" + placeId +
                "&fields=name,rating,formatted_phone_number,formatted_address,opening_hours,website,geometry,photo" +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");

                        txtName.setText(result.optString("name"));
                        txtAddress.setText(result.optString("formatted_address"));
                        txtPhone.setText(result.optString("formatted_phone_number"));
                        txtRating.setText("Rating: " + result.optDouble("rating"));
                        txtWebsite.setText(result.optString("website"));

                        // Opening hours
                        if (result.has("opening_hours")) {
                            boolean openNow = result.getJSONObject("opening_hours").optBoolean("open_now");
                            txtOpenNow.setText(openNow ? "Open Now" : "Closed");
                        }

                        // GET LAT LNG (FOR NAVIGATION)
                        if (result.has("geometry")) {
                            JSONObject location = result.getJSONObject("geometry")
                                    .getJSONObject("location");
                            lat = location.getDouble("lat");
                            lng = location.getDouble("lng");
                        }

                        // PHOTO
                        if (result.has("photos")) {
                            String photoRef = result
                                    .getJSONArray("photos")
                                    .getJSONObject(0)
                                    .getString("photo_reference");

                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                    "?maxwidth=800" +
                                    "&photo_reference=" + photoRef +
                                    "&key=" + API_KEY;

                            Glide.with(PlaceDetailActivity.this)
                                    .load(photoUrl)
                                    .into(imagePlace);
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