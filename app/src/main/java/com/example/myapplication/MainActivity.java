package com.example.myapplication;

import android.Manifest;
import android.util.Log;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView weatherTextView;
    private Button getWeatherButton;
    private Spinner citySpinner;
    private EditText searchEditText;
    private TextView selectedCityTextView;
    private TextView currentLocationTextView;
    private ArrayAdapter<CharSequence> adapter;

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ImageView backgroundImageView;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        backgroundImageView = findViewById(R.id.backgroundImageView);
        setWeatherBackground("default");
        weatherTextView = findViewById(R.id.weatherTextView);
        weatherTextView.setTextSize(20);
        getWeatherButton = findViewById(R.id.getWeatherButton);
        citySpinner = findViewById(R.id.citySpinner);
        searchEditText = findViewById(R.id.searchEditText);
        selectedCityTextView = findViewById(R.id.CityTextView);
        currentLocationTextView = findViewById(R.id.currentLocationTextView);

        adapter = ArrayAdapter.createFromResource(this,
                R.array.cities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(adapter);

        citySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedCity = parentView.getItemAtPosition(position).toString();
                selectedCityTextView.setText(selectedCity);
                selectedCityTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCities(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getLocation();
        }

        getWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedCity = citySpinner.getSelectedItem().toString();
                fetchWeather(selectedCity);
            }
        });
    }

    private void getLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            String formattedLatitude = String.format(Locale.getDefault(), "%.6f", latitude);
                            String formattedLongitude = String.format(Locale.getDefault(), "%.6f", longitude);
                            String cityName = getCityName(latitude, longitude);
                            if (!cityName.isEmpty()) {
                                currentLocationTextView.setText("Текущее местоположение: " + cityName);
                                currentLocationTextView.setVisibility(View.VISIBLE);
                                selectCityInSpinner(cityName);
                            }
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void selectCityInSpinner(String cityName) {
        int index = adapter.getPosition(cityName);
        if (index != -1) {
            citySpinner.setSelection(index);
        }
    }

    private String getCityName(double latitude, double longitude) {
        Geocoder geocoder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            geocoder = new Geocoder(this, Locale.forLanguageTag("ru"));
        }
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                String cityName = addresses.get(0).getLocality();
                if (cityName != null) {
                    return cityName;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void filterCities(String text) {
        adapter.getFilter().filter(text);
    }

    private void fetchWeather(String city) {
        String apiKey = "a3d22948a8634a42f56c1b2fb03e0eee";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to fetch weather", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            private String translateWeatherDescription(String description) {
                switch (description) {
                    case "clear sky":
                        return "ясное небо";
                    case "few clouds":
                        return "небольшая облачность";
                    case "scattered clouds":
                        return "переменная облачность";
                    case "broken clouds":
                        return "облачно с прояснениями";
                    case "shower rain":
                        return "ливень";
                    case "overcast clouds":
                        return "сильная облачность";
                    case "moderate rain":
                    case "light intensity drizzle":
                    case "drizzle":
                    case "heavy intensity drizzle":
                    case "light intensity drizzle rain":
                    case "drizzle rain":
                    case "heavy intensity drizzle rain":
                    case "shower rain and drizzle":
                    case "heavy shower rain and drizzle":
                    case "shower drizzle":
                        return "умеренный дождь";

                    case "rain":
                    case "heavy intensity rain":
                    case "very heavy rain":
                    case "extreme rain":
                    case "freezing rain":
                    case "light intensity shower rain":
                    case "ragged shower rain":
                    case "heavy intensity shower rain":
                        return "дождь";
                    case "thunderstorm":
                    case "thunderstorm with light rain":
                    case "thunderstorm with rain":
                    case "thunderstorm with heavy rain":
                    case "light thunderstorm":
                    case "heavy thunderstorm":
                    case "ragged thunderstorm":
                    case "thunderstorm with light drizzle":
                    case "thunderstorm with drizzle":
                    case "thunderstorm with heavy drizzle":
                        return "гроза";
                    case "snow":
                    case "light snow":
                    case "heavy snow":
                    case "sleet":
                    case "light shower sleet":
                    case "shower sleet":
                    case "light rain and snow":
                    case "rain and snow":
                    case "light shower snow":
                    case "shower snow":
                    case "heavy shower snow":
                        return "снег";
                    case "mist":
                        return "туман";
                    case "light rain":
                        return "небольшой дождь";
                    default:
                        return description;
                }
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        final String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
                        final double temperatureKelvin = json.getJSONObject("main").getDouble("temp");
                        final double temperatureCelsius = temperatureKelvin - 273.15;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String translatedDescription = translateWeatherDescription(weatherDescription);
                                String weatherInfo = "Погода: " + translatedDescription + "\nТемпература: " + Math.round(temperatureCelsius) + "°C";
                                weatherTextView.setText(weatherInfo);
                                setWeatherBackground(weatherDescription);
                                setTextColorByWeatherDescription(weatherDescription);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to fetch weather", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    private void setTextColorByWeatherDescription(String weatherDescription) {
        int textColor;
        switch (weatherDescription) {
            case "clear sky":
                textColor = getResources().getColor(R.color.clear_sky_text_color);
                break;
            case "few clouds":
                textColor = getResources().getColor(R.color.few_clouds_text_color);
                break;
            case "scattered clouds":
                textColor = getResources().getColor(R.color.scattered_clouds_text_color);
                break;
            case "broken clouds":
                textColor = getResources().getColor(R.color.broken_clouds_text_color);
                break;
            case "shower rain":
            case "moderate rain":
            case "rain":
            case "light intensity drizzle":
            case "drizzle":
            case "heavy intensity drizzle":
            case "light intensity drizzle rain":
            case "drizzle rain":
            case "heavy intensity drizzle rain":
            case "shower rain and drizzle":
            case "heavy shower rain and drizzle":
            case "shower drizzle":
            case "light rain":
            case "heavy intensity rain":
            case "very heavy rain":
            case "extreme rain":
            case "freezing rain":
            case "light intensity shower rain":
            case "ragged shower rain":
            case "heavy intensity shower rain":
                textColor = getResources().getColor(R.color.rain_text_color);
                break;
            case "thunderstorm":
            case "thunderstorm with light rain":
            case "thunderstorm with rain":
            case "thunderstorm with heavy rain":
            case "light thunderstorm":
            case "heavy thunderstorm":
            case "ragged thunderstorm":
            case "thunderstorm with light drizzle":
            case "thunderstorm with drizzle":
            case "thunderstorm with heavy drizzle":
                textColor = getResources().getColor(R.color.thunderstorm_text_color);
                break;
            case "snow":
            case "light snow":
            case "heavy snow":
            case "sleet":
            case "light shower sleet":
            case "shower sleet":
            case "light rain and snow":
            case "rain and snow":
            case "light shower snow":
            case "shower snow":
            case "heavy shower snow":
                textColor = getResources().getColor(R.color.snow_text_color);
                break;
            case "mist":
                textColor = getResources().getColor(R.color.mist_text_color);
                break;
            default:
                textColor = getResources().getColor(R.color.default_text_color);
                break;
        }
        weatherTextView.setTextColor(textColor);
    }
    private void setWeatherBackground(String weatherDescription) {
        int backgroundResource;
        switch (weatherDescription) {
            case "default":
                backgroundResource = R.drawable.defauult;
                break;
            case "clear sky":
                backgroundResource = R.drawable.clear_sky_bg;
                break;
            case "few clouds":
                backgroundResource = R.drawable.few_clouds_bg;
                break;
            case "scattered clouds":
                backgroundResource = R.drawable.defauult;
                break;
            case "broken clouds":
                backgroundResource = R.drawable.broken_clouds_bg;
                break;
            case "shower rain":
            case "moderate rain":
            case "rain":
            case "light intensity drizzle":
            case "drizzle":
            case "heavy intensity drizzle":
            case "light intensity drizzle rain":
            case "drizzle rain":
            case "heavy intensity drizzle rain":
            case "shower rain and drizzle":
            case "heavy shower rain and drizzle":
            case "shower drizzle":
            case "light rain":
            case "heavy intensity rain":
            case "very heavy rain":
            case "extreme rain":
            case "freezing rain":
            case "light intensity shower rain":
            case "ragged shower rain":
            case "heavy intensity shower rain":
                backgroundResource = R.drawable.rain_bg;
                break;
            case "thunderstorm":
            case "thunderstorm with light rain":
            case "thunderstorm with rain":
            case "thunderstorm with heavy rain":
            case "light thunderstorm":
            case "heavy thunderstorm":
            case "ragged thunderstorm":
            case "thunderstorm with light drizzle":
            case "thunderstorm with drizzle":
            case "thunderstorm with heavy drizzle":
                backgroundResource = R.drawable.thunderstorm_bg;
                break;
            case "overcast clouds":
                backgroundResource = R.drawable.overcast_clouds;
                break;
            case "snow":
            case "light snow":
            case "heavy snow":
            case "sleet":
            case "light shower sleet":
            case "shower sleet":
            case "light rain and snow":
            case "rain and snow":
            case "light shower snow":
            case "shower snow":
            case "heavy shower snow":
                backgroundResource = R.drawable.snow_bg;
                break;
            case "mist":
            case "smoke":
            case "haze":
            case "sand/dust whirls":
            case "fog":
            case "sand":
            case "dust":
            case "volcanic ash":
            case "squalls":
            case "tornado":
                backgroundResource = R.drawable.mist_bg;
                break;
            default:
                backgroundResource = R.drawable.your_background_image;
                break;
        }
        findViewById(android.R.id.content).setBackgroundResource(backgroundResource);
        getWindow().setBackgroundDrawableResource(backgroundResource);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
