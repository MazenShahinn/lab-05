package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // Firestore
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Firestore instance + collection ref ("cities")
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Snapshot listener keeps ListView synced with Firestore
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Listen failed.", error);
                return;
            }

            cityArrayList.clear();

            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String name = doc.getId();                 // doc id is the city name
                    String province = doc.getString("Province"); // field in Firestore
                    if (province == null) province = "";
                    cityArrayList.add(new City(name, province));
                }
            }

            cityArrayAdapter.notifyDataSetChanged();
        });

        // Add City button -> opens dialog
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // Tap item -> details dialog
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });

        // Long press item -> DELETE (and persist delete to Firestore)
        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city != null) {
                String cityName = getCityName(city);
                citiesRef.document(cityName)
                        .delete()
                        .addOnSuccessListener(unused -> Log.d("Firestore", "Deleted: " + cityName))
                        .addOnFailureListener(e -> Log.e("Firestore", "Delete failed", e));
            }
            return true;
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = getCityName(city);

        city.setName(title);
        city.setProvince(year);

        Map<String, Object> data = new HashMap<>();
        data.put("Province", year);

        // If name changed, delete old doc first so you don't keep duplicates
        if (!oldName.equals(title)) {
            citiesRef.document(oldName).delete();
        }

        citiesRef.document(title)
                .set(data)
                .addOnSuccessListener(unused -> Log.d("Firestore", "Updated: " + title))
                .addOnFailureListener(e -> Log.e("Firestore", "Update failed", e));
    }

    @Override
    public void addCity(City city) {
        String cityName = getCityName(city);
        String province = getProvince(city);

        Map<String, Object> data = new HashMap<>();
        data.put("Province", province);

        citiesRef.document(cityName)
                .set(data)
                .addOnSuccessListener(unused -> Log.d("Firestore", "Added: " + cityName))
                .addOnFailureListener(e -> Log.e("Firestore", "Add failed", e));
    }

    // ---- Helper methods so you only adjust getter names in one spot ----
    private String getCityName(City city) {
        // CHANGE THIS if your City getter is named differently (slides use getCityName()).
        // return city.getCityName();
        return city.getName();
    }

    private String getProvince(City city) {
        // CHANGE THIS if your City getter is named differently.
        return city.getProvince();
    }
}
