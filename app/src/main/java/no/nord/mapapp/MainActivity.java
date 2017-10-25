package no.nord.mapapp;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import Modules.Rute;
import Modules.RuteFinner;
import Modules.RuteFinnerListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, RuteFinnerListener{
    private MapView mapView;
    private GoogleMap map;
    private List<Polyline> polylineRute = new ArrayList<>();
    LatLng latLngFra;
    LatLng latLngTil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        final Context context = this;
        final EditText start = (EditText) findViewById(R.id.sokFra);
        MapsInitializer.initialize(this);
        // Denne delen brukes kun for å fylle startpunkt tekstfeltet, med midlertidige tekst...
        // og for enkelhets skyld. Resultatet kan gi feil utgangspunkt for søk på rute,
        // da det varierer med data på resultatene, og man kan få null referanser feks i båt.
        SmartLocation.with(this).location()
                .config(LocationParams.NAVIGATION)
                .oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        try {
                            Geocoder geocoder = new Geocoder(context);
                            List<Address> adressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            String adresse = adressList.get(0).getThoroughfare();
                            String sted = adressList.get(0).getSubAdminArea();
                            googleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .title(adresse));
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10);
                            googleMap.moveCamera(cameraUpdate);
                            start.setText(adresse+" "+sted);
                        }
                        catch(IOException ex){
                            ex.printStackTrace();
                        }
                    }
                });
        this.mapView.onResume();
    }
    // onClick setter igang hele søkeprosessen, som ligger i denne metoden.
    public void sok(View view) {
        final Context context = this;
        mapView = (MapView) findViewById(R.id.mapView);
        map = mapView.getMap();
        EditText fra = (EditText) findViewById(R.id.sokFra);
        String lokasjonFra = fra.getText().toString().trim();
        EditText til = (EditText) findViewById(R.id.sokTil);
        String lokasjonTil = til.getText().toString().trim();
        List<Address> adresser = null;
        if (lokasjonTil.isEmpty() || lokasjonFra.isEmpty()){
            Toast toast = Toast.makeText(getApplicationContext(), "Begge felter, må være utfylt", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        else {
            Geocoder geocoder = new Geocoder(context);
            try {
                adresser = geocoder.getFromLocationName(lokasjonFra, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (adresser == null || adresser.size() == 0){
                Toast toast = Toast.makeText(getApplicationContext(), "startpunkt adressen er ugyldig", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            else {
                Address adresse = adresser.get(0);
                latLngFra = new LatLng(adresse.getLatitude(), adresse.getLongitude());
                map.clear();
                map.addMarker(new MarkerOptions().position(latLngFra).title(lokasjonFra));
            }
            adresser=null;
            try {
                adresser = geocoder.getFromLocationName(lokasjonTil, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (adresser == null || adresser.size() == 0) {
                Toast toast = Toast.makeText(getApplicationContext(), "sluttpunkt adressen er ugyldig", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            else {
                Address adresse2 = adresser.get(0);
                latLngTil = new LatLng(adresse2.getLatitude(), adresse2.getLongitude());
                map.addMarker(new MarkerOptions().position(latLngTil).title(lokasjonTil));
            }
            try {
                new RuteFinner(this, lokasjonFra, lokasjonTil).startRuteSok();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        this.mapView.onResume();
    }

    @Override
    public void finnRuteResultat(List<Rute> ruter, List<LatLng> punkter) {
        PolylineOptions polyline = new PolylineOptions().
                geodesic(true).
                color(Color.MAGENTA).
                width(10);
        // Alternativt kan man bruke denne, men jeg har ikke lagt ved avstand på denne.
        // Om man prøver ser man at det er langt færre punkter.
        /*polyline.add(latLngFra);
        for (int i = 0; i < punkter.size(); i++) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngFra, 10));
            polyline.add(punkter.get(i));
        }*/

        // Denne delen må kommenteres ut for alternitiv rutetegning fra her ...
        for (Rute rute : ruter) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(rute.startPunkt, 10));
            ((TextView) findViewById(R.id.avstandText)).setText("Kjøredistanse: "+rute.avstand.text);
            for (int i = 0; i < rute.punkter.size(); i++) {
                polyline.add(rute.punkter.get(i));
            }
            polylineRute.add(map.addPolyline(polyline));
        }
        // .... til her

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
