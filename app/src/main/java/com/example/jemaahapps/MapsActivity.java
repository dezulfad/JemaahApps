package com.example.jemaahapps;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FusedLocationProviderClient client;
    private LatLng currentLatLng;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 44;
    private static final String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    private EditText etSearchPlace;
    private Button btnSearch, btnMosque;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        etSearchPlace = findViewById(R.id.etSearchPlace);
        btnSearch = findViewById(R.id.btnSearch);
        btnMosque = findViewById(R.id.btnMosque);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);

        client = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        btnMosque.setOnClickListener(v -> searchNearby("mosque"));

        btnSearch.setOnClickListener(v -> {
            String query = etSearchPlace.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(MapsActivity.this, "Please enter a place name", Toast.LENGTH_SHORT).show();
                return;
            }
            searchNearbyByName(query);
        });

        etSearchPlace.setOnEditorActionListener((TextView v, int actionId, android.view.KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchPlace.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchNearbyByName(query);
                } else {
                    Toast.makeText(MapsActivity.this, "Please enter a place name", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(marker -> {
            String placeId = (String) marker.getTag();
            if (placeId != null) {
                Intent intent = new Intent(MapsActivity.this, PlaceDetailActivity.class);
                intent.putExtra("place_id", placeId);
                startActivity(intent);
            }
            return false;
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
            } else {
                Toast.makeText(MapsActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            }
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchNearby(String type) {
        if (currentLatLng == null) {
            Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=2000&type=" + type +
                "&key=" + API_KEY;

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            String name = place.getString("name");

                            String placeId = place.getString("place_id");
                            LatLng latLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            marker.setTag(placeId);

                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show());

        queue.add(request);

        Toast.makeText(this, "Nearby " + type, Toast.LENGTH_SHORT).show();

    }

    // New method to search by user-input place name using Google Places Text Search API
    private void searchNearbyByName(String query) {
        if (currentLatLng == null) {
            Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                "query=" + query.replace(" ", "+") +
                "&location=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=2000" +
                "&key=" + API_KEY;

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() == 0) {
                            Toast.makeText(this, "No places found for \"" + query + "\"", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            String name = place.getString("name");
                            String placeId = place.getString("place_id");
                            LatLng latLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            marker.setTag(placeId);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show());

        queue.add(request);

        Toast.makeText(this, "Searching for \"" + query + "\"", Toast.LENGTH_SHORT).show();
    }
}
