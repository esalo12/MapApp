package Modules;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class RuteFinner {
    private static final String RUTE_URL_API = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyAJ7bSWIzVG6JV7NoWawybVzWc6ff2vitk";
    private RuteFinnerListener listener;
    private String fra;
    private String til;
    private static final String TAG = "Her";

    // Public RuteFinner tar string fra og til for å kunne brukes i metoder i klassen.
    // Interfacet RuteFinnerListener er også med for å kunne få ut resultatet.
    public RuteFinner(RuteFinnerListener listener, String fra, String til) {
        this.listener = listener;
        this.fra = fra;
        this.til = til;
    }
    // Starter listener og sender URL til lastNedData
    public void startRuteSok() throws UnsupportedEncodingException {
        //listener.finnRuteStart();
        new LastNedData().execute(lagUrl());
    }
    // Lager URL av base_url(RUTE_URL_API) med tillegg for å gjøre et søk hos google.
    //
    private String lagUrl() throws UnsupportedEncodingException {
        String urlFra = URLEncoder.encode(fra, "utf-8");
        String urlTil = URLEncoder.encode(til, "utf-8");

        return RUTE_URL_API + "origin=" + urlFra + "&destination=" + urlTil + "&key=" + GOOGLE_API_KEY;
    }
    // Privat klasse som starter en asyncron oppgave, med en Get URL via startRuteSok's kall på lagURL.
    private class LastNedData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader leser = new BufferedReader(new InputStreamReader(is));

                String linje;
                while ((linje = leser.readLine()) != null) {
                    buffer.append(linje + "\n");
                }
                return buffer.toString();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                parserJSon(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    // Metode som parserer data og legger i ulike variabler
    private void parserJSon(String data) throws JSONException {
        if (data == null)
            return;
        // Lager en Arraylyste for RuteObjecter
        List<LatLng> etappePunkter = new ArrayList<>();
        List<Rute> ruter = new ArrayList<>();
        // Henter inn JSON data(objektet) fra metodekallet
        JSONObject jsonData = new JSONObject(data);
        // Henter JSONArrayen routes fra objektet
        JSONArray jsonRuter = jsonData.getJSONArray("routes");
        // Kjører gjennom JSONArrayen og plasserer objekt.info herfra i et nytt Rute objekt
        for (int i = 0; i < jsonRuter.length(); i++) {
            JSONObject jsonRute = jsonRuter.getJSONObject(i);
            Rute rute = new Rute();

            JSONObject overviewPolyline = jsonRute.getJSONObject("overview_polyline");
            JSONArray etapper = jsonRute.getJSONArray("legs");
            JSONObject etappe = etapper.getJSONObject(0);
            JSONObject avstand = etappe.getJSONObject("distance");
            JSONObject sluttPunkt = etappe.getJSONObject("end_location");
            JSONObject startPunkt = etappe.getJSONObject("start_location");

            // Denne delen er lagt til for å vise alternativ rute tegning:
            // Den er ikke implimentert i Main Activity, men fjerning av utkommentering av implimentasjon
            // samt utkommentering av gjeldende, viser at steps gir færre punkter
            JSONArray steg = etappe.getJSONArray("steps");
            for (int j = 0; j < steg.length(); j++) {
                JSONObject etappen = steg.getJSONObject(j);
                JSONObject etappepunkt = etappen.getJSONObject("end_location");
                LatLng punktet = new LatLng(etappepunkt.getDouble("lat"), etappepunkt.getDouble("lng"));
                etappePunkter.add(punktet);
            }

            rute.avstand = new Avstand(avstand.getString("text"), avstand.getInt("value"));
            rute.startPunkt = new LatLng(startPunkt.getDouble("lat"), startPunkt.getDouble("lng"));
            rute.sluttPunkt = new LatLng(sluttPunkt.getDouble("lat"), sluttPunkt.getDouble("lng"));
            rute.punkter = dekodingPolyLine(overviewPolyline.getString("points"));
            ruter.add(rute);
            Log.d(TAG, ""+steg);
        }

        // Sender ruter funnet til listener som en array med Rute objekter.
        // Her er det lagt til EtappePunkter for alternativ opptegning,
        // som nevnt over ikke implimentert, men kan fort gjøres med fjerning av utkommentering i Main Activity.
        listener.finnRuteResultat(ruter, etappePunkter);
    }

    // Metoden her dekoder Polyline delen av response fra google.
    // Den går igjennom alle punkter i googles "points", og behandler disse i forhold til googles "overview_polyline"
    // Matematikken her har jeg ikke helt grepet på så jeg velger å ikke kommentere denne.
    // Metoden er så og si kopiert fra videoen som er brukt som grunnlag.
    // Metoden dekoder polyline punkter fra points slik at man får de enkelte punkter i ruta fra google.
    // Alternativt kunne man her brukt googles "legs" sin "LatLng" og tegnet mellom dem.
    // Det er langt flere punkter i "points" enn i "legs" med disses "start_location" og "end_location", så ruta fremstår rundere med "points".
    // Se forøvrig alternativ ruteopptegning i metoden over.
    private List<LatLng> dekodingPolyLine(final String polypunkt) {
        int len = polypunkt.length();
        int index = 0;
        List<LatLng> dekodet = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int resultat = 0;
            do {
                b = polypunkt.charAt(index++) - 63;
                resultat |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((resultat & 1) != 0 ? ~(resultat >> 1) : (resultat >> 1));
            lat += dlat;

            shift = 0;
            resultat = 0;
            do {
                b = polypunkt.charAt(index++) - 63;
                resultat |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((resultat & 1) != 0 ? ~(resultat >> 1) : (resultat >> 1));
            lng += dlng;

            dekodet.add(new LatLng(
                    lat/100000d, lng/100000d
            ));
        }
        return dekodet;
    }
}